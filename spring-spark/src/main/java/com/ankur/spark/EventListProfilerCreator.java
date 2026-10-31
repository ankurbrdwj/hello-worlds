package com.ankur.spark;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Collects reported events in memory instead of logging them; handy for asserting on them in tests. */
public class EventListProfilerCreator implements ProfilerCreator {

    private final List<MonitorableEvent> events = new CopyOnWriteArrayList<>();

    public List<MonitorableEvent> getEvents() {
        return events;
    }

    @Override
    public Profiler create() {
        return new LoggingProfiler() {
            @Override
            public void report(Level level, MonitorableEvent monitorableEvent, Long deltaTime) {
                events.add(monitorableEvent);
            }
        };
    }
}
