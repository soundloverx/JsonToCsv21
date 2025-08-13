package org.overb.jsontocsv.elements;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import org.overb.jsontocsv.libs.JsonSchemaHelper;

import java.util.Arrays;
import java.util.Optional;

public class RootValidator {

    private enum RootValidationResults {
        OK_ARRAY, NOT_FOUND, NOT_ARRAY, NO_JSON, EMPTY
    }

    public static void validateRootField(JsonNode loadedJson, JsonSchemaHelper.Schema currentSchema, TextField txtRoot) {
        String styleOk = "";
        String styleError = "-fx-border-color: #d33; -fx-border-width: 2; -fx-background-color: -fx-control-inner-background;";
        Tooltip tip;
        if (loadedJson == null || currentSchema == null) {
            txtRoot.setStyle(styleOk);
            tip = new Tooltip("Load a JSON file to validate the Root path.");
            txtRoot.setTooltip(tip);
            return;
        }
        String value = Optional.ofNullable(txtRoot.getText()).map(String::trim).orElse("");
        if (value.isEmpty()) {
            txtRoot.setStyle(styleOk);
            tip = new Tooltip("Root is empty. For nested JSON, select an array path (e.g., orders.items). For shallow arrays, empty may be fine.");
            txtRoot.setTooltip(tip);
            return;
        }
        ValidationResult res = checkPathAgainstSchema(currentSchema, value);
        switch (res.result) {
            case OK_ARRAY -> {
                txtRoot.setStyle(styleOk);
                String sizeTxt = (res.schema instanceof JsonSchemaHelper.ArraySchema a && a.size() != null) ? (" Size ≈ " + a.size()) : "";
                tip = new Tooltip("Root resolves to an array." + sizeTxt);
            }
            case NOT_FOUND -> {
                txtRoot.setStyle(styleError);
                tip = new Tooltip("Path not found in schema: " + value);
            }
            case NOT_ARRAY -> {
                txtRoot.setStyle(styleError);
                tip = new Tooltip("Path exists but is not an array. Select an array that contains rows.");
            }
            case NO_JSON -> {
                txtRoot.setStyle(styleOk);
                tip = new Tooltip("Load a JSON file to validate the Root path.");
            }
            case EMPTY -> {
                txtRoot.setStyle(styleOk);
                tip = new Tooltip("Root is empty. For nested JSON, select an array path.");
            }
            default -> {
                txtRoot.setStyle(styleOk);
                tip = new Tooltip("");
            }
        }
        txtRoot.setTooltip(tip);
    }

    private record ValidationResult(RootValidationResults result, JsonSchemaHelper.Schema schema) {
    }

    private static ValidationResult checkPathAgainstSchema(JsonSchemaHelper.Schema currentSchema, String path) {
        if (currentSchema == null) {
            return new ValidationResult(RootValidationResults.NO_JSON, null);
        }
        if (path == null || path.isBlank()) {
            return new ValidationResult(RootValidationResults.EMPTY, null);
        }
        String[] parts = Arrays.stream(path.split("\\."))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        JsonSchemaHelper.Schema node = currentSchema;
        for (String part : parts) {
            if (node instanceof JsonSchemaHelper.ObjectSchema obj) {
                JsonSchemaHelper.Schema next = obj.fields.get(part);
                if (next == null) {
                    return new ValidationResult(RootValidationResults.NOT_FOUND, null);
                }
                node = next;
            } else if (node instanceof JsonSchemaHelper.ArraySchema) {
                return new ValidationResult(RootValidationResults.NOT_FOUND, null);
            } else {
                return new ValidationResult(RootValidationResults.NOT_FOUND, null);
            }
        }
        if (node instanceof JsonSchemaHelper.ArraySchema) {
            return new ValidationResult(RootValidationResults.OK_ARRAY, node);
        } else if (node == null) {
            return new ValidationResult(RootValidationResults.NOT_FOUND, null);
        } else {
            return new ValidationResult(RootValidationResults.NOT_ARRAY, node);
        }
    }
}
