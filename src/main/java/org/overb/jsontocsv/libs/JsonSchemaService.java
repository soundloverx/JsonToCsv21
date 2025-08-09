package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.Iterator;

public final class JsonSchemaService {

    public static boolean isShallow(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return false;
        }
        if (!node.isArray()) {
            return false;
        }
        ArrayNode array = (ArrayNode) node;
        if (array.isEmpty()) {
            return true;
        }
        for (JsonNode el : array) {
            if (!el.isObject() || !isFlatObject(el)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isFlatObject(JsonNode obj) {
        Iterator<String> fields = obj.fieldNames();
        while (fields.hasNext()) {
            String name = fields.next();
            JsonNode value = obj.get(name);
            if (value == null || value.isMissingNode()) continue;
            if (value.isObject()) return false;
            if (value.isArray()) {
                if (!isArrayOfPrimitives(value)) return false;
            } else if (!value.isValueNode()) {
                return false;
            }
        }
        return true;
    }

    private static boolean isArrayOfPrimitives(JsonNode array) {
        if (!array.isArray()) return false;
        for (JsonNode el : array) {
            if (!el.isValueNode()) return false;
        }
        return true;
    }

    public static JsonSchemaHelper.Schema buildJsonSchema(JsonNode node) {
        if (node.isObject()) {
            JsonSchemaHelper.ObjectSchema objectSchema = new JsonSchemaHelper.ObjectSchema();
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String key = names.next();
                JsonNode child = node.get(key);
                objectSchema.fields.put(key, buildJsonSchema(child));
            }
            return objectSchema;
        }
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            JsonSchemaHelper.Schema merged = null;
            for (JsonNode element : array) {
                JsonSchemaHelper.Schema child = buildJsonSchema(element);
                merged = merged == null ? child : merged.merge(child);
            }
            JsonSchemaHelper.Schema elementSchema = (merged != null) ? merged : new JsonSchemaHelper.PrimitiveSchema();
            return new JsonSchemaHelper.ArraySchema(elementSchema, array.size());
        }
        return new JsonSchemaHelper.PrimitiveSchema();
    }
}