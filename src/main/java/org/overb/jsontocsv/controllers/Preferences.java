package org.overb.jsontocsv.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.elements.ApplicationProperties;
import org.overb.jsontocsv.libs.UiHelper;

public class Preferences {

    public static ApplicationProperties applicationProperties = ApplicationProperties.load();
    @FXML
    public TextField txtLimit;
    @FXML
    public CheckBox cbLimitPreview;
    @FXML
    public CheckBox cbSnakeCase;
    @FXML
    public CheckBox cbAutoConvert;
    private Stage dialogStage;
    private Window window;

    public void initialize() {
        cbAutoConvert.setSelected(applicationProperties.isAutoConvertOnLoad());
        cbSnakeCase.setSelected(applicationProperties.isColumnsSnakeCase());
        cbLimitPreview.setSelected(applicationProperties.isLimitedPreviewRows());
        txtLimit.setDisable(!cbLimitPreview.isSelected());
        txtLimit.setText("" + applicationProperties.getPreviewLimit());

        cbLimitPreview.setOnAction(event -> {
            txtLimit.setDisable(!cbLimitPreview.isSelected());
        });
        txtLimit.sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                this.window = newValue.getWindow();
            }
        });
    }

    @FXML
    private void onOk() {
        try {
            applicationProperties.setAutoConvertOnLoad(cbAutoConvert.isSelected());
            applicationProperties.setColumnsSnakeCase(cbSnakeCase.isSelected());
            applicationProperties.setLimitedPreviewRows(cbLimitPreview.isSelected());
            int limit;
            try {
                limit = Integer.parseInt(txtLimit.getText());
            } catch (NumberFormatException e) {
                limit = 100;
                txtLimit.setText("100");
            }
            applicationProperties.setPreviewLimit(limit);
            applicationProperties.save();
        } catch (Exception error) {
            UiHelper.errorBox(window, error);
        }
        dialogStage.close();
    }

    @FXML
    private void onCancel() {
        dialogStage.close();
    }

    public static void show(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("Preferences.fxml"));
            Parent pane = loader.load();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Preferences");
            stage.setScene(new Scene(pane));
            stage.setResizable(false);
            UiHelper.centerToOwner(owner, stage);

            Preferences form = loader.getController();
            form.dialogStage = stage;

            stage.showAndWait();
        } catch (Exception error) {
            UiHelper.errorBox(owner, error);
        }
    }
}