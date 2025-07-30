package org.overb.jsontocsv.dto;

import org.overb.jsontocsv.libs.JsonSchemaHelper;

public record NamedSchema(String name, JsonSchemaHelper.Schema schema) {

    @Override
    public String toString() {
        return name;
    }
}
