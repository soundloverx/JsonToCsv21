package org.overb.jsontocsv.controllers;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Window;
import lombok.Setter;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.enums.ColumnTypes;
import org.overb.jsontocsv.libs.CustomStringUtils;
import org.overb.jsontocsv.libs.UiHelper;

public class EditColumn {
    @FXML
    private TextField nameField;
    @FXML
    private TextArea pathField;
    @FXML
    private ToggleGroup toggleTypeGroup;
    @FXML
    private RadioButton rbDefault;
    @FXML
    private RadioButton rbLiteral;
    @FXML
    private RadioButton rbFormula;

    private Stage dialogStage;
    private Window window;
    private CsvColumnDefinition definitionToEdit;
    @Setter
    private ObservableList<CsvColumnDefinition> csvColumnDefinitions;

    public void initialize() {
        rbDefault.setUserData(ColumnTypes.DEFAULT);
        rbLiteral.setUserData(ColumnTypes.LITERAL);
        rbFormula.setUserData(ColumnTypes.FORMULA);

        nameField.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
            }
        });
    }

    @FXML
    private void onOk() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            dialogStage.close();
            return;
        }
        if (Preferences.applicationProperties.isSnakeCaseColumnNames()) {
            name = CustomStringUtils.generateColumnName(name);
            nameField.setText(name);
        }
        for (CsvColumnDefinition definition : csvColumnDefinitions) {
            if (definition.getCsvColumn().equals(name) && !definition.equals(definitionToEdit)) {
                UiHelper.messageBox(window, Alert.AlertType.ERROR, "Error", "The column name '" + name + "' already exists.");
                return;
            }
        }
        ColumnTypes columnType = (ColumnTypes) toggleTypeGroup.getSelectedToggle().getUserData();
        if (definitionToEdit == null) {
            csvColumnDefinitions.add(new CsvColumnDefinition(name, pathField.getText().trim(), columnType));
        } else {
            definitionToEdit.setType(columnType);
            definitionToEdit.setCsvColumn(name);
            definitionToEdit.setJsonColumn(pathField.getText().trim());
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
        nameField.setText(definitionToEdit.getCsvColumn());
        pathField.setText(definitionToEdit.getJsonColumn());

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