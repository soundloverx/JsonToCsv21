package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPInputStream;

public final class JsonIo {

    public static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    public static JsonNode loadJsonFile(File file) throws Exception {
        boolean isGzip = file.getName().endsWith(".gz");
        String content = "";
        try (InputStream raw = Files.newInputStream(file.toPath(), StandardOpenOption.READ);
             InputStream in = isGzip ? new GZIPInputStream(raw) : raw) {
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        String trimmed = content.trim().replaceAll("\r", "");

        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            return MAPPER.readTree(trimmed);
        }
        if (trimmed.contains("},{")) {
            String wrapped = "[" + trimmed + "]";
            return MAPPER.readTree(wrapped);
        }
        if (trimmed.startsWith("{") && trimmed.contains("}\n{")) {
            ArrayNode arrayNode = MAPPER.createArrayNode();
            for (String line : trimmed.split("\n")) {
                if (line.isBlank()) continue;
                arrayNode.add(MAPPER.readTree(line));
            }
            return arrayNode;
        }
        return MAPPER.readTree(trimmed);
    }
}