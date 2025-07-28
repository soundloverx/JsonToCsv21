package org.overb.jsontocsv.libs;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.File;

public class UiHelper {

    public static void messageBox(Window owner, AlertType alertType, String title, String content) {
        Alert alert = new Alert(alertType);
        alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
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

        alert.showAndWait();
    }

    public static File openFileChooser(Window owner, String title, FileChooser.ExtensionFilter... extensionFilters) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        for (FileChooser.ExtensionFilter extensionFilter : extensionFilters) {
            chooser.getExtensionFilters().add(extensionFilter);
        }
        return chooser.showOpenDialog(owner);
    }
}
