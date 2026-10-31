package com.ankur.spark;

import com.ankur.spark.service.common.CalcTask;

public final class ProfilerHelper {

    private ProfilerHelper() {
    }

    public static void stopAndReport(Profiler profiler, EnumCalcEventType eventType, CalcTask calcTask) {
        profiler.stopAndReport(Profiler.Level.INFO, new FunctionCompletedEvent(calcTask, eventType));
    }
}
