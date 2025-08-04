package org.overb.jsontocsv.dto;

import java.io.Serial;
import java.io.Serializable;

public record JsonDragNode(String node, String schemaClass, String fullPath) implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    public static JsonDragNode of(String node, String schemaClass, String fullPath) {
        return new JsonDragNode(node, schemaClass, fullPath);
    }
}
