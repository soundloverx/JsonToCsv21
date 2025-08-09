package org.overb.jsontocsv.elements;

import javafx.scene.control.TreeCell;
import org.overb.jsontocsv.dto.NamedSchema;
import org.overb.jsontocsv.libs.JsonSchemaHelper;

public class NamedSchemaTreeCell extends TreeCell<NamedSchema> {

    @Override
    protected void updateItem(NamedSchema ns, boolean empty) {
        super.updateItem(ns, empty);
        if (empty || ns == null) {
            setText(null);
            return;
        }
        String typeHint = switch (ns.schema()) {
            case JsonSchemaHelper.ObjectSchema o -> " {}";
            case JsonSchemaHelper.ArraySchema a -> " [" + (a.size() == null ? "" : a.size()) + "]";
            default -> "";
        };
        setText(ns.name() + typeHint);
    }
}
