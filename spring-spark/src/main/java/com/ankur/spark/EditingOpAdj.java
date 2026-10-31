package com.ankur.spark;

import java.util.*;

import com.ankur.spark.avro.ParquetSchemaUtils;
import com.ankur.spark.exception.OpAdjException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecordBuilder;
import org.apache.avro.util.Utf8;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class EditingOpAdj implements OpAdj {

    public static final String OBJECT_TYPE_COLUMN = "objecttype";
    public static final String DICTIONARY = "DICTIONARY";
    public static final String VALUE_COLUMN = "value";
    public static final String TRADE_ID_COLUMN = "tradeid";

    private static final Set<Schema.Type> NUMERICAL_SCHEMA_SET =
            EnumSet.of(Schema.Type.INT, Schema.Type.LONG, Schema.Type.FLOAT, Schema.Type.DOUBLE);

    @Override
    public Stream<GenericData.Record> adjust(GenericData.Record sourceRecord) {
        Utf8 objectType = (Utf8) sourceRecord.get(OBJECT_TYPE_COLUMN);
        Object sourceValueColumn = sourceRecord.get(VALUE_COLUMN);

        if (sourceValueColumn == null) {
            throw new OpAdjException(String.format("Cannot edit record: column '%s' is null", VALUE_COLUMN));
        }

        final Schema valueColumnSchema = Optional.ofNullable(sourceRecord.getSchema())
                .map(schema -> schema.getField(VALUE_COLUMN))
                .map(Schema.Field::schema)
                .map(ParquetSchemaUtils::unwrapNullableType)
                .orElseThrow(() -> new OpAdjException(String.format("Cannot extract type of the column '%s'", VALUE_COLUMN)));

        if (!Schema.Type.ARRAY.equals(valueColumnSchema.getType())) {
            throw new OpAdjException(String.format("Expected column '%s' to be of ARRAY type, but got: %s", VALUE_COLUMN, valueColumnSchema.getType()));
        }

        Schema valueArrayItemSchema = Optional.ofNullable(valueColumnSchema.getElementType())
                .map(ParquetSchemaUtils::unwrapNullableType)
                .orElse(null);

        Schema.Type valueArrayItemType = Optional.ofNullable(valueArrayItemSchema)
                .map(Schema::getType)
                .orElseThrow(() -> new OpAdjException(String.format("Cannot extract element type of array column '%s'", VALUE_COLUMN)));

        @SuppressWarnings("unchecked")
        GenericData.Array<Object> sourceArray = (GenericData.Array<Object>) sourceValueColumn;
        Stream<GenericData.Array<Object>> targetValueArraysStream;

        if (NUMERICAL_SCHEMA_SET.contains(valueArrayItemType)) {
            log.info("Adjusting parquet with the RFCS schema");
            targetValueArraysStream = edit(sourceArray, objectType, n -> (Double) n, d -> d);
        } else if (Schema.Type.RECORD.equals(valueArrayItemType)) {
            log.info("Adjusting parquet with the RFOS schema");
            String valueColumnName = getDoubleFieldName(valueArrayItemSchema);
            targetValueArraysStream = edit(sourceArray, objectType,
                    r -> (Double) ((GenericData.Record) r).get(valueColumnName),
                    d -> {
                        GenericData.Record targetArrayItem = new GenericData.Record(valueColumnSchema.getElementType());
                        targetArrayItem.put(valueColumnName, d);
                        return targetArrayItem;
                    });
        } else {
            throw new OpAdjException(String.format("Incorrect element type of array column 'value', expected RECORD or one of [%s] but got: %s",
                    NUMERICAL_SCHEMA_SET, valueArrayItemType));
        }

        log.info("{} was successfully performed for tradeId {}", this.getClass().getSimpleName(), sourceRecord.get(TRADE_ID_COLUMN));

        return targetValueArraysStream.map(valueArray -> {
            GenericRecordBuilder targetRecordBuilder = new GenericRecordBuilder(sourceRecord);
            targetRecordBuilder.set(VALUE_COLUMN, valueArray);
            return targetRecordBuilder.build();
        });
    }

    protected String getDoubleFieldName(Schema recordSchema) {
        // Not recovered from the corrupted source; only reachable on the RFOS (RECORD-typed value array) path.
        throw new UnsupportedOperationException("getDoubleFieldName is not implemented - source not recovered");
    }

    protected abstract Stream<GenericData.Array<Object>> edit(GenericData.Array<Object> source, Utf8 objectType,
                                                                Function<Object, Double> valueSupplier, DoubleFunction<Object> valueConsumer) throws OpAdjException;
}
