package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.enums.CustomFunctions;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FunctionsHelper {

    private static final DateTimeFormatter timestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static List<Map<String, String>> evaluateFormula(Map<String, String> base, JsonNode loadedJson, CsvColumnDefinition columnDefinition) {
        final Pattern FUNCTION_PATTERN = Pattern.compile("\\s*(\\w+)\\s*\\(\\s*([^)]*)\\s*\\)\\s*");
        String formula = columnDefinition.getJsonColumn();
        Matcher m = FUNCTION_PATTERN.matcher(formula);
        if (!m.matches()) {
            return List.of(putValue(base, columnDefinition.getCsvColumn(), "UNKNOWN FORMULA: " + formula));
        }
        String functionName = m.group(1).toUpperCase();
        String functionArguments = m.group(2);
        String[] args;
        if (functionArguments.isBlank()) {
            args = new String[0];
        } else {
            args = functionArguments.split("\\s*,\\s*");
        }
        CustomFunctions function = CustomFunctions.fromName(functionName);
        if (function.getParameters() != -1 && function.getParameters() != args.length) {
            return List.of(putValue(base, columnDefinition.getCsvColumn(), "ERROR: Wrong number of arguments for " + functionName.toUpperCase()));
        }
        return switch (function) {
            case FIND -> List.of(putValue(base, columnDefinition.getCsvColumn(), doFind(base, loadedJson, args)));
            case CURRENT_TIMESTAMP -> List.of(putValue(base, columnDefinition.getCsvColumn(), doCurrentTimestamp()));
            case CONCAT -> List.of(putValue(base, columnDefinition.getCsvColumn(), doConcat(base, args)));
            default ->
                    List.of(putValue(base, columnDefinition.getCsvColumn(), "UNKNOWN FUNCTION: " + functionName.toUpperCase()));
        };
    }

    static Map<String, String> putValue(Map<String, String> original, String key, String value) {
        Map<String, String> copy = new HashMap<>(original);
        copy.put(key, value);
        return copy;
    }

    private static String doCurrentTimestamp() {
        return LocalDateTime.now().format(timestampFormatter);
    }

    private static String doFind(Map<String, String> base, JsonNode loadedJson, String[] args) {
        String valueKey = args[0].trim();
        String lookupFullPath = args[1].trim();
        String returnField = args[2].trim();
        String searchValue = base.get(valueKey);

        String[] pathSegments = lookupFullPath.split("\\.");
        String compareField = pathSegments[pathSegments.length - 1];
        String parentPath = String.join(".", Arrays.copyOf(pathSegments, pathSegments.length - 1));
        JsonNode parentNode = parentPath.isBlank() ? loadedJson : DataHelper.navigate(loadedJson, parentPath);

        final Iterable<JsonNode> nodesToCheck;
        if (parentNode.isArray()) {
            nodesToCheck = parentNode;
        } else if (parentNode.isObject()) {
            nodesToCheck = Collections.singleton(parentNode);
        } else {
            return null;
        }
        for (JsonNode node : nodesToCheck) {
            JsonNode compareNode = node.path(compareField);
            if (compareNode.isMissingNode()) {
                return null;
            }
            if ((compareNode.isNull() && searchValue == null) || (compareNode.isValueNode() && compareNode.asText().equals(searchValue))) {
                return node.path(returnField).asText(null);
            }
        }
        return null;
    }

    private static String doConcat(Map<String, String> base, String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String argument : args) {
            argument = argument.trim();
            String piece = "";
            if (argument.length() >= 2 && argument.startsWith("'") && argument.endsWith("'")) {
                piece = argument.substring(1, argument.length() - 1);
            } else {
                piece = base.get(argument);
                if (piece == null) {
                    piece = "";
                }
            }
            sb.append(piece);
        }
        return sb.toString();
    }
}
