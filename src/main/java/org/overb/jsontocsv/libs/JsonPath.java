package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JsonPath {

    public static JsonNode navigate(JsonNode node, String path) {
        if (node == null || path == null || path.isBlank()) return node;
        for (String seg : path.split("\\.")) {
            node = node.path(seg);
        }
        return node;
    }

    public static List<JsonNode> findNodesByPath(JsonNode root, String path) {
        if (root == null) return List.of();
        if (path == null || path.isBlank()) return List.of(root);

        List<JsonNode> current = List.of(root);
        String[] segments = path.split("\\.");
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                JsonNode child = node.path(segment);
                if (child.isMissingNode() || child.isNull()) continue;
                if (child.isArray()) {
                    child.forEach(next::add);
                } else {
                    next.add(child);
                }
            }
            if (next.isEmpty()) return Collections.emptyList();
            current = next;
        }
        return current;
    }

    public static String findArrayAncestorPath(JsonNode record, String path) {
        if (path == null || path.isBlank()) return null;
        String[] segments = path.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments.length; i++) {
            if (i > 0) sb.append('.');
            sb.append(segments[i]);
            JsonNode current = JsonPath.navigate(record, sb.toString());
            if (current.isArray()) return sb.toString();
            if (current.isMissingNode() || current.isNull()) break;
        }
        return null;
    }

    public static String relativePath(String fullPath, String ancestorPath) {
        if (ancestorPath == null || ancestorPath.isBlank()) return fullPath;
        if (fullPath.equals(ancestorPath)) return "";
        if (fullPath.startsWith(ancestorPath + ".")) {
            return fullPath.substring(ancestorPath.length() + 1);
        }
        return fullPath;
    }
}