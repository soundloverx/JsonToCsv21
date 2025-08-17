package org.overb.jsontocsv.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.opencsv.CSVWriter;
import com.opencsv.ICSVWriter;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.dto.CsvDefinitionsBundle;
import org.overb.jsontocsv.dto.JsonDragNode;
import org.overb.jsontocsv.dto.NamedSchema;
import org.overb.jsontocsv.elements.NamedSchemaTreeCell;
import org.overb.jsontocsv.elements.ReorderableRowFactory;
import org.overb.jsontocsv.elements.RootValidator;
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
    public MenuItem mnuUpdate;
    @FXML
    public Menu mnuRecentFiles;
    @FXML
    public TextField txtSchemaSearch;
    @FXML
    private Label lblColumnsCount;
    @FXML
    private Label lblPreviewRowsCount;
    @FXML
    private Label lblPreviewTime;
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
    private ReorderableRowFactory<CsvColumnDefinition> reorderFactory;
    private JsonNode loadedJson;
    private JsonSchemaHelper.Schema currentSchema;
    private TreeItem<NamedSchema> fullSchemaRoot;
    private Window window;
    private Task<ObservableList<Map<String, String>>> currentPreviewTask;
    private boolean definitionsChanged = false;
    private boolean closeHandlerRegistered = false;

    @FXML
    public void initialize() {
        rebuildRecentFilesMenu();
        txtSchemaSearch.textProperty().addListener((obs, oldV, newV) -> applySchemaFilter());
        mnuAddDefinition.setOnAction(e -> EditColumn.show(window, csvColumnDefinitions, null));
        mnuPreferences.setOnAction(e -> {
            Preferences.show(window);
            generateCsvPreview();
        });
        mnuRefresh.setOnAction(e -> generateCsvPreview());
        mnuUpdate.setOnAction(e -> CheckUpdates.show(window));
        tvJsonSchema.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
                if (!closeHandlerRegistered && this.window instanceof Stage stage) {
                    closeHandlerRegistered = true;
                    stage.setOnCloseRequest(evt -> {
                        if (!promptSaveIfNeeded()) {
                            evt.consume();
                        }
                    });
                }
            }
        });
        tvJsonSchema.setCellFactory(tv -> new NamedSchemaTreeCell());
        tvJsonSchema.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tvJsonSchema.setOnDragOver(evt -> {
            if (evt.getGestureSource() != tvJsonSchema) {
                Dragboard db = evt.getDragboard();
                if (db.hasFiles() && db.getFiles().stream().anyMatch(f -> f.getName().endsWith(".json") || f.getName().endsWith(".json.gz"))) {
                    evt.acceptTransferModes(TransferMode.COPY);
                }
            }
            evt.consume();
        });
        tvJsonSchema.setOnDragDropped(evt -> {
            Dragboard db = evt.getDragboard();
            if (db.hasFiles()) {
                Optional<File> fileOpt = db.getFiles().stream().filter(f -> f.getName().toLowerCase().endsWith(".json") || f.getName().toLowerCase().endsWith(".json.gz")).findFirst();
                fileOpt.ifPresent(this::loadJson);
            }
            evt.setDropCompleted(true);
            evt.consume();
        });

        csvNameColumn.setCellValueFactory(new PropertyValueFactory<>("columnName"));
        jsonPathColumn.setCellValueFactory(new PropertyValueFactory<>("jsonSource"));
        customColumn.setCellValueFactory(new PropertyValueFactory<>("custom"));
        customColumn.setCellFactory(CheckBoxTableCell.forTableColumn(customColumn));

        tblColumnDefinitions.setItems(csvColumnDefinitions);
        tblColumnDefinitions.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        reorderFactory = new ReorderableRowFactory<>(csvColumnDefinitions);
        tblColumnDefinitions.setRowFactory(tv -> {
            TableRow<CsvColumnDefinition> row = reorderFactory.call(tv);
            row.setOnMouseClicked(evt -> editColumnDefinition(evt, row));
            return row;
        });
        tblColumnDefinitions.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        csvColumnDefinitions.addListener((ListChangeListener<CsvColumnDefinition>) change -> {
            updateColumnsCounter();
            if (change.next()) {
                definitionsChanged = true;
                generateCsvPreview();
            }
        });

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
            if (evt.getGestureSource() != tblColumnDefinitions) {
                if (evt.getDragboard().hasContent(NAMED_SCHEMA_LIST)) {
                    evt.acceptTransferModes(TransferMode.COPY);
                } else if (evt.getDragboard().hasFiles() && evt.getDragboard().getFiles().stream().anyMatch(f -> f.getName().toLowerCase().endsWith(".j2csv"))) {
                    evt.acceptTransferModes(TransferMode.COPY);
                }
            }
            evt.consume();
        });
        tblColumnDefinitions.setOnDragDropped(evt -> {
            Dragboard db = evt.getDragboard();
            if (db.hasFiles()) {
                Optional<File> fileOpt = db.getFiles().stream().filter(f -> f.getName().toLowerCase().endsWith(".j2csv")).findFirst();
                fileOpt.ifPresent(this::loadCsvDefinitions);
                evt.setDropCompleted(true);
                evt.consume();
                return;
            }
            if (!db.hasContent(NAMED_SCHEMA_LIST)) {
                evt.setDropCompleted(false);
                evt.consume();
            }
            List<JsonDragNode> items = ((List<JsonDragNode>) db.getContent(NAMED_SCHEMA_LIST))
                    .stream().filter(item -> item.schemaClass().equals(JsonSchemaHelper.PrimitiveSchema.class.getSimpleName()))
                    .toList();
            if (items.isEmpty()) {
                evt.setDropCompleted(false);
                evt.consume();
            }
            ObservableList<CsvColumnDefinition> csvDefinitions = tblColumnDefinitions.getItems();
            for (JsonDragNode item : items) {
                String csvColumn = item.node().toLowerCase();
                if (App.properties.isColumnsSnakeCase()) {
                    csvColumn = CustomStringUtils.generateColumnName(item.node());
                }
                String uniqueColumnName = ensureUniqueColumnName(csvColumn, csvDefinitions);
                CsvColumnDefinition def = new CsvColumnDefinition();
                def.setColumnName(uniqueColumnName);
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
        txtRoot.textProperty().addListener((obs, ov, nv) -> RootValidator.validateRootField(loadedJson, currentSchema, txtRoot));
        RootValidator.validateRootField(loadedJson, currentSchema, txtRoot);
        updateColumnsCounter();
        setPreviewCounters(0, "-");
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
        tblCsvPreview.setItems(FXCollections.observableArrayList());
        csvColumnDefinitions.clear();
        if (reorderFactory != null) {
            reorderFactory.resetHistory();
        }
        tblColumnDefinitions.getItems().clear();
        txtRoot.setText(null);
        currentSchema = null;
        updateColumnsCounter();
        setPreviewCounters(0, "-");
    }

    @FXML
    private void resetEverything() {
        resetDefinitions();
        tvJsonSchema.setRoot(null);
        fullSchemaRoot = null;
        loadedJson = null;
        setPreviewCounters(0, "-");
    }

    @FXML
    public void rebuildDefinitions(ActionEvent actionEvent) {
        if (loadedJson == null) {
            return;
        }
        resetDefinitions();
        try {
            setControlsEnabled(false);
            parseJsonIntoCsvColumns(loadedJson);
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            setControlsEnabled(true);
        }
    }

    @FXML
    private void mnuLoadJsonFile() {
        File file = UiHelper.openFileChooser(window, FileDialogTypes.LOAD, "Open JSON file (*.json | *.json.gz)",
                new FileChooser.ExtensionFilter("JSON Files", "*.json", "*.json.gz"),
                new FileChooser.ExtensionFilter("All files", "*.*"));
        if (file == null) return;
        loadJson(file);
    }

    private void loadJson(File file) {
        try {
            setControlsEnabled(false);
            loadedJson = JsonIo.loadJsonFile(file);
            loadJsonSchemaIntoTree();
            if (App.properties.isAutoConvertOnLoad() && csvColumnDefinitions.isEmpty()) {
                parseJsonIntoCsvColumns(loadedJson);
            } else {
                setPreviewCounters(0, "-");
                generateCsvPreview();
            }
            App.properties.addRecentFile(file.getAbsolutePath());
            rebuildRecentFilesMenu();
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            setControlsEnabled(true);
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
        setControlsEnabled(false);
        try (CSVWriter writer = new CSVWriter(new FileWriter(file), ',', ICSVWriter.NO_QUOTE_CHARACTER, ICSVWriter.NO_ESCAPE_CHARACTER, "\n")) {
            var rowConsumer = CsvRowConsumer.rowWriter(writer, App.properties.getNullType());
            rowConsumer.accept(headers.toArray(new String[0]));
            long rows = CsvRowExpander.streamCsvRows(loadedJson, txtRoot.getText(), csvColumnDefinitions, headers, rowConsumer);
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info", "Saved " + rows + " rows to " + file.getName());
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            setControlsEnabled(true);
        }
    }

    private void loadJsonSchemaIntoTree() {
        currentSchema = JsonSchemaService.buildJsonSchema(loadedJson);
        TreeItem<NamedSchema> rootItem = toTreeItem("", currentSchema);
        fullSchemaRoot = rootItem;
        tvJsonSchema.setRoot(rootItem);
        expandAll(rootItem);
        applySchemaFilter();
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

    private void setControlsEnabled(boolean enabled) {
        tblColumnDefinitions.setDisable(!enabled);
        tblCsvPreview.setDisable(!enabled);
        tvJsonSchema.setDisable(!enabled);
        txtRoot.setDisable(!enabled);
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
        } else if (csvColumnDefinitions.isEmpty()) {
            Optional<String> recommendedRoot = JsonRootDetector.detectSuggestedRoot(loadedJson);
            if (recommendedRoot.isPresent()) {
                txtRoot.setText(recommendedRoot.get());
                loadSimpleJson(JsonPath.navigate(rootNode, recommendedRoot.get()));
                if (csvColumnDefinitions.isEmpty()) {
                    txtRoot.setText(null);
                    UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info",
                            "Unable to automatically detect the data root in the nested JSON.");
                }
            } else {
                txtRoot.setText(null);
                UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Info",
                        "Unable to automatically detect the data root in the nested JSON.");
            }
            RootValidator.validateRootField(loadedJson, currentSchema, txtRoot);
        }
        generateCsvPreview();
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
            String csvColumn = columnName.toLowerCase();
            if (App.properties.isColumnsSnakeCase()) {
                csvColumn = CustomStringUtils.generateColumnName(columnName);
            }
            String uniqueColumnName = ensureUniqueColumnName(csvColumn, csvColumnDefinitions);
            csvColumnDefinitions.add(new CsvColumnDefinition(uniqueColumnName, columnName, ColumnTypes.DEFAULT));
        }
    }

    private String ensureUniqueColumnName(String columnName, ObservableList<CsvColumnDefinition> csvDefinitions) {
        String uniqueColumn = columnName;
        int suffix = 0;
        while (true) {
            Set<String> existing = csvDefinitions.stream()
                    .map(col -> col.getColumnName().toLowerCase())
                    .collect(Collectors.toSet());
            if (existing.contains(uniqueColumn.toLowerCase())) {
                uniqueColumn = columnName + "_" + ++suffix;
                continue;
            }
            break;
        }
        return uniqueColumn;
    }

    private void generateCsvPreview() {
        if (loadedJson == null) {
            setPreviewCounters(0, "-");
            return;
        }
        RootValidator.validateRootField(loadedJson, currentSchema, txtRoot);
        if (csvColumnDefinitions.isEmpty()) {
            tblCsvPreview.getColumns().clear();
            tblCsvPreview.setItems(FXCollections.observableArrayList());
            tblCsvPreview.setPlaceholder(new Label("No columns"));
            setPreviewCounters(0, "-");
            return;
        }
        tblCsvPreview.getColumns().clear();
        for (CsvColumnDefinition def : csvColumnDefinitions) {
            TableColumn<Map<String, String>, String> col = new TableColumn<>(def.getColumnName());
            col.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().get(def.getColumnName())));
            col.setReorderable(false);
            tblCsvPreview.getColumns().add(col);
        }
        final String root = txtRoot.getText();
        final List<CsvColumnDefinition> defsSnapshot = new ArrayList<>(csvColumnDefinitions);
        final int limit = App.properties.isLimitedPreviewRows() ? App.properties.getPreviewLimit() : 0;
        if (currentPreviewTask != null && currentPreviewTask.isRunning()) {
            currentPreviewTask.cancel();
        }
        tblCsvPreview.setPlaceholder(new ProgressIndicator());
        long lastPreviewStartNanos = System.nanoTime();
        setPreviewCounters(0, "…");

        currentPreviewTask = new Task<>() {
            @Override
            protected ObservableList<Map<String, String>> call() {
                return CsvRowExpander.previewCsvRows(loadedJson, root, defsSnapshot, limit);
            }
        };
        currentPreviewTask.setOnSucceeded(e -> {
            ObservableList<Map<String, String>> rows = currentPreviewTask.getValue();
            if (rows == null) {
                rows = FXCollections.observableArrayList();
            }
            tblCsvPreview.setItems(rows);
            long elapsedMs = Math.max(0, (System.nanoTime() - lastPreviewStartNanos) / 1_000_000);
            int rowCount = rows.size();
            tblCsvPreview.setPlaceholder(rowCount == 0 ? new Label("No rows") : new Label(""));
            setPreviewCounters(rowCount, elapsedMs + " ms");
        });

        currentPreviewTask.setOnFailed(e -> {
            UiHelper.errorBox(window, (Exception) currentPreviewTask.getException());
            tblCsvPreview.setItems(FXCollections.observableArrayList()); // ensure list present
            tblCsvPreview.setPlaceholder(new Label("Error"));
            setPreviewCounters(0, "error");
        });

        currentPreviewTask.setOnCancelled(e -> {
            tblCsvPreview.setItems(FXCollections.observableArrayList()); // ensure list present
            tblCsvPreview.setPlaceholder(new Label("Cancelled"));
            setPreviewCounters(0, "cancelled");
        });

        new Thread(currentPreviewTask, "preview-builder").start();
    }

    public void mnuLoadCsvDefinitions(ActionEvent actionEvent) {
        if (!promptSaveIfNeeded()) {
            return;
        }
        File file = UiHelper.openFileChooser(window, FileDialogTypes.LOAD, "Load J2CSV definitions", new FileChooser.ExtensionFilter("J2CSV Files (*.j2csv)", "*.j2csv"));
        if (file == null) return;
        loadCsvDefinitions(file);
    }

    private void loadCsvDefinitions(File file) {
        if (!file.exists() || !file.isFile() || !file.getName().toLowerCase(Locale.ROOT).endsWith(".j2csv")) {
            App.properties.removeRecentFile(file.getAbsolutePath());
            UiHelper.messageBox(window, Alert.AlertType.ERROR, "Error", "File does not exist or is not a J2CSV file.");
            return;
        }
        setControlsEnabled(false);
        try {
            CsvDefinitionsBundle bundle = JsonIo.MAPPER.readValue(file, CsvDefinitionsBundle.class);
            txtRoot.setText(bundle.root() != null ? bundle.root() : "");
            csvColumnDefinitions.setAll(bundle.definitions() != null ? bundle.definitions() : List.of());
            if (reorderFactory != null) {
                reorderFactory.resetHistory();
            }
            definitionsChanged = false;
            tblColumnDefinitions.refresh();
            generateCsvPreview();
            App.properties.addRecentFile(file.getAbsolutePath());
            rebuildRecentFilesMenu();
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            setControlsEnabled(true);
        }
    }

    public void saveCsvDefinitions(ActionEvent actionEvent) {
        saveCsvDefinitionsInteractive();
    }

    private boolean promptSaveIfNeeded() {
        if (!definitionsChanged) {
            return true;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initOwner(window);
        alert.setTitle("Unsaved changes");
        alert.setHeaderText("You have unsaved CSV definitions.");
        alert.setContentText("Do you want to save your changes before continuing?");
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get() == ButtonType.CANCEL) {
            return false;
        }
        if (result.get() == ButtonType.NO) {
            return true;
        }
        return saveCsvDefinitionsInteractive();
    }

    public boolean saveCsvDefinitionsInteractive() {
        if (csvColumnDefinitions.isEmpty()) {
            UiHelper.messageBox(window, Alert.AlertType.INFORMATION, "Alert", "Nothing to save.");
            return true;
        }
        File file = UiHelper.openFileChooser(window, FileDialogTypes.SAVE, "SAVE J2CSV definitions", new FileChooser.ExtensionFilter("J2CSV Files (*.j2csv)", "*.j2csv"));
        if (file == null) {
            return false;
        }
        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".j2csv")) {
            file = new File(file.getParentFile(), file.getName() + ".j2csv");
        }
        setControlsEnabled(false);
        try {
            CsvDefinitionsBundle toSave = new CsvDefinitionsBundle(txtRoot.getText(), csvColumnDefinitions);
            JsonIo.MAPPER.writerWithDefaultPrettyPrinter().writeValue(file, toSave);
            definitionsChanged = false;
            return true;
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        } finally {
            setControlsEnabled(true);
        }
        return false;
    }

    private void updateColumnsCounter() {
        if (lblColumnsCount != null) {
            lblColumnsCount.setText(String.valueOf(csvColumnDefinitions.size()));
        }
    }

    private void setPreviewCounters(int rows, String timeText) {
        if (lblPreviewRowsCount != null) {
            lblPreviewRowsCount.setText(String.valueOf(rows));
        }
        if (lblPreviewTime != null) {
            lblPreviewTime.setText(timeText == null ? "-" : timeText);
        }
    }

    private void rebuildRecentFilesMenu() {
        if (mnuRecentFiles == null) return;
        mnuRecentFiles.getItems().clear();
        if (App.properties.getRecentFiles().isEmpty()) {
            MenuItem empty = new MenuItem("(no recent files)");
            empty.setDisable(true);
            mnuRecentFiles.getItems().add(empty);
            return;
        }
        for (String filePath : App.properties.getRecentFiles()) {
            MenuItem mi = new MenuItem(filePath);
            mi.setOnAction(evt -> {
                if (!promptSaveIfNeeded()) {
                    return;
                }
                File file = new File(((MenuItem) evt.getSource()).getText());
                if (file.getName().endsWith(".j2csv")) {
                    loadCsvDefinitions(file);
                } else {
                    loadJson(file);
                }
            });
            mnuRecentFiles.getItems().add(mi);
        }
        mnuRecentFiles.getItems().add(new SeparatorMenuItem());
        MenuItem clear = new MenuItem("Clear recent files");
        clear.setOnAction(e -> {
            App.properties.clearRecentFiles();
            rebuildRecentFilesMenu();
        });
        mnuRecentFiles.getItems().add(clear);
    }

    private void applySchemaFilter() {
        if (tvJsonSchema == null) return;
        if (fullSchemaRoot == null) return;

        String q = txtSchemaSearch == null ? "" : Optional.ofNullable(txtSchemaSearch.getText()).orElse("");
        q = q.trim().toLowerCase(Locale.ROOT);

        if (q.isEmpty()) {
            tvJsonSchema.setRoot(fullSchemaRoot);
            expandAll(fullSchemaRoot);
            return;
        }

        TreeItem<NamedSchema> filtered = filterTree(fullSchemaRoot, "", q);
        tvJsonSchema.setRoot(filtered);
        expandAll(filtered);
    }

    private TreeItem<NamedSchema> filterTree(TreeItem<NamedSchema> source, String pathPrefix, String q) {
        if (source == null || source.getValue() == null) return null;

        String name = Optional.ofNullable(source.getValue().name()).orElse("");
        String fullPath = joinPath(pathPrefix, name);

        // Recreate node and include children that match
        TreeItem<NamedSchema> copy = new TreeItem<>(source.getValue());

        // Filter children recursively
        for (TreeItem<NamedSchema> child : source.getChildren()) {
            TreeItem<NamedSchema> filteredChild = filterTree(child, fullPath, q);
            if (filteredChild != null) {
                copy.getChildren().add(filteredChild);
            }
        }

        boolean nameMatches = name.toLowerCase(Locale.ROOT).contains(q);
        boolean pathMatches = fullPath.toLowerCase(Locale.ROOT).contains(q);

        // Keep node if it matches by itself or has any matching descendants
        if (nameMatches || pathMatches || !copy.getChildren().isEmpty()) {
            return copy;
        }
        return null;
    }

    private String joinPath(String prefix, String name) {
        String n = Optional.ofNullable(name).orElse("");
        if (prefix == null || prefix.isEmpty()) return n;
        if (n == null || n.isEmpty()) return prefix;
        return prefix + "." + n;
    }
}