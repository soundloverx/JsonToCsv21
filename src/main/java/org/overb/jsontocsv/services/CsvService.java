package org.overb.jsontocsv.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.enums.ColumnTypes;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CsvService {

    public static List<Map<String, String>> generateCsvPreviewRows(JsonNode rootNode, List<CsvColumnDefinition> csvColumnDefinitions) {
        List<Map<String, String>> rows = new ArrayList<>();
        int limit = 20;
        for (JsonNode node : rootNode) {
            Map<String, String> row = new HashMap<>();
            for (CsvColumnDefinition def : csvColumnDefinitions) {
                String value = switch (def.getType()) {
                    case DEFAULT -> JsonService.extractJsonValue(node, def.getJsonColumn());
                    case LITERAL -> def.getJsonColumn();
                    default -> null;
                };
                row.put(def.getCsvColumn(), value);
            }
            rows.add(row);
            if (--limit == 0) {
                break;
            }
        }
        return rows;
    }

    public static ObservableList<Map<String, String>> previewRows(JsonNode json, String rootPath, List<CsvColumnDefinition> definitions) throws IllegalArgumentException {
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
        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        for (JsonNode record : records) {
            List<Map<String, String>> contexts = new ArrayList<>();
            contexts.add(new HashMap<>());
            for (CsvColumnDefinition def : definitions) {
                String relPath = def.getJsonColumn();
                List<JsonNode> found = JsonService.findNodesByPath(record, relPath);
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
        }
        return rows;
    }
}
