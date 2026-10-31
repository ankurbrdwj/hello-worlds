package com.ankur.spark.service.common;

public final class DbaxVersionParser {

    private DbaxVersionParser() {
    }

    public static String format(DbaxVersionInfo versionInfo) {
        return versionInfo.toString();
    }
}
