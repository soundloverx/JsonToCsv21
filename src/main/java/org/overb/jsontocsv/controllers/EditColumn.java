package org.overb.jsontocsv.controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.Setter;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.enums.ColumnTypes;
import org.overb.jsontocsv.libs.CustomStringUtils;
import org.overb.jsontocsv.libs.UiHelper;

import java.awt.*;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class EditColumn {
    @FXML
    private TextField txtColumnName;
    @FXML
    private TextArea txtJsonSource;
    @FXML
    private ToggleGroup toggleTypeGroup;
    @FXML
    private RadioButton rbDefault;
    @FXML
    private RadioButton rbLiteral;
    @FXML
    private RadioButton rbFormula;
    @FXML
    private Button helpButton;

    private Stage dialogStage;
    private Window window;
    private CsvColumnDefinition definitionToEdit;
    @Setter
    private ObservableList<CsvColumnDefinition> csvColumnDefinitions;

    public void initialize() {
        rbDefault.setUserData(ColumnTypes.DEFAULT);
        rbLiteral.setUserData(ColumnTypes.LITERAL);
        rbFormula.setUserData(ColumnTypes.FORMULA);

        txtColumnName.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
            }
        });
    }

    @FXML
    private void onHelp() {
        try {
            URL resource = App.class.getResource("/formulas.html");
            if (resource == null) {
                UiHelper.messageBox(window, Alert.AlertType.ERROR, "Error", "Could not find formulas.html in resources.");
                return;
            }
            if ("file".equalsIgnoreCase(resource.getProtocol())) {
                Desktop.getDesktop().browse(resource.toURI());
                return;
            }
            Path tmp = Files.createTempFile("formulas", ".html");
            tmp.toFile().deleteOnExit();
            try (InputStream in = resource.openStream()) {
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
            }
            Desktop.getDesktop().browse(tmp.toUri());
        } catch (Exception ex) {
            UiHelper.errorBox(window, ex);
        }
    }

    @FXML
    private void onOk() {
        String name = txtColumnName.getText().trim();
        if (name.isEmpty()) {
            UiHelper.messageBox(window, Alert.AlertType.ERROR, "Error", "Column name cannot be empty.");
            return;
        }
        if (Preferences.applicationProperties.isColumnsSnakeCase()) {
            name = CustomStringUtils.generateColumnName(name);
            txtColumnName.setText(name);
        }
        for (CsvColumnDefinition definition : csvColumnDefinitions) {
            if (definition.getColumnName().equals(name) && !definition.equals(definitionToEdit)) {
                UiHelper.messageBox(window, Alert.AlertType.ERROR, "Error", "The column name '" + name + "' already exists.");
                return;
            }
        }
        ColumnTypes columnType = (ColumnTypes) toggleTypeGroup.getSelectedToggle().getUserData();
        if (definitionToEdit == null) {
            csvColumnDefinitions.add(new CsvColumnDefinition(name, txtJsonSource.getText().trim(), columnType));
        } else {
            definitionToEdit.setType(columnType);
            definitionToEdit.setColumnName(name);
            definitionToEdit.setJsonSource(txtJsonSource.getText().trim());
        }
        dialogStage.close();
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    public void setDefinitionToEdit(CsvColumnDefinition definitionToEdit) {
        if (definitionToEdit == null) {
            return;
        }
        this.definitionToEdit = definitionToEdit;
        txtColumnName.setText(definitionToEdit.getColumnName());
        txtJsonSource.setText(definitionToEdit.getJsonSource());

        switch (definitionToEdit.getType()) {
            case LITERAL -> rbLiteral.setSelected(true);
            case FORMULA -> rbFormula.setSelected(true);
            default -> rbDefault.setSelected(true);
        }
    }

    public static void show(Window owner, ObservableList<CsvColumnDefinition> csvColumnDefinitions, CsvColumnDefinition definitionToEdit) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("EditColumn.fxml"));
            Parent pane = loader.load();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle((definitionToEdit == null ? "Add " : "Edit ") + " column...");
            stage.setScene(new Scene(pane));
            UiHelper.centerToOwner(owner, stage);

            EditColumn form = loader.getController();
            form.setCsvColumnDefinitions(csvColumnDefinitions);
            form.setDefinitionToEdit(definitionToEdit);
            form.dialogStage = stage;

            stage.showAndWait();
        } catch (Exception error) {
            UiHelper.errorBox(owner, error);
        }
    }
}