package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.*;
import java.util.stream.Collectors;

public class JsonRootDetector {
    /*
    Goal: Find the most likely array node that represents the “rows” to be expanded into CSV (usually an array of objects), and return its dot-path (e.g., data.items).
    Heuristics to rank candidate roots Consider each array in the JSON tree as a candidate and score it using:
    - Element type
        - Array of objects: strong positive
        - Array of arrays: neutral (might still be nested tables)
        - Array of primitives: negative
    - Homogeneity
        - Elements share similar keys/shape: positive
        - Very heterogeneous: negative
    - Keys richness
        - Objects with multiple keys (2–50) vs single-key wrappers: positive
        - Extremely large key sets (hundreds): slight negative (might be dictionary-like)
    - Depth
        - Slightly nested arrays (depth 1–3) usually indicate the real table; too deep may be overly specific: mild penalty with depth
    - Size estimate
        - Non-empty arrays score better; very small (size 0/1) lower than arrays with 5+ elements
    - Naming hints
        - Field names like items, records, rows, data, results, list, entries: small boost
    - Uniqueness against siblings
        - If multiple arrays exist, prefer the one whose objects align with column-like fields (shared keys), not arrays of primitives or mixed.
     */

    public static Optional<String> detectSuggestedRoot(JsonNode root) {
        List<Candidate> candidates = new ArrayList<>();
        collectArrays(root, "", 0, candidates);
        if (candidates.isEmpty()) return Optional.empty();

        for (Candidate c : candidates) {
            analyzeArray(c);
            score(c);
        }
        candidates.sort(Comparator.comparingDouble((Candidate c) -> c.score).reversed());
        Candidate best = candidates.get(0);
        // Optional: require a minimal score to auto-fill, otherwise just suggest
        return best.score > 1.5 ? Optional.of(best.path) : Optional.of(best.path);
    }

    private static void collectArrays(JsonNode node, String path, int depth, List<Candidate> out) {
        if (node == null) return;
        if (node.isArray()) {
            out.add(new Candidate(path, depth, (ArrayNode) node));
        }
        if (node.isObject()) {
            node.fieldNames().forEachRemaining(fn -> {
                JsonNode child = node.get(fn);
                String nextPath = path.isEmpty() ? fn : path + "." + fn;
                collectArrays(child, nextPath, depth + 1, out);
            });
        } else if (node.isArray()) {
            // dive into first few elements to find nested arrays (rare but possible)
            int i = 0;
            for (JsonNode child : node) {
                if (i++ >= 5) break;
                collectArrays(child, path, depth + 1, out);
            }
        }
    }

    private static void analyzeArray(Candidate c) {
        ArrayNode arr = c.array;
        int size = arr.size();
        c.size = size;
        int sample = Math.min(size, 20);

        int objCount = 0, arrCount = 0, primCount = 0;
        List<Set<String>> keySets = new ArrayList<>();
        int i = 0;
        for (JsonNode el : arr) {
            if (i++ >= sample) break;
            if (el.isObject()) {
                objCount++;
                Set<String> keys = new HashSet<>();
                el.fieldNames().forEachRemaining(keys::add);
                keySets.add(keys);
            } else if (el.isArray()) {
                arrCount++;
            } else {
                primCount++;
            }
        }
        c.objectElemCount = objCount;
        c.arrayElemCount = arrCount;
        c.primitiveElemCount = primCount;

        // Homogeneity via average Jaccard similarity between key sets
        c.avgKeyJaccard = averageJaccard(keySets);
        c.unionKeyCount = keySets.stream().flatMap(Set::stream).collect(Collectors.toSet()).size();

        // Name hint
        String lastName = c.path.contains(".") ? c.path.substring(c.path.lastIndexOf('.') + 1) : c.path;
        c.nameHintBoost = nameHint(lastName);
    }

    private static double averageJaccard(List<Set<String>> sets) {
        int n = sets.size();
        if (n < 2) return 0.0;
        double sum = 0.0;
        int pairs = 0;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                sum += jaccard(sets.get(i), sets.get(j));
                pairs++;
            }
        }
        return pairs == 0 ? 0.0 : (sum / pairs);
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        Set<String> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<String> uni = new HashSet<>(a);
        uni.addAll(b);
        return uni.isEmpty() ? 0.0 : ((double) inter.size() / (double) uni.size());
    }

    private static double nameHint(String name) {
        if (name == null) return 0.0;
        String n = name.toLowerCase(Locale.ROOT);
        // Light boosts
        if (n.matches(".*(items|records|rows|results|data|list|entries)$")) return 0.25;
        if (n.matches(".*(users|orders|products|events)$")) return 0.15;
        return 0.0;
    }

    private static void score(Candidate c) {
        double score = 0.0;

        // Element type weighting
        int sampleCount = c.objectElemCount + c.arrayElemCount + c.primitiveElemCount;
        if (sampleCount == 0) {
            c.score = -1.0;
            return;
        }
        double objRatio = (double) c.objectElemCount / sampleCount;
        double arrRatio = (double) c.arrayElemCount / sampleCount;
        double primRatio = (double) c.primitiveElemCount / sampleCount;

        score += objRatio * 2.0;          // arrays of objects are ideal
        score += arrRatio * 0.2;          // arrays of arrays are possible but weaker
        score -= primRatio * 1.0;         // primitives unlikely to be row-level

        // Homogeneity (consistent object shape)
        score += Math.min(1.0, c.avgKeyJaccard) * 0.8;

        // Keys richness (prefer some columns, but avoid absurdly large)
        if (c.unionKeyCount >= 2 && c.unionKeyCount <= 60) score += 0.6;
        else if (c.unionKeyCount > 60) score -= 0.2;

        // Size: prefer non-empty; small positive effect
        if (c.size == 0) score -= 0.5;
        else if (c.size == 1) score -= 0.2;
        else if (c.size >= 5) score += 0.3;

        // Depth: slight penalty for being too deep (avoid overly specific nested arrays)
        score -= Math.max(0, c.depth - 3) * 0.1;

        // Name hints
        score += c.nameHintBoost;

        c.score = score;
    }

    private static class Candidate {
        final String path;
        final int depth;
        final ArrayNode array;
        int size;
        int objectElemCount, arrayElemCount, primitiveElemCount;
        int unionKeyCount;
        double avgKeyJaccard;
        double nameHintBoost;
        double score;

        Candidate(String path, int depth, ArrayNode array) {
            this.path = path;
            this.depth = depth;
            this.array = array;
        }
    }
}
