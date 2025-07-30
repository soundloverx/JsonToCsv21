package org.overb.jsontocsv.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.dto.NamedSchema;
import org.overb.jsontocsv.elements.NamedSchemaTreeCell;
import org.overb.jsontocsv.elements.ReorderableRowFactory;
import org.overb.jsontocsv.enums.ColumnTypes;
import org.overb.jsontocsv.libs.JsonSchemaHelper;
import org.overb.jsontocsv.libs.UiHelper;
import org.overb.jsontocsv.services.CsvService;
import org.overb.jsontocsv.services.JsonService;

import java.io.File;
import java.util.*;

public class Main {

    @FXML
    private TreeView<NamedSchema> tvJsonSchema;
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
    private MenuItem mnuAddDefinition;

    private final ObservableList<CsvColumnDefinition> csvColumnDefinitions = FXCollections.observableArrayList();
    private JsonNode rootNode;
    private Window window;

    @FXML
    public void initialize() {
        mnuAddDefinition.setOnAction(e -> {
            EditColumn.show(window, csvColumnDefinitions, null);
        });
        tvJsonSchema.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
                // also register the menu shortcut so it works regardless of control focus
                KeyCombination keyCombination = mnuAddDefinition.getAccelerator();
                newValue.getAccelerators().put(keyCombination, () -> mnuAddDefinition.fire());
            }
        });
        tvJsonSchema.addEventHandler(TreeItem.branchExpandedEvent(), Event::consume);
        tvJsonSchema.addEventHandler(TreeItem.branchCollapsedEvent(), Event::consume);
        tvJsonSchema.setCellFactory(tv -> new NamedSchemaTreeCell());

        csvNameColumn.setCellValueFactory(new PropertyValueFactory<>("csvColumn"));
        jsonPathColumn.setCellValueFactory(new PropertyValueFactory<>("jsonColumn"));
        customColumn.setCellValueFactory(new PropertyValueFactory<>("custom"));
        customColumn.setCellFactory(CheckBoxTableCell.forTableColumn(customColumn));
        columnDefinitionsTable.setItems(csvColumnDefinitions);
        Callback<TableView<CsvColumnDefinition>, TableRow<CsvColumnDefinition>> reorderFactory = new ReorderableRowFactory<>(csvColumnDefinitions);
        columnDefinitionsTable.setRowFactory(tv -> {
            TableRow<CsvColumnDefinition> row = reorderFactory.call(tv);
            row.setOnMouseClicked(evt -> editColumnDefinition(evt, row));
            return row;
        });
        columnDefinitionsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        csvColumnDefinitions.addListener((ListChangeListener<CsvColumnDefinition>) change -> {
            if (change.next()) {
                generateCsvPreview();
            }
        });
    }

    private void editColumnDefinition(MouseEvent evt, TableRow<CsvColumnDefinition> row) {
        if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2 && !row.isEmpty()) {
            CsvColumnDefinition edited = row.getItem();
            CsvColumnDefinition original = new CsvColumnDefinition(edited);
            EditColumn.show(window, csvColumnDefinitions, edited);
            if (!edited.equals(original)) {
                columnDefinitionsTable.refresh();
                generateCsvPreview();
            }
        }
    }

    @FXML
    private void resetDefinitions() {
        csvTableView.getItems().clear();
        csvColumnDefinitions.clear();
    }

    @FXML
    private void resetEverything() {
        resetDefinitions();
        tvJsonSchema.setRoot(null);
        rootNode = null;
    }

    @FXML
    private void openJsonFile() {
        File file = UiHelper.openFileChooser(window, "Open JSON file", new FileChooser.ExtensionFilter("JSON Files", "*.json"));
        if (file == null) return;
        try {
            resetEverything();
            rootNode = JsonService.loadFromFile(file);
            loadJsonSchemaIntoTree();
            parseJsonIntoCsvColumns(rootNode);
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        }
    }

    private void loadJsonSchemaIntoTree() {
        JsonSchemaHelper.Schema schema = JsonService.buildSchema(rootNode);
        TreeItem<NamedSchema> rootItem = toTreeItem("", schema);
        if (rootItem != null) {
            rootItem.setExpanded(true);
        }
        tvJsonSchema.setRoot(rootItem);
    }

    private TreeItem<NamedSchema> toTreeItem(String name, JsonSchemaHelper.Schema schema) {
        TreeItem<NamedSchema> item;
        if (schema instanceof JsonSchemaHelper.ArraySchema arr) {
            item = new TreeItem<>(new NamedSchema(name, schema));
            item.getChildren().add(toTreeItem("", arr.elementSchema));
        } else if (schema instanceof JsonSchemaHelper.ObjectSchema obj) {
            item = new TreeItem<>(new NamedSchema(name, schema));
            obj.fields.forEach((fieldName, subSchema) -> item.getChildren().add(toTreeItem(fieldName, subSchema)));
        } else {
            item = new TreeItem<>(new NamedSchema(name, schema));
        }
        if ((item.getValue() == null || item.getValue().name().isEmpty()) && (item.getChildren() == null || item.getChildren().isEmpty())) {
            return null;
        }
        return item;
    }

    private void parseJsonIntoCsvColumns(JsonNode rootNode) throws Exception {
        boolean isNested = JsonService.isNested(rootNode);
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
        List<Map<String, String>> previewRows = CsvService.generateCsvPreviewRows(rootNode, csvColumnDefinitions);
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
}