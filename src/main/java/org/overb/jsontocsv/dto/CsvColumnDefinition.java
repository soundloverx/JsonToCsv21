package org.overb.jsontocsv.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.overb.jsontocsv.enums.ColumnTypes;

@NoArgsConstructor
public class CsvColumnDefinition {
    @Getter
    @Setter
    private String csvColumn;
    @Getter
    @Setter
    private String jsonColumn;
    @Getter
    private ColumnTypes type;
    @Getter
    private final BooleanProperty custom = new SimpleBooleanProperty();

    public CsvColumnDefinition(String csvColumn, String jsonColumn, ColumnTypes type) {
        this.csvColumn = csvColumn;
        this.jsonColumn = jsonColumn;
        setType(type);
    }

    public boolean isCustom() {
        return custom.get();
    }

    public void setCustom(boolean value) {
        custom.set(value);
    }

    public BooleanProperty customProperty() {
        return custom;
    }

    public void setType(ColumnTypes type) {
        this.type = type;
        this.setCustom(type.isCustom());
    }
}