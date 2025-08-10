package org.overb.jsontocsv.libs;

import javafx.scene.Scene;
import javafx.stage.Window;
import org.overb.jsontocsv.App;

import java.util.Objects;

public final class ThemeManager {
    private static final String DARK_CSS = Objects.requireNonNull(App.class.getResource("/style.css")).toExternalForm();

    private ThemeManager() {}

    public static boolean isDark(Scene scene) {
        return scene != null && scene.getStylesheets().contains(DARK_CSS);
    }

    public static void setDark(Scene scene, boolean enable) {
        if (scene == null) return;
        var styles = scene.getStylesheets();
        if (enable) {
            if (!styles.contains(DARK_CSS)) {
                styles.add(DARK_CSS);
            }
        } else {
            styles.remove(DARK_CSS);
        }
    }

    public static void setDarkForAll(boolean enable) {
        for (Window w : Window.getWindows()) {
            if (w.getScene() != null) {
                setDark(w.getScene(), enable);
            }
        }
    }

    public static void toggleAll() {
        boolean anyDark = Window.getWindows().stream()
                .map(Window::getScene)
                .filter(Objects::nonNull)
                .anyMatch(ThemeManager::isDark);
        setDarkForAll(!anyDark);
    }
}
