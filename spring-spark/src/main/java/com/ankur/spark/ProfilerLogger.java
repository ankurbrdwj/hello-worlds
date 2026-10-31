package com.ankur.spark;

import java.util.*;

public interface ProfilerLogger {

    void log(Profiler.Level level, String message);

    boolean isEnabled(Profiler.Level level);
}
