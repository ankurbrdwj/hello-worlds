package com.ankur.spark.service.aggregation;

import java.util.*;

import com.ankur.spark.service.common.CalculationLocation;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AggregationRequest {
    private Map<String, String> grainPaths;
    private String vcsExecutionId;
    private String grain;
    private String outSurface;
    private String inSurface;
    private RetryTemplate processRetryTemplate;
    private Set<String> tablesToLand;
    private Map<String, String> aggregationCriteria;

    private CalculationLocation calculationLocation;
    private HdfsRequest hdfsRequest;
    private GcsRequest gcsRequest;

    @Getter
    @Builder
    public static class HdfsRequest {
        private RetryTemplate ioRetryTemplate;
        private RetryTemplate fsRetryTemplate;
    }

    @Getter
    @Builder
    public static class GcsRequest {
        private String bucketName;
    }
}


