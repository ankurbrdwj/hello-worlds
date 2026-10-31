package com.ankur.spark.service.common;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathUtils {

    private PathUtils() {
    }

    /** First match of {@code pattern} in {@code path}, or {@code null} when it does not match. */
    public static String getKeyFromPath(String path, Pattern pattern) {
        Matcher matcher = pattern.matcher(path);
        return matcher.find() ? matcher.group() : null;
    }
}
