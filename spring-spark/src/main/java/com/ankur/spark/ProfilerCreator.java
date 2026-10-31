package com.ankur.spark;

public interface ProfilerCreator {

    /** The returned profiler measures time from the moment it is created. */
    Profiler create();

    default Profiler createAndStart() {
        return create();
    }
}
