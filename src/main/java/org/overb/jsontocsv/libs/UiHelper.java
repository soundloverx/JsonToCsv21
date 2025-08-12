package org.overb.jsontocsv.libs;

import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.overb.jsontocsv.App;
import org.overb.jsontocsv.enums.FileDialogTypes;

import java.io.File;
import java.util.Objects;

public class UiHelper {

    private static File lastDirectory = null;

    public static void messageBox(Window owner, AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.initOwner(owner);
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(
                new Image(Objects.requireNonNull(App.class.getResourceAsStream("/icons/j2c-64.png")))
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        centerToOwner(alert);
        alert.showAndWait();
    }

    public static void errorBox(Window owner, Exception error) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(owner);
        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(
                new Image(Objects.requireNonNull(App.class.getResourceAsStream("/icons/j2c-64.png")))
        );
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(error.getMessage());

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setText(ExceptionUtils.getStackTrace(error));
        VBox.setVgrow(textArea, Priority.ALWAYS);

        alert.getDialogPane().setExpandableContent(textArea);
        alert.getDialogPane().setExpanded(true);
        centerToOwner(alert);
        alert.showAndWait();
    }

    public static File openFileChooser(Window owner, FileDialogTypes dialogType, String title, FileChooser.ExtensionFilter... extensionFilters) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        for (FileChooser.ExtensionFilter extensionFilter : extensionFilters) {
            chooser.getExtensionFilters().add(extensionFilter);
        }
        if (lastDirectory != null) {
            chooser.setInitialDirectory(lastDirectory);
        }
        File selectedFile = null;
        if (dialogType == FileDialogTypes.SAVE) {
            selectedFile = chooser.showSaveDialog(owner);
        } else {
            selectedFile = chooser.showOpenDialog(owner);
        }
        if (selectedFile != null) {
            lastDirectory = selectedFile.getParentFile();
        }
        return selectedFile;
    }

    public static File openDirectoryChooser(Window owner, String title) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        if (lastDirectory != null) {
            directoryChooser.setInitialDirectory(lastDirectory);
        }
        File selectedFile = directoryChooser.showDialog(owner);
        if (selectedFile != null) {
            lastDirectory = selectedFile.getParentFile();
        }
        return selectedFile;
    }

    public static void centerToOwner(Window owner, Window modal) {
        EventHandler<WindowEvent> existing = modal.getOnShown();
        modal.setOnShown(e -> {
            double centerX = owner.getX() + (owner.getWidth() - modal.getWidth()) / 2;
            double centerY = owner.getY() + (owner.getHeight() - modal.getHeight()) / 2;
            modal.setX(centerX);
            modal.setY(centerY);
            if (existing != null) existing.handle(e);
        });
    }

    public static void centerToOwner(Dialog<?> dialog) {
        dialog.setOnShown(evt -> {
            Window modalWindow = dialog.getDialogPane().getScene().getWindow();
            Window owner = null;
            if (modalWindow instanceof Stage stage) {
                owner = stage.getOwner();
            } else if (modalWindow instanceof PopupWindow popupWindow) {
                owner = popupWindow.getOwnerWindow();
            }
            if (owner != null) {
                centerToOwner(owner, modalWindow);
            }
        });
    }
}
