package org.overb.jsontocsv.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.overb.jsontocsv.enums.ColumnTypes;
import org.overb.jsontocsv.serializing.CsvColumnDefinitionDeserializer;
import org.overb.jsontocsv.serializing.CsvColumnDefinitionSerializer;

import java.util.Objects;

@NoArgsConstructor
@JsonSerialize(using = CsvColumnDefinitionSerializer.class)
@JsonDeserialize(using = CsvColumnDefinitionDeserializer.class)
public class CsvColumnDefinition {
    @Getter
    @Setter
    private String columnName;
    @Getter
    @Setter
    private String jsonSource;
    @Getter
    private ColumnTypes type;
    @Getter
    private final BooleanProperty custom = new SimpleBooleanProperty();

    public CsvColumnDefinition(String ColumnName, String jsonSource, ColumnTypes type) {
        this.columnName = ColumnName;
        this.jsonSource = jsonSource;
        setType(type);
    }

    public CsvColumnDefinition(CsvColumnDefinition copy) {
        this.columnName = copy.columnName;
        this.jsonSource = copy.jsonSource;
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
        return Objects.equals(columnName, that.columnName) && Objects.equals(jsonSource, that.jsonSource) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(columnName, jsonSource, type);
    }
}