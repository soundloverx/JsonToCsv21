package org.overb.jsontocsv.serializing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.overb.jsontocsv.dto.CsvColumnDefinition;

import java.io.IOException;

public class CsvColumnDefinitionSerializer extends JsonSerializer<CsvColumnDefinition> {

    @Override
    public void serialize(CsvColumnDefinition value, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("csv", value.getColumnName());
        jsonGenerator.writeStringField("json", value.getJsonSource());
        jsonGenerator.writeStringField("type", value.getType() != null ? value.getType().name() : null);
        jsonGenerator.writeEndObject();
    }
}
