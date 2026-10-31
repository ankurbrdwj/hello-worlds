package com.ankur.spark.service.aggregation;

import java.util.HashMap;
import java.util.Map;

public final class AggregationCriteriaResolver {

    private AggregationCriteriaResolver() {
    }

    /**
     * Table key to criteria. {@code aggregationCriteria} is a {@code |}-separated list of
     * {@code tableKey=criteria} entries; {@code functionProperties} entries are applied on top and win on conflict.
     */
    public static Map<String, String> buildAggregationCriteriaMap(String aggregationCriteria, Map<String, String> functionProperties) {
        Map<String, String> criteriaMap = new HashMap<>();
        if (aggregationCriteria != null && !aggregationCriteria.isBlank()) {
            for (String entry : aggregationCriteria.split("\\|")) {
                String[] tableAndCriteria = entry.split("=", 2);
                if (tableAndCriteria.length != 2) {
                    throw new AggregationException("Invalid aggregation criteria entry [" + entry + "]");
                }
                criteriaMap.put(tableAndCriteria[0].trim().toLowerCase(), tableAndCriteria[1].trim());
            }
        }
        if (functionProperties != null) {
            functionProperties.forEach((tableKey, criteria) -> criteriaMap.put(tableKey.toLowerCase(), criteria));
        }
        return criteriaMap;
    }
}
