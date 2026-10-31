package com.ankur.spark.service.common;

import java.nio.file.Path;

public final class PathHelper {

    private PathHelper() {
    }

    /** Path as a string with forward slashes, regardless of the platform separator. */
    public static String toUnixPathString(Path path) {
        return path.toString().replace('\\', '/');
    }
}
