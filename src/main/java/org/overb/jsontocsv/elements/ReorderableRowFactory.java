package org.overb.jsontocsv.elements;

import javafx.collections.ObservableList;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.*;
import javafx.util.Callback;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ReorderableRowFactory<T> implements Callback<TableView<T>, TableRow<T>> {

    private static final DataFormat INDEX_FORMAT = new DataFormat("application/x-java-serialized-object");
    private static final Object TABLE_CONFIGURED_KEY = new Object();
    private final ObservableList<T> backingList;
    private final Deque<List<DeletedItem<T>>> undoStack = new ArrayDeque<>();
    private final Deque<List<DeletedItem<T>>> redoStack = new ArrayDeque<>();

    public ReorderableRowFactory(ObservableList<T> backingList) {
        this.backingList = backingList;
    }

    public void resetHistory() {
        undoStack.clear();
        redoStack.clear();
    }

    private record DeletedItem<U>(U item, int index) {
    }

    @Override
    public TableRow<T> call(TableView<T> tableView) {
        configureTableHandlersOnce(tableView);
        TableRow<T> row = new TableRow<>();
        row.setOnDragDetected(event -> {
            if (row.isEmpty()) {
                return;
            }
            var selectionModel = tableView.getSelectionModel();
            List<Integer> selected = new ArrayList<>(selectionModel.getSelectedIndices());
            // if the row is not selected, start a drag with just that row
            if (!selectionModel.isSelected(row.getIndex())) {
                selected.clear();
                selected.add(row.getIndex());
            }
            if (selected.isEmpty()) {
                return;
            }
            selected.sort(Comparator.naturalOrder());
            Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.put(INDEX_FORMAT, selected);
            db.setContent(content);
            event.consume();
        });

        row.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(INDEX_FORMAT)) {
                event.acceptTransferModes(TransferMode.MOVE);
                event.consume();
            }
        });

        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (!db.hasContent(INDEX_FORMAT)) {
                return;
            }
            List<Integer> draggedIndices = (List<Integer>) db.getContent(INDEX_FORMAT);
            if (draggedIndices == null || draggedIndices.isEmpty()) {
                return;
            }
            List<Integer> selection = draggedIndices.stream().filter(Objects::nonNull).sorted().toList();
            if (selection.isEmpty()) {
                return;
            }
            List<T> draggedItems = selection.stream().map(backingList::get).collect(Collectors.toList());
            // AtomicInteger only for lambda final requirement
            final AtomicInteger dropIndex = new AtomicInteger(row.isEmpty() ? backingList.size() : row.getIndex());

            // when dropping below original positions, we must account for removals
            // adjust dropIndex by subtracting how many selected indices are strictly less than dropIndex
            int lessThanDrop = (int) selection.stream().filter(i -> i < dropIndex.get()).count();
            dropIndex.getAndAdd(-lessThanDrop);

            // remove from the end to avoid reindexing issues
            for (int i = selection.size() - 1; i >= 0; i--) {
                int index = selection.get(i);
                if (index >= 0 && index < backingList.size()) {
                    backingList.remove(index);
                }
            }

            if (dropIndex.get() < 0) dropIndex.set(0);
            if (dropIndex.get() > backingList.size()) {
                dropIndex.set(backingList.size());
            }
            int insertPos = dropIndex.get();
            for (T item : draggedItems) {
                backingList.add(insertPos++, item);
            }
            // reselect moved items
            var sm = tableView.getSelectionModel();
            sm.clearSelection();
            for (T item : draggedItems) {
                sm.select(item);
            }
            event.setDropCompleted(true);
            event.consume();
        });
        return row;
    }

    private void configureTableHandlersOnce(TableView<T> tableView) {
        var props = tableView.getProperties();
        if (props.containsKey(TABLE_CONFIGURED_KEY)) {
            return;
        }
        props.put(TABLE_CONFIGURED_KEY, Boolean.TRUE);

        // Del, Ctrl+Z, Ctrl+Y (deletions only)
        tableView.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            if (evt.getCode() == KeyCode.DELETE) {
                handleDelete(tableView);
                evt.consume();
                return;
            }
            if (evt.getCode() == KeyCode.Z && evt.isControlDown()) {
                handleUndo(tableView);
                evt.consume();
                return;
            }
            if (evt.getCode() == KeyCode.Y && evt.isControlDown()) {
                handleRedo(tableView);
                evt.consume();
            }
        });
    }

    private void handleDelete(TableView<T> tableView) {
        var selectionModel = tableView.getSelectionModel();
        List<Integer> selected = new ArrayList<>(selectionModel.getSelectedIndices());
        if (selected.isEmpty()) {
            return;
        }
        selected.sort(Comparator.naturalOrder());
        List<DeletedItem<T>> deletedBatch = new ArrayList<>(selected.size());
        for (int index : selected) {
            if (index >= 0 && index < backingList.size()) {
                deletedBatch.add(new DeletedItem<>(backingList.get(index), index));
            }
        }
        if (deletedBatch.isEmpty()) {
            return;
        }
        for (int i = selected.size() - 1; i >= 0; i--) {
            int index = selected.get(i);
            if (index >= 0 && index < backingList.size()) {
                backingList.remove(index);
            }
        }
        undoStack.push(deletedBatch);
        redoStack.clear();
        int anchor = Math.min(deletedBatch.getFirst().index, backingList.size() - 1);
        selectionModel.clearSelection();
        if (anchor >= 0 && anchor < backingList.size()) {
            selectionModel.select(anchor);
        }
    }

    private void handleUndo(TableView<T> tableView) {
        if (undoStack.isEmpty()) {
            return;
        }
        var batch = undoStack.pop();
        var selectionModel = tableView.getSelectionModel();
        selectionModel.clearSelection();
        for (DeletedItem<T> item : batch) {
            int insertAt = Math.max(0, Math.min(item.index, backingList.size()));
            backingList.add(insertAt, item.item);
            selectionModel.select(item.item);
        }
        redoStack.push(batch);
    }

    private void handleRedo(TableView<T> tableView) {
        if (redoStack.isEmpty()) {
            return;
        }
        var batch = redoStack.pop();
        var selectionModel = tableView.getSelectionModel();
        selectionModel.clearSelection();
        List<Integer> currentIndexes = batch.stream()
                .map(item -> backingList.indexOf(item.item))
                .filter(index -> index >= 0)
                .sorted(Comparator.reverseOrder())
                .toList();
        for (int index : currentIndexes) {
            backingList.remove(index);
        }
        int anchor = currentIndexes.isEmpty() ? -1 : Math.min(currentIndexes.getLast(), backingList.size() - 1);
        if (anchor >= 0 && anchor < backingList.size()) {
            selectionModel.select(anchor);
        }
        undoStack.push(batch);
    }
}