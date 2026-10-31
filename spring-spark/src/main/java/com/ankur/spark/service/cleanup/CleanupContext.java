package com.ankur.spark.service.cleanup;

import com.ankur.spark.FunctionContext;
import com.ankur.spark.service.common.CalcTask;
import com.ankur.spark.service.common.CalculationLocation;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class CleanupContext implements FunctionContext {

    private CleanupRequest cleanupRequest;
    private CalcTask calcTask;
    private CalculationLocation calculationLocation;

    @Override
    public void cleanup() {
        // nothing to clean up
    }
}
