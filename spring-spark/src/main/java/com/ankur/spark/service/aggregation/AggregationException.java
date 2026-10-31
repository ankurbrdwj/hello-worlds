package com.ankur.spark.service.aggregation;

public class AggregationException extends RuntimeException {

    public AggregationException(String message) {
        super(message);
    }

    public AggregationException(String message, Throwable cause) {
        super(message, cause);
    }
}
