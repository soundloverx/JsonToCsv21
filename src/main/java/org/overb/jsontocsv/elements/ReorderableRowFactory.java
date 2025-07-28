package org.overb.jsontocsv.elements;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.util.Callback;

public class ReorderableRowFactory<T> implements Callback<TableView<T>, TableRow<T>> {
    private static final DataFormat INDEX_FORMAT = new DataFormat("application/x-java-serialized-object");
    private final ObservableList<T> backingList;

    public ReorderableRowFactory(ObservableList<T> backingList) {
        this.backingList = backingList;
    }

    @Override
    public TableRow<T> call(TableView<T> tableView) {
        TableRow<T> row = new TableRow<>();

        row.setOnDragDetected(event -> {
            if (!row.isEmpty()) {
                int index = row.getIndex();
                Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.put(INDEX_FORMAT, index);
                db.setContent(content);
                event.consume();
            }
        });

        row.setOnDragOver(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(INDEX_FORMAT)) {
                int draggedIndex = (Integer) db.getContent(INDEX_FORMAT);
                if (row.getIndex() != draggedIndex) {
                    event.acceptTransferModes(TransferMode.MOVE);
                    event.consume();
                }
            }
        });

        row.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasContent(INDEX_FORMAT)) {
                int draggedIndex = (Integer) db.getContent(INDEX_FORMAT);
                T draggedItem = backingList.get(draggedIndex);
                int dropIndex = row.isEmpty() ? backingList.size() : row.getIndex();
                backingList.remove(draggedIndex);
                if (dropIndex > draggedIndex) {
                    dropIndex--;
                }
                backingList.add(dropIndex, draggedItem);
                event.setDropCompleted(true);
                tableView.getSelectionModel().select(draggedItem);
                event.consume();
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem mnuRemove = new MenuItem("Remove");
        mnuRemove.setOnAction(e -> {
            T item = row.getItem();
            if (item != null) {
                backingList.remove(item);
            }
        });
        contextMenu.getItems().add(mnuRemove);
        row.contextMenuProperty().bind(
                Bindings.when(row.emptyProperty())
                        .then((ContextMenu) null)
                        .otherwise(contextMenu)
        );
        return row;
    }
}
