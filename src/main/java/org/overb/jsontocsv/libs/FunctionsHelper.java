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

    static List<Map<String, String>> evaluateFormula(Map<String, String> base,
                                                     JsonNode loadedJson,
                                                     CsvColumnDefinition columnDefinition,
                                                     JsonNode localBase) {
        final Pattern FUNCTION_PATTERN = Pattern.compile("\\s*(\\w+)\\s*\\(\\s*([^)]*)\\s*\\)\\s*");
        String formula = columnDefinition.getJsonSource();
        Matcher m = FUNCTION_PATTERN.matcher(formula);
        if (!m.matches()) {
            return List.of(putValue(base, columnDefinition.getColumnName(), "UNKNOWN FORMULA: " + formula));
        }
        String functionName = m.group(1).toUpperCase();
        String functionArguments = m.group(2);
        String[] rawArgs = functionArguments.isBlank() ? new String[0] : CustomStringUtils.splitStringsRespectingQuotes(functionArguments).toArray(String[]::new);

        String[] resolvedArgs = new String[rawArgs.length];
        for (int i = 0; i < rawArgs.length; i++) {
            resolvedArgs[i] = rawArgs[i].trim();
        }

        CustomFunctions function = CustomFunctions.fromName(functionName);
        if (function.getParameters() != -1 && function.getParameters() != resolvedArgs.length) {
            return List.of(putValue(base, columnDefinition.getColumnName(), "ERROR: Wrong number of arguments for " + functionName.toUpperCase()));
        }

        return switch (function) {
            case FIND -> {
                String res = doFind(base, loadedJson,
                        new String[]{
                                CustomStringUtils.unquoteIfQuoted(resolvedArgs[0]),
                                CustomStringUtils.unquoteIfQuoted(resolvedArgs[1]),
                                CustomStringUtils.unquoteIfQuoted(resolvedArgs[2])
                        }
                );
                yield List.of(putValue(base, columnDefinition.getColumnName(), res));
            }
            case CURRENT_TIMESTAMP -> List.of(putValue(base, columnDefinition.getColumnName(), doCurrentTimestamp()));
            case CONCAT -> List.of(putValue(base, columnDefinition.getColumnName(), doConcat(base, resolvedArgs)));
            case JSON ->
                    List.of(putValue(base, columnDefinition.getColumnName(), doJson(loadedJson, localBase, CustomStringUtils.unquoteIfQuoted(resolvedArgs[0]))));
            default ->
                    List.of(putValue(base, columnDefinition.getColumnName(), "UNKNOWN FUNCTION: " + functionName.toUpperCase()));
        };
    }

    static Map<String, String> putValue(Map<String, String> original, String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(original);
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
        JsonNode parentNode = parentPath.isBlank() ? loadedJson : JsonPath.navigate(loadedJson, parentPath);

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
            if ((compareNode.isNull() && searchValue == null) || (compareNode.isValueNode() && Objects.equals(compareNode.asText(), searchValue))) {
                return node.path(returnField).asText(null);
            }
        }
        return null;
    }

    private static String doConcat(Map<String, String> base, String[] args) {
        StringBuilder sb = new StringBuilder();
        for (String argument : args) {
            String arg = argument.trim();
            String piece;
            if (arg.length() >= 2 && arg.startsWith("'") && arg.endsWith("'")) {
                piece = arg.substring(1, arg.length() - 1);
            } else {
                piece = base.get(arg);
                if (piece == null) piece = "";
            }
            sb.append(piece);
        }
        return sb.toString();
    }

    private static String doJson(JsonNode loadedJson, JsonNode localBase, String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        JsonNode base = (localBase != null) ? localBase : loadedJson;
        if (base == null) {
            return null;
        }
        JsonNode node = JsonPath.navigate(base, path);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.toString();
    }
}