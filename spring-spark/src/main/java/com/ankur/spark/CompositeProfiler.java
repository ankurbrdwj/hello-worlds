package com.ankur.spark;

import java.util.List;

/** Fans every call out to a list of profilers. */
public class CompositeProfiler implements Profiler {

    private final List<Profiler> delegates;

    public CompositeProfiler(List<Profiler> delegates) {
        this.delegates = delegates;
    }

    @Override
    public Long stop() {
        Long deltaTime = null;
        for (Profiler delegate : delegates) {
            Long delegateDelta = delegate.stop();
            if (deltaTime == null) {
                deltaTime = delegateDelta;
            }
        }
        return deltaTime;
    }

    @Override
    public Long stopAndReport(Level level, MonitorableEvent monitorableEvent) {
        Long deltaTime = stop();
        report(level, monitorableEvent, deltaTime);
        return deltaTime;
    }

    @Override
    public void report(Level level, MonitorableEvent monitorableEvent) {
        delegates.forEach(delegate -> delegate.report(level, monitorableEvent));
    }

    @Override
    public void report(Level level, MonitorableEvent monitorableEvent, Long deltaTime) {
        delegates.forEach(delegate -> delegate.report(level, monitorableEvent, deltaTime));
    }

    @Override
    public void log(Level level, String message) {
        delegates.forEach(delegate -> delegate.log(level, message));
    }

    @Override
    public boolean isEnabled(Level level) {
        return delegates.stream().anyMatch(delegate -> delegate.isEnabled(level));
    }
}
