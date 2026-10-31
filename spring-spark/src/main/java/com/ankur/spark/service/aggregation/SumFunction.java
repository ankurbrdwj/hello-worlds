package com.ankur.spark.service.aggregation;

public class SumFunction extends NumericAggregationFunction {

    public SumFunction() {
        super(Long::sum, Double::sum);
    }
}
