package org.overb.jsontocsv.libs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Helper storing the ObjectSchema, ArraySchema and PrimitiveSchema classes used by JsonService to build the schema of a loaded json
 */
public class JsonSchemaHelper {

    public sealed interface Schema permits ObjectSchema, ArraySchema, PrimitiveSchema {
        Schema merge(Schema other);

        JsonNode instantiate();
    }

    public static final class ObjectSchema implements Schema {
        public final Map<String, Schema> fields = new LinkedHashMap<>();

        @Override
        public Schema merge(Schema other) {
            if (!(other instanceof ObjectSchema otherSchema)) return this; // the data types don't matter

            for (Map.Entry<String, Schema> e : otherSchema.fields.entrySet()) {
                fields.merge(e.getKey(), e.getValue(), Schema::merge);
            }
            return this;
        }

        @Override
        public JsonNode instantiate() {
            ObjectNode node = JsonIo.MAPPER.createObjectNode();
            for (Map.Entry<String, Schema> entry : fields.entrySet()) {
                node.set(entry.getKey(), entry.getValue().instantiate());
            }
            return node;
        }
    }

    public static final class ArraySchema implements Schema {
        public Schema elementSchema;

        private Integer itemsCount;

        public ArraySchema(Schema first, Integer itemsCount) {
            this.elementSchema = first;
            this.itemsCount = itemsCount;
        }

        public Integer size() {
            return itemsCount;
        }

        public void setSize(Integer itemsCount) {
            this.itemsCount = itemsCount;
        }

        @Override
        public Schema merge(Schema other) {
            if (other instanceof ArraySchema otherSchema) {
                this.elementSchema = this.elementSchema.merge(otherSchema.elementSchema);
                if (this.itemsCount != null && otherSchema.itemsCount != null) {
                    if (!this.itemsCount.equals(otherSchema.itemsCount)) {
                        this.itemsCount = null;
                    }
                } else if (this.itemsCount == null) {
                    this.itemsCount = otherSchema.itemsCount;
                }
            }
            return this;
        }

        @Override
        public JsonNode instantiate() {
            ArrayNode array = JsonIo.MAPPER.createArrayNode();
            int count = (itemsCount != null && itemsCount > 0) ? itemsCount : 1;
            for (int i = 0; i < count; i++) {
                array.add(elementSchema.instantiate());
            }
            return array;
        }
    }


    public static final class PrimitiveSchema implements Schema {
        @Override
        public Schema merge(Schema other) {
            return this;
        }

        @Override
        public JsonNode instantiate() {
            return NullNode.instance;
        }
    }
}
