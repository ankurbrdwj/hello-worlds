package com.ankur.orderservice.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Factory for creating configured ObjectMapper instances.
 */
public final class ObjectMapperFactory {
    
    private ObjectMapperFactory() {
        // Utility class
    }
    
    /**
     * Creates a new ObjectMapper with standard configuration.
     * 
     * @return configured ObjectMapper instance
     */
    public static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, false);
        return mapper;
    }
}
