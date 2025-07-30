package org.overb.jsontocsv.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.overb.jsontocsv.libs.JsonSchemaHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.Map;

public class JsonService {

    public static JsonNode loadFromFile(File file) throws Exception {
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

    public static JsonSchemaHelper.Schema buildSchema(JsonNode node) {
        if (node.isObject()) {
            JsonSchemaHelper.ObjectSchema objectSchema = new JsonSchemaHelper.ObjectSchema();
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                JsonNode child = node.get(key);
                objectSchema.fields.put(key, buildSchema(child));
            }
            return objectSchema;
        }
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            JsonSchemaHelper.Schema merged = null;
            for (JsonNode element : array) {
                JsonSchemaHelper.Schema child = buildSchema(element);
                if (merged == null) merged = child;
                else merged = merged.merge(child);
            }
            // an empty array is still an array
            return new JsonSchemaHelper.ArraySchema(merged != null ? merged : new JsonSchemaHelper.PrimitiveSchema());
        }
        // scalars = PrimitiveSchema
        return new JsonSchemaHelper.PrimitiveSchema();
    }

    public static JsonNode extractSchemaJson(JsonNode json) throws Exception {
        JsonSchemaHelper.Schema schema = buildSchema(json);
        return schema.instantiate();
//        return JsonSchemaHelper.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(minimal); //maybe I will customize the pretty printer someday
    }
}
