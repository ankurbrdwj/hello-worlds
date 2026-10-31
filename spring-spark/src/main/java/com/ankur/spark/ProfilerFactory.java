package com.ankur.spark;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import lombok.Getter;

public class ProfilerFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ProfilerFactory.class);
    private static Properties properties;

    @Getter
    private static ProfilerCreator profilerCreator;

    static {
        List<DestinationType> destinationTypes = getDestinationType();
        profilerCreator = getProfilerCreator(destinationTypes);
    }

    private static List<DestinationType> getDestinationType() {
        List<DestinationType> destinationTypes;
        try {
            destinationTypes = new ArrayList<>();
            for (String destinationType : PerformancePropertiesReader.getDestinationType().split(",")) {
                destinationTypes.add(DestinationType.valueOf(destinationType));
            }
            properties = PerformancePropertiesReader.getProperties();
        } catch (Exception e) {
            destinationTypes = new ArrayList<>();
            destinationTypes.add(DestinationType.LOG);
            LOG.warn("Failed to read a destination type, will use log by default: {}", e.getMessage());
        }
        return destinationTypes;
    }

    private static ProfilerCreator getProfilerCreator(List<DestinationType> destinationTypes) {
        if (destinationTypes.size() == 1 && destinationTypes.contains(DestinationType.EVENT_LIST)) {
            return new EventListProfilerCreator();
        } else {
            List<ProfilerCreator> creators = new ArrayList<>();
            for (DestinationType destinationType : destinationTypes) {
                switch (destinationType) {
                    case JMS_QUEUE:
                        creators.add(new JmsProfilerCreator(properties));
                        break;
                    case LOG:
                        creators.add(new LoggingProfilerCreator());
                        break;
                    default:
                        break;
                }
            }
            if (creators.isEmpty()) {
                creators.add(new LoggingProfilerCreator());
            }
            return new CompositeProfilerCreator(creators);
        }
    }

    public static Profiler create() {
        return profilerCreator.create();
    }

    public static void setProfilerCreator(ProfilerCreator profilerCreator) {
        ProfilerFactory.profilerCreator = profilerCreator;
    }

    public static Profiler createAndStart() {
        return profilerCreator.createAndStart();
    }

    private enum DestinationType {
        JMS_QUEUE, LOG, EVENT_LIST
    }
}
