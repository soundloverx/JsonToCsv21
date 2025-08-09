package org.overb.jsontocsv.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.CSVWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Callback;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.dto.JsonDragNode;
import org.overb.jsontocsv.dto.NamedSchema;
import org.overb.jsontocsv.elements.NamedSchemaTreeCell;
import org.overb.jsontocsv.elements.ReorderableRowFactory;
import org.overb.jsontocsv.enums.ColumnTypes;
import org.overb.jsontocsv.enums.FileDialogTypes;
import org.overb.jsontocsv.libs.*;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    @FXML
    public TextField txtRoot;
    @FXML
    public MenuItem mnuRefresh;
    @FXML
    public MenuItem mnuPreferences;
    @FXML
    private TreeView<NamedSchema> tvJsonSchema;
    @FXML
    private TableView<CsvColumnDefinition> tblColumnDefinitions;
    @FXML
    private TableColumn<CsvColumnDefinition, String> csvNameColumn;
    @FXML
    private TableColumn<CsvColumnDefinition, String> jsonPathColumn;
    @FXML
    private TableColumn<CsvColumnDefinition, Boolean> customColumn;
    @FXML
    private TableView<Map<String, String>> tblCsvPreview;
    @FXML
    private MenuItem mnuAddDefinition;

    private static final DataFormat NAMED_SCHEMA_LIST = new DataFormat("application/x-java-named-schema-list");
    private final ObservableList<CsvColumnDefinition> csvColumnDefinitions = FXCollections.observableArrayList();
    private JsonNode loadedJson;
    private Window window;

    @FXML
    public void initialize() {
        mnuAddDefinition.setOnAction(e -> {
            EditColumn.show(window, csvColumnDefinitions, null);
        });
        mnuPreferences.setOnAction(e -> {
            Preferences.show(window);
            generateCsvPreview();
        });
        mnuRefresh.setOnAction(e -> {
            generateCsvPreview();
        });
        tvJsonSchema.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
            }
        });
        tvJsonSchema.setCellFactory(tv -> new NamedSchemaTreeCell());
        tvJsonSchema.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        csvNameColumn.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        jsonPathColumn.setCellValueFactory(new PropertyValueFactory<>("jsonSource"));
        customColumn.setCellValueFactory(new PropertyValueFactory<>("custom"));
        customColumn.setCellFactory(CheckBoxTableCell.forTableColumn(customColumn));
        tblColumnDefinitions.setItems(csvColumnDefinitions);
        Callback<TableView<CsvColumnDefinition>, TableRow<CsvColumnDefinition>> reorderFactory = new ReorderableRowFactory<>(csvColumnDefinitions);
        tblColumnDefinitions.setRowFactory(tv -> {
            TableRow<CsvColumnDefinition> row = reorderFactory.call(tv);
            row.setOnMouseClicked(evt -> editColumnDefinition(evt, row));
            return row;
        });
        tblColumnDefinitions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        csvColumnDefinitions.addListener((ListChangeListener<CsvColumnDefinition>) change -> {
            if (change.next()) {
                generateCsvPreview();
            }
        });

        // drag & drop from the json schema into the csv definitions:
        tvJsonSchema.setOnDragDetected(evt -> {
            var selected = tvJsonSchema.getSelectionModel().getSelectedItems();
            if (selected.isEmpty()) return;
            Dragboard db = tvJsonSchema.startDragAndDrop(TransferMode.COPY);
            ClipboardContent content = new ClipboardContent();
            List<JsonDragNode> selection = selected.stream()
                    .map(item -> JsonDragNode.of(item.getValue().name(), item.getValue().schema().getClass().getSimpleName(), buildFullTreePath(item)))
                    .toList();
            content.put(NAMED_SCHEMA_LIST, selection);
            db.setContent(content);
            evt.consume();
        });
        tblColumnDefinitions.setOnDragOver(evt -> {
            if (evt.getGestureSource() != tblColumnDefinitions && evt.getDragboard().hasContent(NAMED_SCHEMA_LIST)) {
                evt.acceptTransferModes(TransferMode.COPY);
            }
            evt.consume();
        });
        tblColumnDefinitions.setOnDragDropped(evt -> {
            Dragboard db = evt.getDragboard();
            if (!db.hasContent(NAMED_SCHEMA_LIST)) {
                evt.setDropCompleted(false);
                evt.consume();
            }
            @SuppressWarnings("unchecked")
            List<JsonDragNode> items = ((List<JsonDragNode>) db.getContent(NAMED_SCHEMA_LIST))
                    .stream().filter(item -> item.schemaClass().equals(JsonSchemaHelper.PrimitiveSchema.class.getSimpleName()))
                    .toList();
            if (items.isEmpty()) {
                evt.setDropCompleted(false);
                evt.consume();
            }
            ObservableList<CsvColumnDefinition> csvDefinitions = tblColumnDefinitions.getItems();
            for (JsonDragNode item : items) {
                String csvColumn = item.node();
                if (Preferences.applicationProperties.isColumnsSnakeCase()) {
                    csvColumn = CustomStringUtils.generateColumnName(item.node());
                }

                Set<String> existing = csvDefinitions.stream()
                        .map(CsvColumnDefinition::getColumnName)
                        .collect(Collectors.toSet());
                if (existing.contains(csvColumn)) {
                    csvColumn = csvColumn + "_" + UUID.randomUUID();
                }
                CsvColumnDefinition def = new CsvColumnDefinition();
                def.setColumnName(csvColumn);
                def.setJsonSource(item.node());
                def.setType(ColumnTypes.DEFAULT);
                csvDefinitions.add(def);
            }
            evt.setDropCompleted(true);
            evt.consume();
        });
        txtRoot.setOnDragOver(evt -> {
            if (evt.getGestureSource() == tvJsonSchema && evt.getDragboard().hasContent(NAMED_SCHEMA_LIST)) {
                evt.acceptTransferModes(TransferMode.COPY);
            }
            evt.consume();
        });
        txtRoot.setOnDragDropped(evt -> {
            Dragboard db = evt.getDragboard();
            if (!db.hasContent(NAMED_SCHEMA_LIST)) {
                evt.setDropCompleted(false);
                evt.consume();
            }
            @SuppressWarnings("unchecked")
            List<JsonDragNode> items = ((List<JsonDragNode>) db.getContent(NAMED_SCHEMA_LIST))
                    .stream().filter(item -> item.schemaClass().equals(JsonSchemaHelper.ArraySchema.class.getSimpleName()))
                    .toList();
            if (items.isEmpty()) {
                evt.setDropCompleted(false);
                evt.consume();
            }
            txtRoot.setText(items.getFirst().fullPath());
            generateCsvPreview();
            evt.setDropCompleted(true);
            evt.consume();
        });
    }

    private String buildFullTreePath(TreeItem<NamedSchema> item) {
        List<String> parts = new ArrayList<>();
        TreeItem<NamedSchema> node = item;
        while (node != null && node.getValue() != null && node.getParent() != null) {
            parts.add(node.getValue().name());
            node = node.getParent();
        }
        Collections.reverse(parts);
        return String.join(".", parts);
    }

    private void editColumnDefinition(MouseEvent evt, TableRow<CsvColumnDefinition> row) {
        if (evt.getButton() == MouseButton.PRIMARY && evt.getClickCount() == 2 && !row.isEmpty()) {
            CsvColumnDefinition edited = row.getItem();
            CsvColumnDefinition original = new CsvColumnDefinition(edited);
            EditColumn.show(window, csvColumnDefinitions, edited);
            if (!edited.equals(original)) {
                tblColumnDefinitions.refresh();
                generateCsvPreview();
            }
        }
    }

    @FXML
    private void resetDefinitions() {
        tblCsvPreview.getItems().clear();
        csvColumnDefinitions.clear();
        txtRoot.setText(null);
    }

    @FXML
    private void resetEverything() {
        resetDefinitions();
        tvJsonSchema.setRoot(null);
        loadedJson = null;
    }

    @FXML
    private void loadJsonFile() {
        File file = UiHelper.openFileChooser(window, FileDialogTypes.LOAD, "Open JSON file", new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));
        if (file == null) return;
        try {
            resetEverything();
            tblColumnDefinitions.setDisable(true);
            tblCsvPreview.setDisable(true);
            tvJsonSchema.setDisable(true);
            loadedJson = JsonIo.loadJsonFile(file);
            loadJsonSchemaIntoTree();
            if (Preferences.applicationProperties.isAutoConvertOnLoad()) {
                parseJsonIntoCsvColumns(loadedJson);
            }
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            tblColumnDefinitions.setDisable(false);
            tblCsvPreview.setDisable(false);
            tvJsonSchema.setDisable(false);
        }
    }

    @FXML
    public void saveCsv(ActionEvent actionEvent) {
        if (csvColumnDefinitions.isEmpty()) {
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Alert", "Nothing to save.");
            return;
        }
        File file = UiHelper.openFileChooser(window, FileDialogTypes.SAVE, "SAVE CSV", new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
        if (file == null) return;

        List<String> headers = csvColumnDefinitions.stream()
                .map(CsvColumnDefinition::getColumnName)
                .toList();
        tblColumnDefinitions.setDisable(true);
        tblCsvPreview.setDisable(true);
        tvJsonSchema.setDisable(true);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file))) {
            writer.writeNext(headers.toArray(new String[0]));
            long rows = CsvRowExpander.streamCsvRows(loadedJson, txtRoot.getText(), csvColumnDefinitions, headers, writer::writeNext);
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info", "Saved " + rows + " rows to " + file.getName());
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            tblColumnDefinitions.setDisable(false);
            tblCsvPreview.setDisable(false);
            tvJsonSchema.setDisable(false);
        }
    }

    private void loadJsonSchemaIntoTree() {
        JsonSchemaHelper.Schema schema = JsonSchemaService.buildJsonSchema(loadedJson);
        TreeItem<NamedSchema> rootItem = toTreeItem("", schema);
        tvJsonSchema.setRoot(rootItem);
        expandAll(rootItem);
    }

    private void expandAll(TreeItem<?> root) {
        if (root == null) {
            return;
        }
        root.setExpanded(true);
        for (TreeItem<?> child : root.getChildren()) {
            expandAll(child);
        }
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
        if (JsonSchemaService.isShallow(rootNode)) {
            loadSimpleJson(rootNode);
        } else if (rootNode.isArray() && !rootNode.isEmpty()) {
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info", "You have loaded a nested JSON.\nYou have to manually configure the CSV columns.\nRemember to fill in the root node name.");
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
            String csvColumn = columnName;
            if (Preferences.applicationProperties.isColumnsSnakeCase()) {
                csvColumn = CustomStringUtils.generateColumnName(columnName);
            }
            csvColumnDefinitions.add(new CsvColumnDefinition(csvColumn, columnName, ColumnTypes.DEFAULT));
        }
    }

    private void generateCsvPreview() {
        if (loadedJson == null) return;
        tblCsvPreview.getColumns().clear();
        if (csvColumnDefinitions.isEmpty()) {
            return;
        }
        for (CsvColumnDefinition def : csvColumnDefinitions) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(def.getColumnName());
            col.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().get(def.getColumnName())));
            col.setReorderable(false);
            tblCsvPreview.getColumns().add(col);
        }
        try {
            int limit = Preferences.applicationProperties.getPreviewLimit();
            if (!Preferences.applicationProperties.isLimitedPreviewRows()) {
                limit = 0;
            }
            tblCsvPreview.setItems(CsvRowExpander.previewCsvRows(loadedJson, txtRoot.getText(), csvColumnDefinitions, limit));
        } catch (Exception e) {
            UiHelper.errorBox(window, e);
        }
    }

    public void loadCsvDefinitions(ActionEvent actionEvent) {
        File file = UiHelper.openFileChooser(window, FileDialogTypes.LOAD, "Load J2CSV definitions", new FileChooser.ExtensionFilter("J2CSV Files (*.j2csv)", "*.j2csv"));
        if (file == null) return;

        tblColumnDefinitions.setDisable(true);
        tblCsvPreview.setDisable(true);
        tvJsonSchema.setDisable(true);
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<CsvColumnDefinition> loaded = mapper.readValue(
                    file,
                    new TypeReference<List<CsvColumnDefinition>>() {
                    }
            );

            csvColumnDefinitions.clear();
            csvColumnDefinitions.addAll(loaded);
            tblColumnDefinitions.refresh();
            generateCsvPreview();
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            tblColumnDefinitions.setDisable(false);
            tblCsvPreview.setDisable(false);
            tvJsonSchema.setDisable(false);
        }

    }

    public void saveCsvDefinitions(ActionEvent actionEvent) {
        if (csvColumnDefinitions.isEmpty()) {
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Alert", "Nothing to save.");
            return;
        }
        File file = UiHelper.openFileChooser(window, FileDialogTypes.SAVE, "SAVE J2CSV definitions", new FileChooser.ExtensionFilter("J2CSV Files (*.j2csv)", "*.j2csv"));
        if (file == null) return;
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".j2csv")) {
            file = new File(file.getParentFile(), file.getName() + ".j2csv");
        }

        tblColumnDefinitions.setDisable(true);
        tblCsvPreview.setDisable(true);
        tvJsonSchema.setDisable(true);
        try {
            ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(file, new ArrayList<>(csvColumnDefinitions));
//            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info", "Saved " + csvColumnDefinitions.size() + " column definitions to " + file.getName());
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            tblColumnDefinitions.setDisable(false);
            tblCsvPreview.setDisable(false);
            tvJsonSchema.setDisable(false);
        }
    }
}