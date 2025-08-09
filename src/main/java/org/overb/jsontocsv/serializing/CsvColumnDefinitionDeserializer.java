package org.overb.jsontocsv.serializing;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.overb.jsontocsv.dto.CsvColumnDefinition;
import org.overb.jsontocsv.enums.ColumnTypes;

import java.io.IOException;

public class CsvColumnDefinitionDeserializer extends JsonDeserializer<CsvColumnDefinition> {

    @Override
    public CsvColumnDefinition deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        if (jsonParser.currentToken() == null) jsonParser.nextToken();
        if (jsonParser.currentToken() != JsonToken.START_OBJECT) {
            return (CsvColumnDefinition) deserializationContext.handleUnexpectedToken(CsvColumnDefinition.class, jsonParser);
        }

        ObjectNode node = jsonParser.getCodec().readTree(jsonParser);
        String csvColumn = node.hasNonNull("csv") ? node.get("csv").asText() : null;
        String jsonColumn = node.hasNonNull("json") ? node.get("json").asText() : null;
        ColumnTypes type = null;
        if (node.hasNonNull("type")) {
            String t = node.get("type").asText().toUpperCase();
            try {
                type = ColumnTypes.valueOf(t);
            } catch (IllegalArgumentException ignored) {
            }
        }
        CsvColumnDefinition def = new CsvColumnDefinition(csvColumn, jsonColumn, type);
        def.setCustom(type != ColumnTypes.DEFAULT);
        return def;
    }
}

