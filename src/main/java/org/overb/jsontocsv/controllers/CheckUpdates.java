package org.overb.jsontocsv.controllers;

import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.dto.AppVersion;
import org.overb.jsontocsv.dto.UpdateStatus;
import org.overb.jsontocsv.libs.HttpService;
import org.overb.jsontocsv.libs.ThemeManager;
import org.overb.jsontocsv.libs.UiHelper;

import java.util.Objects;

public class CheckUpdates {

    @FXML
    public Label lblCurrent;
    @FXML
    public Label lblRemote;
    @FXML
    public Button btnDownload;
    @FXML
    public Button btnOK;
    private Stage dialogStage;

    public void initialize() {
        lblCurrent.setText("Checking for updates...");
        lblRemote.setVisible(false);
        btnDownload.setDisable(true);
        btnOK.setDisable(false);
    }

    public void checkRemoteStatus() {
        Task<UpdateStatus> task = new Task<>() {
            @Override
            protected UpdateStatus call() throws Exception {
                String url = App.properties.getUpdateStatusFileLink();
                HttpService http = new HttpService();
                return http.fetchUpdateStatus(url);
            }
        };
        task.setOnSucceeded(e -> {
            UpdateStatus result = task.getValue();
            lblCurrent.setText("Current version: " + AppVersion.getCurrentVersion());
            lblRemote.setText("Latest version: " + result.latest());
            UiHelper.messageBox(dialogStage, Alert.AlertType.INFORMATION, "Info", "Current version: " + AppVersion.getCurrentVersion() + "\nStatus version: " + result.latest());
            btnDownload.setDisable(false);
        });
        task.setOnFailed(e -> {
            lblCurrent.setText("Failed to retrieve update info.");
            UiHelper.errorBox(dialogStage, (Exception) task.getException());
        });
        Thread t = new Thread(task, "check-updates-task");
        t.setDaemon(true);
        t.start();
    }

    public static void show(Window owner) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("CheckUpdates.fxml"));
            Parent pane = loader.load();

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.getIcons().add(new Image(Objects.requireNonNull(App.class.getResourceAsStream("/icons/j2c-64.png"))));
            stage.setTitle("Check for updates");
            Scene scene = new Scene(pane);
            if (App.properties.isDarkMode()) {
                ThemeManager.setDark(scene, true);
            }
            stage.setScene(scene);
            stage.setResizable(false);
            CheckUpdates form = loader.getController();
            form.dialogStage = stage;
            stage.setOnShown(event -> form.checkRemoteStatus());
            UiHelper.centerToOwner(owner, stage);
            stage.showAndWait();
        } catch (Exception error) {
            UiHelper.errorBox(owner, error);
        }
    }

    @FXML
    public void okAction(ActionEvent actionEvent) {
        dialogStage.close();
    }
}