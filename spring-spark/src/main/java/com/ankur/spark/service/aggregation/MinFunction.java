package com.ankur.spark.service.aggregation;

public class MinFunction extends NumericAggregationFunction {

    public MinFunction() {
        super(Math::min, Math::min);
    }
}
