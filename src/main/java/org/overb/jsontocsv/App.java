package org.overb.jsontocsv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.overb.jsontocsv.elements.ApplicationProperties;
import org.overb.jsontocsv.libs.ThemeManager;

import java.io.IOException;

public class App extends Application {

    public static ApplicationProperties properties = ApplicationProperties.load();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("Main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        if (App.properties.isDarkMode()) {
            ThemeManager.setDark(scene, true);
        }
        stage.setTitle("Json to CSV");
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}