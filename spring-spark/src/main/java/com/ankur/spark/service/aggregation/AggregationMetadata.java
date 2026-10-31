package com.ankur.spark.service.aggregation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Getter
@Builder
@ToString
public class AggregationMetadata {

    private final List<String> groupByColumns;
    private final Map<AggregationFunctionType, List<String>> functionColumns;
    private final List<String> allColumns;

    public Set<AggregationFunctionType> getFunctionTypes() {
        return functionColumns.keySet();
    }
}
