package com.ankur.spark.service.aggregation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link AggregationMetadata} from a criteria string of {@code ;}-separated parts, each
 * {@code KEY:col1,col2}, where KEY is {@code GROUP_BY} or an {@link AggregationFunctionType}.
 * Example: {@code GROUP_BY:book,ccy;SUM:amount;MAX:rate}.
 */
public class AggregationMetadataFactory {

    static final String GROUP_BY = "GROUP_BY";

    public AggregationMetadata create(String criteria) {
        List<String> groupByColumns = new ArrayList<>();
        Map<AggregationFunctionType, List<String>> functionColumns = new EnumMap<>(AggregationFunctionType.class);

        for (String part : criteria.split(";")) {
            if (part.isBlank()) {
                continue;
            }
            String[] keyAndColumns = part.split(":", 2);
            if (keyAndColumns.length != 2) {
                throw new AggregationException("Invalid aggregation criteria part [" + part + "]");
            }
            String key = keyAndColumns[0].trim().toUpperCase();
            List<String> columns = Arrays.stream(keyAndColumns[1].split(",")).map(String::trim).filter(c -> !c.isEmpty()).toList();
            if (GROUP_BY.equals(key)) {
                groupByColumns.addAll(columns);
            } else {
                try {
                    functionColumns.computeIfAbsent(AggregationFunctionType.valueOf(key), k -> new ArrayList<>()).addAll(columns);
                } catch (IllegalArgumentException e) {
                    throw new AggregationException("Unknown aggregation function [" + key + "]", e);
                }
            }
        }

        List<String> allColumns = new ArrayList<>(groupByColumns);
        functionColumns.values().forEach(allColumns::addAll);

        return AggregationMetadata.builder()
                .groupByColumns(groupByColumns)
                .functionColumns(functionColumns)
                .allColumns(allColumns)
                .build();
    }
}
