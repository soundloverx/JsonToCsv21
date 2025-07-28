package org.overb.jsontocsv.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import org.overb.jsontocsv.dto.CsvColumnDefinition;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvService {

    public List<Map<String, String>> generateCsvPreviewRows(JsonNode rootNode, List<CsvColumnDefinition> csvColumnDefinitions) {
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


}
