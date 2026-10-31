package com.ankur.spark.service.aggregation;

import java.util.*;


import com.google.common.collect.ImmutableMap;
import com.ankur.spark.AggregationFunction;
import lombok.extern.slf4j.Slf4j;

import static com.ankur.spark.service.aggregation.AggregationFunctionType.MAX;
import static com.ankur.spark.service.aggregation.AggregationFunctionType.MIN;
import static com.ankur.spark.service.aggregation.AggregationFunctionType.SUM;

@Slf4j
public class AggregationFunctionProvider {

    private Map<AggregationFunctionType, AggregationFunction> aggFunctionMap = ImmutableMap.of(
            SUM, new SumFunction(),
            MAX, new MaxFunction(),
            MIN, new MinFunction()
    );

    public AggregationFunction getFunction(AggregationFunctionType functionType) {
        return aggFunctionMap.get(functionType);
    }

}
