package org.overb.jsontocsv.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class JsonService {

    private final ObjectMapper mapper = new ObjectMapper();

    public JsonNode loadFromFile(File file) throws Exception {
        JsonNode rootNode = null;
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String trimmed = content.trim().replaceAll("\r", "");
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            rootNode = mapper.readTree(trimmed);
        } else if (trimmed.contains("},{")) {
            String wrapped = "[" + trimmed + "]";
            rootNode = mapper.readTree(wrapped);
        } else if (trimmed.startsWith("{") && trimmed.contains("}\n{")) {
            ArrayNode arrayNode = mapper.createArrayNode();
            for (String line : trimmed.split("\n")) {
                if (line.isBlank()) continue;
                arrayNode.add(mapper.readTree(line));
            }
            rootNode = arrayNode;
        } else {
            rootNode = mapper.readTree(trimmed);
        }
        return rootNode;
    }

    public static int maxDepth(JsonNode node) {
        if (!node.isContainerNode()) {
            return 1;
        }
        int maxChild = 0;
        if (node.isObject()) {
            for (JsonNode child : node) {
                maxChild = Math.max(maxChild, maxDepth(child));
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                maxChild = Math.max(maxChild, maxDepth(element));
            }
        }
        return maxChild;
    }

    public static boolean isNested(JsonNode node) {
        return maxDepth(node) > 1;
    }

    public static String extractJsonValue(JsonNode rootNode, String fieldName) {
        if (rootNode.get(fieldName) == null || rootNode.get(fieldName).isNull()) {
            return null;
        }
        return rootNode.get(fieldName).asText();
    }
}
