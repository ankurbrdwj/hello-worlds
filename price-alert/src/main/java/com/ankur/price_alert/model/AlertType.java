package com.ankur.price_alert.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum that accepts both canonical names and common synonyms from client payloads.
 */
public enum AlertType {
    PRICE_BETWEEN,
    PRICE_BELOW,
    PRICE_EQUALS,
    PRICE_ABOVE;

    private static final Map<String, AlertType> LOOKUP =
            Stream.of(values())
                    .collect(Collectors.toMap(
                            v -> v.name().toUpperCase(),
                            v -> v
                    ));

    static {
        // synonyms
        LOOKUP.put("GREATER", PRICE_ABOVE);
        LOOKUP.put("GREATER_THAN", PRICE_ABOVE);
        LOOKUP.put("LESS", PRICE_BELOW);
        LOOKUP.put("LESS_THAN", PRICE_BELOW);
        LOOKUP.put("BETWEEN", PRICE_BETWEEN);
        LOOKUP.put("EQUAL", PRICE_EQUALS);
        LOOKUP.put("EQUALS", PRICE_EQUALS);
    }

    @JsonCreator
    public static AlertType fromString(String key) {
        if (key == null) {
            throw new IllegalArgumentException("AlertType value is null");
        }
        AlertType t = LOOKUP.get(key.trim().toUpperCase());
        if (t == null) {
            throw new IllegalArgumentException("Unknown AlertType: " + key);
        }
        return t;
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}