package com.ankur.spark.service.copy;

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
public class CopyContext implements FunctionContext {

    private CopyRequest request;
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
