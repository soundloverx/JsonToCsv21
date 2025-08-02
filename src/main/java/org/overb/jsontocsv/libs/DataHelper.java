package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.overb.jsontocsv.dto.CsvColumnDefinition;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class DataHelper {

    public static JsonNode loadJsonFile(File file) throws Exception {
        JsonNode rootNode = null;
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String trimmed = content.trim().replaceAll("\r", "");
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            rootNode = JsonSchemaHelper.mapper.readTree(trimmed);
        } else if (trimmed.contains("},{")) {
            String wrapped = "[" + trimmed + "]";
            rootNode = JsonSchemaHelper.mapper.readTree(wrapped);
        } else if (trimmed.startsWith("{") && trimmed.contains("}\n{")) {
            ArrayNode arrayNode = JsonSchemaHelper.mapper.createArrayNode();
            for (String line : trimmed.split("\n")) {
                if (line.isBlank()) continue;
                arrayNode.add(JsonSchemaHelper.mapper.readTree(line));
            }
            rootNode = arrayNode;
        } else {
            rootNode = JsonSchemaHelper.mapper.readTree(trimmed);
        }
        return rootNode;
    }

    public static int getJsonDepth(JsonNode node) {
        if (node.isArray()) {
            return !node.isEmpty() ? 1 : 0;
        }
        if (node.isObject()) {
            int childMax = 0;
            for (Iterator<JsonNode> it = node.elements(); it.hasNext(); ) {
                childMax = Math.max(childMax, getJsonDepth(it.next()));
            }
            return 1 + childMax;
        }
        return 0;
    }

    public static JsonSchemaHelper.Schema buildJsonSchema(JsonNode node) {
        if (node.isObject()) {
            JsonSchemaHelper.ObjectSchema objectSchema = new JsonSchemaHelper.ObjectSchema();
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                JsonNode child = node.get(key);
                objectSchema.fields.put(key, buildJsonSchema(child));
            }
            return objectSchema;
        }
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            JsonSchemaHelper.Schema merged = null;
            for (JsonNode element : array) {
                JsonSchemaHelper.Schema child = buildJsonSchema(element);
                if (merged == null) merged = child;
                else merged = merged.merge(child);
            }
            return new JsonSchemaHelper.ArraySchema(merged != null ? merged : new JsonSchemaHelper.PrimitiveSchema());
        }
        return new JsonSchemaHelper.PrimitiveSchema();
    }

    public static ObservableList<Map<String, String>> previewCsvRows(JsonNode json, String rootPath, List<CsvColumnDefinition> definitions, int limit) throws IllegalArgumentException {
        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        if(definitions == null || definitions.isEmpty()) {
            return rows;
        }
        if (rootPath != null && !rootPath.isBlank()) {
            for (String seg : rootPath.split("\\.")) {
                json = json.path(seg);
            }
        }
        List<JsonNode> records = new ArrayList<>();
        if (json.isArray()) {
            for (JsonNode node : json) {
                records.add(node);
            }
        } else {
            records.add(json);
        }
        for (JsonNode record : records) {
            List<Map<String, String>> contexts = new ArrayList<>();
            contexts.add(new HashMap<>());
            for (CsvColumnDefinition def : definitions) {
                String relPath = def.getJsonColumn();
                List<JsonNode> found = findNodesByPath(record, relPath);
                List<Map<String, String>> next = new ArrayList<>();
                for (Map<String, String> context : contexts) {
                    if (found.isEmpty()) {
                        Map<String, String> copy = new HashMap<>(context);
                        copy.put(def.getCsvColumn(), "");
                        next.add(copy);
                    } else {
                        for (JsonNode node : found) {
                            Map<String, String> copy = new HashMap<>(context);
                            copy.put(def.getCsvColumn(), node.isValueNode() ? node.asText() : node.toString());
                            next.add(copy);
                        }
                    }
                }
                contexts = next;
            }
            rows.addAll(contexts);
            if (limit > 0 && rows.size() >= limit) {
                break;
            }
        }
        return rows;
    }

    public static List<JsonNode> findNodesByPath(JsonNode root, String path) {
        List<JsonNode> current = List.of(root);
        for (String segment : path.split("\\.")) {
            List<JsonNode> next = new ArrayList<>();
            for (JsonNode node : current) {
                JsonNode child = node.path(segment);
                if (child.isMissingNode() || child.isNull()) {
                    continue;
                }
                if (child.isArray()) {
                    child.forEach(next::add);
                } else {
                    next.add(child);
                }
            }
            current = next;
            if (current.isEmpty()) break;
        }
        return current;
    }
}
