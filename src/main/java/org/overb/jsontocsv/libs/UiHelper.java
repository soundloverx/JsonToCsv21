package org.overb.jsontocsv.libs;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.overb.jsontocsv.enums.FileDialogTypes;

import java.io.File;

public class UiHelper {

    public static void messageBox(Window owner, AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        centerToOwner(alert);
        alert.showAndWait();
    }

    public static void errorBox(Window owner, Exception error) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.initOwner(owner);
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
        if (dialogType == FileDialogTypes.SAVE) {
            return chooser.showSaveDialog(owner);
        } else {
            return chooser.showOpenDialog(owner);
        }
    }

    public static void centerToOwner(Window owner, Window modal) {
        modal.setOnShown(e -> {
            double centerX = owner.getX() + (owner.getWidth() - modal.getWidth()) / 2;
            double centerY = owner.getY() + (owner.getHeight() - modal.getHeight()) / 2;
            modal.setX(centerX);
            modal.setY(centerY);
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
