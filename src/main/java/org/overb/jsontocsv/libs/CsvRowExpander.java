package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.enums.ColumnTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public final class CsvRowExpander {

    public static ObservableList<Map<String, String>> previewCsvRows(JsonNode loadedJson, String rootPath,
                                                                     List<CsvColumnDefinition> definitions, int limit) {
        ObservableList<Map<String, String>> rows = FXCollections.observableArrayList();
        if (definitions == null || definitions.isEmpty() || loadedJson == null) return rows;

        JsonNode root = JsonPath.navigate(loadedJson, rootPath);
        List<String> headers = headersFrom(definitions);

        for (JsonNode record : toRecordList(root)) {
            streamRecord(loadedJson, record, definitions, headers, row -> {
                if (limit > 0 && rows.size() >= limit) {
                    return;
                }
                Map<String, String> map = new LinkedHashMap<>();
                for (int i = 0; i < headers.size(); i++) {
                    map.put(headers.get(i), row[i]);
                }
                rows.add(map);
            });
            if (limit > 0 && rows.size() >= limit) break;
        }
        if (limit > 0 && rows.size() > limit) {
            return FXCollections.observableArrayList(rows.subList(0, limit));
        }
        return rows;
    }

    public static long streamCsvRows(JsonNode loadedJson, String rootPath, List<CsvColumnDefinition> definitions,
                                     List<String> headers, Consumer<String[]> rowConsumer) {
        if (definitions == null || definitions.isEmpty() || loadedJson == null) {
            return 0L;
        }
        AtomicLong rowCounter = new AtomicLong(0);
        Consumer<String[]> countingConsumer = row -> {
            rowConsumer.accept(row);
            rowCounter.incrementAndGet();
        };

        JsonNode root = JsonPath.navigate(loadedJson, rootPath);
        for (JsonNode record : toRecordList(root)) {
            streamRecord(loadedJson, record, definitions, headers, countingConsumer);
        }
        return rowCounter.get();
    }

    private static List<JsonNode> toRecordList(JsonNode root) {
        if (root == null) return List.of();
        if (root.isArray()) {
            List<JsonNode> list = new ArrayList<>(root.size());
            root.forEach(list::add);
            return list;
        }
        return List.of(root);
    }

    private static void streamRecord(JsonNode loadedJson, JsonNode record, List<CsvColumnDefinition> definitions,
                                     List<String> headers, Consumer<String[]> rowConsumer) {
        Map<String, List<CsvColumnDefinition>> groupedNonFormulas = new LinkedHashMap<>();
        Map<String, List<CsvColumnDefinition>> groupedFormulas = new LinkedHashMap<>();
        List<CsvColumnDefinition> scalars = new ArrayList<>();
        List<CsvColumnDefinition> scalarFormulas = new ArrayList<>();
        for (CsvColumnDefinition def : definitions) {
            boolean isFormula = def.getType() == ColumnTypes.FORMULA;
            String ancestor = JsonPath.findArrayAncestorPath(record, def.getJsonSource());
            if (ancestor == null) {
                if (isFormula) scalarFormulas.add(def);
                else scalars.add(def);
            } else {
                if (isFormula) groupedFormulas.computeIfAbsent(ancestor, k -> new ArrayList<>()).add(def);
                else groupedNonFormulas.computeIfAbsent(ancestor, k -> new ArrayList<>()).add(def);
            }
        }

        List<Map<String, String>> rowsInProgress = new ArrayList<>();
        rowsInProgress.add(new LinkedHashMap<>());
        for (CsvColumnDefinition def : scalars) {
            List<Map<String, String>> next = new ArrayList<>();
            for (Map<String, String> rowInProgress : rowsInProgress) {
                next.addAll(expandNonFormula(rowInProgress, loadedJson, record, def, def.getJsonSource()));
            }
            rowsInProgress = next;
        }
        List<String> groupOrder = new ArrayList<>(groupedNonFormulas.keySet());
        for (String k : groupedFormulas.keySet()) {
            if (!groupOrder.contains(k)) groupOrder.add(k);
        }
        for (Map<String, String> rowInProgress : rowsInProgress) {
            emitRowsForGroups(loadedJson, record, groupOrder, 0, groupedNonFormulas, groupedFormulas,
                    scalarFormulas, rowInProgress, headers, rowConsumer);
        }
    }

    private static void emitRowsForGroups(JsonNode loadedJson, JsonNode record, List<String> groupOrder, int rowIndex,
                                          Map<String, List<CsvColumnDefinition>> groupedNonFormulas,
                                          Map<String, List<CsvColumnDefinition>> groupedFormulas,
                                          List<CsvColumnDefinition> scalarFormulas,
                                          Map<String, String> rowInProgress,
                                          List<String> headers,
                                          java.util.function.Consumer<String[]> rowConsumer) {
        if (rowIndex >= groupOrder.size()) {
            List<Map<String, String>> afterScalarList = applyFormulasAccum(rowInProgress, loadedJson, record, record, scalarFormulas);
            for (Map<String, String> row : afterScalarList) {
                rowConsumer.accept(buildCsvRow(row, headers));
            }
            return;
        }

        String ancestorPath = groupOrder.get(rowIndex);
        List<CsvColumnDefinition> nonFormulas = groupedNonFormulas.getOrDefault(ancestorPath, List.of());
        List<CsvColumnDefinition> formulas = groupedFormulas.getOrDefault(ancestorPath, List.of());

        JsonNode arrayNode = JsonPath.navigate(record, ancestorPath);
        if (!arrayNode.isArray() || arrayNode.isEmpty()) {
            Map<String, String> withNulls = new LinkedHashMap<>(rowInProgress);
            for (CsvColumnDefinition def : nonFormulas) {
                withNulls = FunctionsHelper.putValue(withNulls, def.getColumnName(), null);
            }
            List<Map<String, String>> afterGroupList = applyFormulasAccum(withNulls, loadedJson, record, null, formulas);
            for (Map<String, String> nextRow : afterGroupList) {
                emitRowsForGroups(loadedJson, record, groupOrder, rowIndex + 1,
                        groupedNonFormulas, groupedFormulas, scalarFormulas, nextRow, headers, rowConsumer);
            }
            return;
        }

        for (JsonNode element : arrayNode) {
            List<Map<String, String>> elementRows = new ArrayList<>();
            elementRows.add(new LinkedHashMap<>(rowInProgress));

            for (CsvColumnDefinition csvColumnDefinition : nonFormulas) {
                String relPath = JsonPath.relativePath(csvColumnDefinition.getJsonSource(), ancestorPath);
                List<Map<String, String>> next = new ArrayList<>();
                for (Map<String, String> row : elementRows) {
                    next.addAll(expandNonFormula(row, loadedJson, element, csvColumnDefinition, relPath));
                }
                elementRows = next;
            }

            for (Map<String, String> row : elementRows) {
                List<Map<String, String>> afterGroupList = applyFormulasAccum(row, loadedJson, record, element, formulas);
                for (Map<String, String> nextRow : afterGroupList) {
                    emitRowsForGroups(loadedJson, record, groupOrder, rowIndex + 1,
                            groupedNonFormulas, groupedFormulas, scalarFormulas, nextRow, headers, rowConsumer);
                }
            }
        }
    }

    private static List<Map<String, String>> applyFormulasAccum(Map<String, String> base, JsonNode loadedJson, JsonNode record,
                                                                JsonNode localBase, List<CsvColumnDefinition> formulas) {
        List<Map<String, String>> rowsInProgress = List.of(base);
        for (CsvColumnDefinition f : formulas) {
            List<Map<String, String>> next = new ArrayList<>();
            for (Map<String, String> rowInProgress : rowsInProgress) {
                List<Map<String, String>> res = FunctionsHelper.evaluateFormula(rowInProgress, loadedJson, f, localBase);
                if (res.isEmpty()) {
                    next.add(rowInProgress);
                } else {
                    next.addAll(res);
                }
            }
            rowsInProgress = next;
        }
        return rowsInProgress;
    }


    private static List<Map<String, String>> expandNonFormula(Map<String, String> base, JsonNode loadedJson, JsonNode baseNode,
                                                              CsvColumnDefinition columnDefinition, String effectivePath) {
        // Formulas are handled elsewhere (computed last), so just pass through
        if (columnDefinition.getType() == ColumnTypes.FORMULA) {
            return List.of(base);
        }
        // LITERAL stays the same
        if (columnDefinition.getType() == ColumnTypes.LITERAL) {
            return List.of(FunctionsHelper.putValue(base, columnDefinition.getColumnName(), columnDefinition.getJsonSource()));
        }
        // DEFAULT (and any non-formula custom type that resolves a path)
        List<JsonNode> found = JsonPath.findNodesByPath(baseNode, effectivePath);
        if (found.isEmpty()) {
            return List.of(FunctionsHelper.putValue(base, columnDefinition.getColumnName(), null));
        }
        List<Map<String, String>> out = new ArrayList<>(found.size());
        for (JsonNode node : found) {
            out.add(FunctionsHelper.putValue(base, columnDefinition.getColumnName(), node.isValueNode() ? node.asText() : node.toString()));
        }
        return out;
    }

    public static List<String> headersFrom(List<CsvColumnDefinition> definitions) {
        List<String> headers = new ArrayList<>(definitions.size());
        for (CsvColumnDefinition d : definitions) headers.add(d.getColumnName());
        return headers;
    }

    private static String[] buildCsvRow(Map<String, String> rowInProgress, List<String> headers) {
        String[] row = new String[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            row[i] = rowInProgress.get(headers.get(i));
        }
        return row;
    }
}