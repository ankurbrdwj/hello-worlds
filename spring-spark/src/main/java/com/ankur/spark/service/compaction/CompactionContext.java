package com.ankur.spark.service.compaction;

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
public class CompactionContext implements FunctionContext {

    private CompactionRequest compactionRequest;
    private HdfsConfiguration hdfsConfig;
    private GcsStorageConfig gcsConfig;
    private CalcTask calcTask;
    private CalculationLocation calculationLocation;

    @Override
    public void cleanup() {
        if (hdfsConfig != null) {
            hdfsConfig.cleanup();
        }
    }
}
