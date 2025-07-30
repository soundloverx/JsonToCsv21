package org.overb.jsontocsv.elements;

import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import org.overb.jsontocsv.dto.NamedSchema;
import org.overb.jsontocsv.libs.JsonSchemaHelper;

public class NamedSchemaTreeCell extends TreeCell<NamedSchema> {

    @Override
    protected void updateItem(NamedSchema ns, boolean empty) {
        super.updateItem(ns, empty);
        if (empty || ns == null) {
            setText(null);
        } else {
            TreeItem<NamedSchema> item = getTreeItem();
            if (item != null) {
                item.setExpanded(true);
            }
            String typeHint = switch (ns.schema()) {
                case JsonSchemaHelper.ObjectSchema o -> " {}";
                case JsonSchemaHelper.ArraySchema a -> " []";
                default -> "";
            };
            setText(ns.name() + typeHint);
        }
    }
}
