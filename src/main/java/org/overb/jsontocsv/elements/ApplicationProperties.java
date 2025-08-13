package org.overb.jsontocsv.elements;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.overb.jsontocsv.enums.CsvNullStyles;
import org.overb.jsontocsv.libs.JsonIo;
import org.overb.jsontocsv.libs.ThemeManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@NoArgsConstructor
@Getter
@Setter
public class ApplicationProperties {

    @JsonProperty("auto_convert_on_load")
    private boolean autoConvertOnLoad;

    @JsonProperty("csv_columns_snake_case")
    private boolean columnsSnakeCase;

    @JsonProperty("use_limit_preview_rows")
    private boolean limitedPreviewRows;

    @JsonProperty("limit_preview_rows")
    private int previewLimit;

    @JsonProperty("null_type")
    private CsvNullStyles nullType;

    @JsonProperty("dark_mode")
    private boolean darkMode;

    @JsonProperty("recent_files")
    private List<String> recentFiles = new ArrayList<>();

    @JsonIgnore
    public static final String updateStatusFileLink;
    @JsonIgnore
    public static final String version;

    static {
        Properties properties = new Properties();
        try (var in = ApplicationProperties.class.getResourceAsStream("/app.properties")) {
            if (in != null) properties.load(in);
        } catch (Exception ignore) {
        }
        updateStatusFileLink = properties.getProperty("update");
        version = properties.getProperty("version");
    }

    public void save() throws IOException {
        Path configFile = getConfigPath();
        Files.createDirectories(configFile.getParent());
        try (var w = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
            JsonIo.MAPPER.writerWithDefaultPrettyPrinter().writeValue(w, this);
        }
    }

    public void addRecentFile(String path) {
        if (path == null) return;
        recentFiles.remove(path);
        recentFiles.addFirst(path);
        while (recentFiles.size() > 8) {
            recentFiles.removeLast();
        }
        try {
            save();
        } catch (IOException ignore) {
        }
    }

    public void removeRecentFile(String path) {
        recentFiles.remove(path);
        try {
            save();
        } catch (IOException ignore) {
        }
    }

    public void clearRecentFiles() {
        recentFiles.clear();
        try {
            save();
        } catch (IOException ignore) {
        }
    }

    private static Path getConfigPath() throws IOException {
        Path configDir = Paths.get(
                System.getProperty("os.name").toLowerCase().contains("win")
                        ? System.getenv("APPDATA")
                        : System.getProperty("user.home"),
                "org.overb.JsonToCsv"
        );
        Files.createDirectories(configDir);
        return configDir.resolve("settings.json");
    }

    public static ApplicationProperties load() {
        try {
            Path configFile = getConfigPath();
            if (Files.exists(configFile)) {
                try (var r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                    ApplicationProperties properties = JsonIo.MAPPER.readValue(r, ApplicationProperties.class);
                    if (properties.isDarkMode()) {
                        ThemeManager.setDarkForAll(true);
                    }
                    return properties;
                }
            }
        } catch (IOException e) {
        }
        ApplicationProperties defaultProperties = new ApplicationProperties();
        defaultProperties.setAutoConvertOnLoad(true);
        defaultProperties.setColumnsSnakeCase(true);
        defaultProperties.setLimitedPreviewRows(true);
        defaultProperties.setPreviewLimit(100);
        defaultProperties.setNullType(CsvNullStyles.EMPTY);
        defaultProperties.setDarkMode(false);
        return defaultProperties;
    }
}
