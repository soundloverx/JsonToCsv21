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
import org.overb.jsontocsv.libs.UiHelper;

public class AddColumn {
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
        for (CsvColumnDefinition definition : csvColumnDefinitions) {
            if (definition.getCsvColumn().equals(name)) {
                UiHelper.messageBox(window, Alert.AlertType.ERROR, "Error", "The column name '" + name + "' already exists.");
                return;
            }
        }
        ColumnTypes columnType = (ColumnTypes) toggleTypeGroup.getSelectedToggle().getUserData();
        csvColumnDefinitions.add(new CsvColumnDefinition(name, pathField.getText().trim(), columnType));
        dialogStage.close();
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    public static void show(Window owner, ObservableList<CsvColumnDefinition> csvColumnDefinitions) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("AddColumn.fxml"));
            Parent pane = loader.load();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Add column...");
            stage.setScene(new Scene(pane));

            AddColumn form = loader.getController();
            form.setCsvColumnDefinitions(csvColumnDefinitions);
            form.dialogStage = stage;

            stage.showAndWait();
        } catch (Exception error) {
            UiHelper.errorBox(owner, error);
        }
    }
}