package com.ankur.spark.service.aggregation;

import com.ankur.spark.FunctionContext;
import com.ankur.spark.GcsStorageConfig;
import com.ankur.spark.HdfsConfiguration;
import com.ankur.spark.service.common.CalcTask;
import com.ankur.spark.service.common.CalculationLocation;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class AggregationContext implements FunctionContext {

    private AggregationRequest aggregationRequest;
    // TODO : HDFS and GCP are Mutually exclusive configuration - they need to be merged into a single overloaded property.
    private HdfsConfiguration hdfsConfig;
    private GcsStorageConfig gcsConfig;
    private CalcTask calcTask;
    private CalculationLocation calculationLocation;

    @Override
    public void cleanup() {
        // We do not have anything to clean-up in GcsStorageConfig
        if (hdfsConfig != null) {
            hdfsConfig.cleanup();
        }
    }
}
