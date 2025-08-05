package org.overb.jsontocsv.elements;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@NoArgsConstructor
@Getter
@Setter
public class ApplicationProperties {

    private static final ObjectMapper MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @JsonProperty("auto_convert_on_load")
    private boolean autoConvertOnLoad;

    @JsonProperty("csv_columns_snake_case")
    private boolean columnsSnakeCase;

    @JsonProperty("use_limit_preview_rows")
    private boolean limitedPreviewRows;

    @JsonProperty("limit_preview_rows")
    private int previewLimit;

    public void save() throws IOException {
        Path file = getConfigPath();
        MAPPER.writeValue(Files.newBufferedWriter(file), this);
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
                return MAPPER.readValue(Files.newBufferedReader(configFile), ApplicationProperties.class);
            }
        } catch (IOException e) {
        }
        ApplicationProperties defaultProperties = new ApplicationProperties();
        defaultProperties.setAutoConvertOnLoad(true);
        defaultProperties.setColumnsSnakeCase(true);
        defaultProperties.setLimitedPreviewRows(true);
        defaultProperties.setPreviewLimit(100);
        return defaultProperties;
    }
}
