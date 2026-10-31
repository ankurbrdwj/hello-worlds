package com.ankur.spark;

public class LoggingProfilerCreator implements ProfilerCreator {

    @Override
    public Profiler create() {
        return new LoggingProfiler();
    }
}
