package org.overb.jsontocsv.enums;

import lombok.Getter;

public enum ColumnTypes {
    DEFAULT(false),
    LITERAL(true),
    FORMULA(true);

    @Getter
    private final boolean custom;

    ColumnTypes(final boolean custom) {
        this.custom = custom;
    }
}
