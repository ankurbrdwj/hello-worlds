package com.ankur.spark.avro;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

public final class ParquetSchemaUtils {

    private ParquetSchemaUtils() {
    }

    /** For a nullable union ["null", T] returns T, otherwise the schema itself. */
    public static Schema unwrapNullableType(Schema schema) {
        if (schema.getType() != Schema.Type.UNION) {
            return schema;
        }
        return schema.getTypes().stream()
                .filter(type -> type.getType() != Schema.Type.NULL)
                .findFirst()
                .orElse(schema);
    }

    /** The value of {@code fieldName}, or {@code null} when the record's schema has no such field. */
    public static Object getFieldOrNull(GenericData.Record record, String fieldName) {
        return record.getSchema().getField(fieldName) == null ? null : record.get(fieldName);
    }
}
