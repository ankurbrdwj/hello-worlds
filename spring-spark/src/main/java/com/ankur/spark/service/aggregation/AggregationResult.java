package com.ankur.spark.service.aggregation;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Set;

@Getter
@Builder
@ToString
public class AggregationResult {

    private final Set<String> aggregatedParquetPaths;
    private final long totalBytes;
}
