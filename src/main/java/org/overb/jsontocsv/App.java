package org.overb.jsontocsv;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import org.overb.jsontocsv.elements.ApplicationProperties;
import org.overb.jsontocsv.libs.ThemeManager;

import java.io.IOException;
import java.util.Objects;

public class App extends Application {

    public static ApplicationProperties properties = ApplicationProperties.load();

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("Main.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 900, 600);
        if (App.properties.isDarkMode()) {
            ThemeManager.setDark(scene, true);
        }
        stage.getIcons().addAll(
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/j2c-16.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/j2c-32.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/j2c-48.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/j2c-64.png"))),
                new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/j2c-128.png")))
        );
        stage.setTitle("Json to CSV converter (v" + ApplicationProperties.version + ")");
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}