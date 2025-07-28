package org.overb.jsontocsv.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.overb.jsontocsv.enums.ColumnTypes;

import java.util.Objects;

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

    public CsvColumnDefinition(CsvColumnDefinition copy) {
        this.csvColumn = copy.csvColumn;
        this.jsonColumn = copy.jsonColumn;
        setType(copy.type);
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

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CsvColumnDefinition that)) return false;
        return Objects.equals(csvColumn, that.csvColumn) && Objects.equals(jsonColumn, that.jsonColumn) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(csvColumn, jsonColumn, type);
    }
}