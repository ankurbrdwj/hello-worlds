package com.ankur.spark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/** There is no JMS client on the classpath, so this falls back to logging. */
public class JmsProfilerCreator implements ProfilerCreator {

    private static final Logger LOG = LoggerFactory.getLogger(JmsProfilerCreator.class);

    public JmsProfilerCreator(Properties properties) {
        LOG.warn("JMS profiler destination is not wired up, profiler events are logged instead");
    }

    @Override
    public Profiler create() {
        return new LoggingProfiler();
    }
}
