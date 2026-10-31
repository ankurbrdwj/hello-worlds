package com.ankur.spark.service.aggregation;

import com.ankur.spark.AggregationFunction;
import org.apache.avro.generic.GenericData;

import java.util.List;
import java.util.function.DoubleBinaryOperator;
import java.util.function.LongBinaryOperator;

/** Applies a binary numeric operation column by column, writing the result into the base record. */
abstract class NumericAggregationFunction implements AggregationFunction {

    private final LongBinaryOperator longOp;
    private final DoubleBinaryOperator doubleOp;

    NumericAggregationFunction(LongBinaryOperator longOp, DoubleBinaryOperator doubleOp) {
        this.longOp = longOp;
        this.doubleOp = doubleOp;
    }

    @Override
    public GenericData.Record apply(List<String> columns, GenericData.Record baseRecord, GenericData.Record patchRecord) {
        for (String column : columns) {
            baseRecord.put(column, combine(baseRecord.get(column), patchRecord.get(column)));
        }
        return baseRecord;
    }

    private Object combine(Object base, Object patch) {
        if (base == null) {
            return patch;
        }
        if (patch == null) {
            return base;
        }
        if (isIntegral(base) && isIntegral(patch)) {
            long result = longOp.applyAsLong(((Number) base).longValue(), ((Number) patch).longValue());
            return base instanceof Integer && patch instanceof Integer ? (Object) (int) result : (Object) result;
        }
        return doubleOp.applyAsDouble(((Number) base).doubleValue(), ((Number) patch).doubleValue());
    }

    private static boolean isIntegral(Object value) {
        return value instanceof Integer || value instanceof Long;
    }
}
