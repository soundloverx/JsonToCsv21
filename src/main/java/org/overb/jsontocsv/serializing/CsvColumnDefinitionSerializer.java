package org.overb.jsontocsv.serializing;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.overb.jsontocsv.dto.CsvColumnDefinition;

import java.io.IOException;

public class CsvColumnDefinitionSerializer extends JsonSerializer<CsvColumnDefinition> {

    @Override
    public void serialize(CsvColumnDefinition csvColumnDefinition, JsonGenerator jsonGenerator, SerializerProvider serializers) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField("csv", csvColumnDefinition.getColumnName());
        jsonGenerator.writeStringField("json", csvColumnDefinition.getJsonSource());
        jsonGenerator.writeStringField("type", csvColumnDefinition.getType() != null ? csvColumnDefinition.getType().name() : null);
        jsonGenerator.writeEndObject();
    }
}
