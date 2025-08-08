package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class JsonIo {

    public static JsonNode loadJsonFile(File file) throws Exception {
        String content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        String trimmed = content.trim().replaceAll("\r", "");

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return JsonSchemaHelper.mapper.readTree(trimmed);
        }
        if (trimmed.contains("},{")) {
            String wrapped = "[" + trimmed + "]";
            return JsonSchemaHelper.mapper.readTree(wrapped);
        }
        if (trimmed.startsWith("{") && trimmed.contains("}\n{")) {
            ArrayNode arrayNode = JsonSchemaHelper.mapper.createArrayNode();
            for (String line : trimmed.split("\n")) {
                if (line.isBlank()) continue;
                arrayNode.add(JsonSchemaHelper.mapper.readTree(line));
            }
            return arrayNode;
        }
        return JsonSchemaHelper.mapper.readTree(trimmed);
    }
}