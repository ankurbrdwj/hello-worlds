package com.ankur.spark.service.aggregation;

public class MaxFunction extends NumericAggregationFunction {

    public MaxFunction() {
        super(Math::max, Math::max);
    }
}
