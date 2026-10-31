package com.ankur.spark.service.common;

import java.util.regex.Pattern;

public final class FunctionConstants {

    public static final String AGGREGATION_FILE_NAME_PREFIX = "aggregation_";
    public static final String PARQUET_FILE_NAME_EXT = ".parquet";

    public static final String VCS_EXECUTION_ID_PREFIX = "vcs_execution_id=";
    // The digits following the prefix, e.g. ".../vcs_execution_id=123/..." -> "123"
    public static final Pattern VCS_EXECUTION_ID_PATTERN = Pattern.compile("(?<=" + VCS_EXECUTION_ID_PREFIX + ")\\d+");

    // The table key path segment, e.g. ".../tablekey=TRADE/..." -> "TRADE"
    public static final Pattern TABLEKEY_PATTERN = Pattern.compile("(?<=tablekey=)[^/\\\\]+");

    private FunctionConstants() {
    }
}
