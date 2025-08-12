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
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.dto.AppVersion;
import org.overb.jsontocsv.dto.UpdateStatus;
import org.overb.jsontocsv.elements.ApplicationProperties;
import org.overb.jsontocsv.libs.HttpService;
import org.overb.jsontocsv.libs.ThemeManager;
import org.overb.jsontocsv.libs.UiHelper;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class CheckUpdates {

    private static File downloadedUpdate = null;
    @FXML
    public StackPane formContainer;
    @FXML
    public VBox mainVbox;
    @FXML
    public Label lblCurrent;
    @FXML
    public Label lblRemote;
    @FXML
    public Button btnDownload;
    @FXML
    public Button btnOK;
    @FXML
    public TextArea txtNotes;
    private Stage dialogStage;
    private final HttpService http = new HttpService();

    public void initialize() {
        lblCurrent.setText("Checking for updates...");
        lblRemote.setVisible(false);
        btnDownload.setDisable(true);
        btnDownload.setVisible(false);
        btnOK.setDisable(false);
        txtNotes.managedProperty().bind(txtNotes.visibleProperty());
        txtNotes.setVisible(false);
    }

    public void checkRemoteStatus() {
        Task<UpdateStatus> task = new Task<>() {
            @Override
            protected UpdateStatus call() throws Exception {
                String url = ApplicationProperties.updateStatusFileLink;
                return http.fetchUpdateStatus(url);
            }
        };
        task.setOnSucceeded(e -> setDownloadState(task.getValue()));
        task.setOnFailed(e -> {
            lblCurrent.setText("Failed to retrieve update info.");
            UiHelper.errorBox(dialogStage, (Exception) task.getException());
        });
        Thread t = new Thread(task, "check-updates-task");
        t.setDaemon(true);
        t.start();
    }

    private void setDownloadState(UpdateStatus updateStatus) {
        try {
            AppVersion current = new AppVersion(ApplicationProperties.version);
            AppVersion latest = new AppVersion(updateStatus.latest());
            lblCurrent.setText("Current version: " + current);
            lblRemote.setText("Latest version: " + latest);
            lblRemote.setVisible(true);
            if (current.compareTo(latest) >= 0) {
                txtNotes.setText("You have the latest version.");
                txtNotes.setMinHeight(40);
                txtNotes.setPrefHeight(40);
            } else {
                txtNotes.setText("Patch notes:\n");
                if (updateStatus.notes() != null && updateStatus.notes().length > 0) {
                    for (String note : updateStatus.notes()) {
                        txtNotes.setText(txtNotes.getText() + "\n" + note);
                    }
                    int textHeight = (updateStatus.notes().length + 1) * 25;
                    if (textHeight > 200) {
                        textHeight = 200;
                    }
                    txtNotes.setMinHeight(textHeight);
                    txtNotes.setPrefHeight(textHeight);
                    dialogStage.setResizable(true);
                }
                btnDownload.setVisible(true);
                if (downloadedUpdate != null && downloadedUpdate.exists()) {
                    btnDownload.setDisable(true);
                    btnDownload.setText("Already downloaded");
                } else {
                    btnDownload.setDisable(false);
                    btnDownload.setUserData(updateStatus.link());
                }
            }
            txtNotes.setVisible(true);
            dialogStage.setMinHeight(150 + txtNotes.getHeight());
            dialogStage.sizeToScene();
        } catch (Exception error) {
            UiHelper.errorBox(dialogStage, error);
        }
    }

    @FXML
    public void okAction(ActionEvent actionEvent) {
        dialogStage.close();
    }

    @FXML
    public void download(ActionEvent actionEvent) {
        btnDownload.setText("Downloading...");
        btnDownload.setDisable(true);
        String link = btnDownload.getUserData() == null ? "" : btnDownload.getUserData().toString();
        File selectedDir = UiHelper.openDirectoryChooser(dialogStage, "Select download Location");
        if (selectedDir == null) {
            btnDownload.setText("Download");
            btnDownload.setDisable(false);
            return;
        }
        try {
            Path downloadFilePath = http.downloadTo(link, selectedDir.toPath());
            btnDownload.setText("Done");
            downloadedUpdate = downloadFilePath.toFile();
            UiHelper.messageBox(dialogStage, Alert.AlertType.INFORMATION, "Download complete",
                    "You can now close the app and run the installer downloaded at\n" + downloadFilePath);
        } catch (Exception ex) {
            UiHelper.errorBox(dialogStage, ex);
            btnDownload.setText("Download");
            btnDownload.setDisable(false);
        }
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
            stage.setMinWidth(280);
            stage.setMinHeight(150);
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
}