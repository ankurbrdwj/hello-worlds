package com.ankur.spark;

import java.util.Properties;

/** Reads profiler settings from system properties: {@code profiler.destination=LOG,JMS_QUEUE,EVENT_LIST}. */
public final class PerformancePropertiesReader {

    private PerformancePropertiesReader() {
    }

    public static String getDestinationType() {
        return System.getProperty("profiler.destination", "LOG");
    }

    public static Properties getProperties() {
        Properties properties = new Properties();
        System.getProperties().stringPropertyNames().stream()
                .filter(name -> name.startsWith("profiler."))
                .forEach(name -> properties.setProperty(name, System.getProperty(name)));
        return properties;
    }
}
