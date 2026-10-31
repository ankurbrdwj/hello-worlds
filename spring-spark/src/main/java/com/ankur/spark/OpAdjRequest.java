package com.ankur.spark;

import java.util.*;

import com.ankur.spark.service.common.CalcTask;
import com.ankur.spark.service.common.CalculationLocation;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public abstract class OpAdjRequest {

    private CalculationLocation calculationLocation;
    private String sourcePath;
    private String targetPath;
    private long vcsExecutionId;
    private CalcTask calcTask;
    private OpAdjPredicate recordFilter;
}
