package org.overb.jsontocsv.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.elements.ApplicationProperties;
import org.overb.jsontocsv.enums.CsvNullStyles;
import org.overb.jsontocsv.libs.ThemeManager;
import org.overb.jsontocsv.libs.UiHelper;

import java.util.Objects;

public class Preferences {

    @FXML
    public TextField txtLimit;
    @FXML
    public CheckBox cbLimitPreview;
    @FXML
    public CheckBox cbSnakeCase;
    @FXML
    public CheckBox cbAutoConvert;
    @FXML
    public ToggleGroup toggleNullTypeGroup;
    @FXML
    public RadioButton cbNullEmpty;
    @FXML
    public RadioButton cbNullNull;
    @FXML
    public CheckBox cbDarkMode;
    private Stage dialogStage;
    private Window window;

    public void initialize() {
        cbDarkMode.setOnAction(event -> {
            ThemeManager.toggleAll();
        });
        cbAutoConvert.setSelected(App.properties.isAutoConvertOnLoad());
        cbSnakeCase.setSelected(App.properties.isColumnsSnakeCase());
        cbLimitPreview.setSelected(App.properties.isLimitedPreviewRows());
        txtLimit.setDisable(!cbLimitPreview.isSelected());
        txtLimit.setText("" + App.properties.getPreviewLimit());
        if (App.properties.getNullType() == CsvNullStyles.EMPTY) {
            cbNullEmpty.setSelected(true);
        } else {
            cbNullNull.setSelected(true);
        }
        cbDarkMode.setSelected(App.properties.isDarkMode());

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
            App.properties.setAutoConvertOnLoad(cbAutoConvert.isSelected());
            App.properties.setColumnsSnakeCase(cbSnakeCase.isSelected());
            App.properties.setLimitedPreviewRows(cbLimitPreview.isSelected());
            int limit;
            try {
                limit = Integer.parseInt(txtLimit.getText());
            } catch (NumberFormatException e) {
                limit = 100;
                txtLimit.setText("100");
            }
            App.properties.setPreviewLimit(limit);
            App.properties.setNullType(cbNullEmpty.isSelected() ? CsvNullStyles.EMPTY : CsvNullStyles.LITERAL_NULL);
            App.properties.setDarkMode(cbDarkMode.isSelected());
            App.properties.save();
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
            Scene scene = new Scene(pane);
            if (App.properties.isDarkMode()) {
                ThemeManager.setDark(scene, true);
            }
            stage.setScene(scene);
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