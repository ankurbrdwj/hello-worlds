package com.ankur.spark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingProfiler implements Profiler {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingProfiler.class);

    private final long startNanos = System.nanoTime();

    @Override
    public Long stop() {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    @Override
    public Long stopAndReport(Level level, MonitorableEvent monitorableEvent) {
        Long deltaTime = stop();
        report(level, monitorableEvent, deltaTime);
        return deltaTime;
    }

    @Override
    public void report(Level level, MonitorableEvent monitorableEvent) {
        report(level, monitorableEvent, stop());
    }

    @Override
    public void report(Level level, MonitorableEvent monitorableEvent, Long deltaTime) {
        log(level, String.format("event=%s deltaMs=%d", monitorableEvent.getEventType(), deltaTime));
    }

    @Override
    public void log(Level level, String message) {
        switch (level) {
            case ERROR -> LOG.error(message);
            case WARN -> LOG.warn(message);
            case INFO -> LOG.info(message);
            case DEBUG -> LOG.debug(message);
            case TRACE -> LOG.trace(message);
        }
    }

    @Override
    public boolean isEnabled(Level level) {
        return switch (level) {
            case ERROR -> LOG.isErrorEnabled();
            case WARN -> LOG.isWarnEnabled();
            case INFO -> LOG.isInfoEnabled();
            case DEBUG -> LOG.isDebugEnabled();
            case TRACE -> LOG.isTraceEnabled();
        };
    }
}
