package org.overb.jsontocsv.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.elements.ReorderableRowFactory;
import org.overb.jsontocsv.enums.ColumnTypes;
import org.overb.jsontocsv.libs.JsonHelper;
import org.overb.jsontocsv.libs.UiHelper;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public class Main {

    @FXML
    private TextArea jsonTextArea;
    @FXML
    private TableView<CsvColumnDefinition> columnDefinitionsTable;
    @FXML
    private TableColumn<CsvColumnDefinition, String> csvNameColumn;
    @FXML
    private TableColumn<CsvColumnDefinition, String> jsonPathColumn;
    @FXML
    private TableColumn<CsvColumnDefinition, Boolean> customColumn;
    @FXML
    private TableView<Map<String, String>> csvTableView;
    @FXML
    private Button newButton;
    @FXML
    private Button openJsonButton;
    @FXML
    private Button addCsvColumn;

    private final ObservableList<CsvColumnDefinition> csvColumnDefinitions = FXCollections.observableArrayList();
    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode rootNode;
    private Window window;

    @FXML
    public void initialize() {
        newButton.setOnAction(e -> resetEverything());
        openJsonButton.setOnAction(e -> openJsonFile());
        openJsonButton.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
            }
        });

        csvNameColumn.setCellValueFactory(new PropertyValueFactory<>("csvColumn"));
        jsonPathColumn.setCellValueFactory(new PropertyValueFactory<>("jsonColumn"));
        customColumn.setCellValueFactory(new PropertyValueFactory<>("custom"));
        customColumn.setCellFactory(CheckBoxTableCell.forTableColumn(customColumn));
        columnDefinitionsTable.setItems(csvColumnDefinitions);
        columnDefinitionsTable.setRowFactory(new ReorderableRowFactory<>(csvColumnDefinitions));
        columnDefinitionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        csvColumnDefinitions.addListener((ListChangeListener<CsvColumnDefinition>) change -> {
            while (change.next()) {
                generateCsvPreview();
            }
        });

        addCsvColumn.setOnAction(e -> {
            AddColumn.show(window, csvColumnDefinitions);
        });
    }

    private void resetEverything() {
        jsonTextArea.clear();
        csvTableView.getItems().clear();
        csvColumnDefinitions.clear();
        rootNode = null;
    }

    private void openJsonFile() {
        File file = UiHelper.openFileChooser(jsonTextArea.getScene().getWindow(), "Open JSON file", new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        if (file == null) return;
        try {
            resetEverything();
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
            jsonTextArea.setText(rootNode.toPrettyString());

            parseJsonIntoCsvColumns(rootNode);
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        }
    }

    private void parseJsonIntoCsvColumns(JsonNode rootNode) throws Exception {
        boolean isNested = JsonHelper.isNested(rootNode);
        if (isNested) {
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info", "You have loaded a nested JSON.\nYou have to manually configure the CSV columns.");
            return;
        }
        if (rootNode.isArray() && !rootNode.isEmpty()) {
            loadSimpleJson(rootNode);
        }
    }

    private void loadSimpleJson(JsonNode rootNode) throws Exception {
        Set<String> columns = new LinkedHashSet<>();
        for (JsonNode node : rootNode) {
            Iterator<String> iterator = node.fieldNames();
            while (iterator.hasNext()) {
                columns.add(iterator.next());
            }
        }
        for (String columnName : columns) {
            csvColumnDefinitions.add(new CsvColumnDefinition(columnName, columnName, ColumnTypes.DEFAULT));
        }
    }

    private void generateCsvPreview() {
        if (rootNode == null) return;
        List<Map<String, String>> previewRows = extractSimpleCsvRows();
        csvTableView.getColumns().clear();
        for (CsvColumnDefinition def : csvColumnDefinitions) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(def.getCsvColumn());
            col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().get(def.getCsvColumn())));
            col.setReorderable(false);
            csvTableView.getColumns().add(col);
        }
        ObservableList<Map<String, String>> data = FXCollections.observableArrayList(previewRows);
        csvTableView.setItems(data);
    }

    private List<Map<String, String>> extractSimpleCsvRows() {
        List<Map<String, String>> rows = new ArrayList<>();
        int limit = 20;
        for (JsonNode node : rootNode) {
            Map<String, String> row = new HashMap<>();
            for (CsvColumnDefinition def : csvColumnDefinitions) {
                String value = switch (def.getType()) {
                    case DEFAULT -> extractJsonValue(node, def.getJsonColumn());
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

    private String extractJsonValue(JsonNode rootNode, String fieldName) {
        if (rootNode.get(fieldName) == null || rootNode.get(fieldName).isNull()) {
            return null;
        }
        return rootNode.get(fieldName).asText();
    }
}