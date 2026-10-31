package com.ankur.spark;

import java.util.List;

public class CompositeProfilerCreator implements ProfilerCreator {

    private final List<ProfilerCreator> creators;

    public CompositeProfilerCreator(List<ProfilerCreator> creators) {
        this.creators = creators;
    }

    @Override
    public Profiler create() {
        return new CompositeProfiler(creators.stream().map(ProfilerCreator::create).toList());
    }
}
