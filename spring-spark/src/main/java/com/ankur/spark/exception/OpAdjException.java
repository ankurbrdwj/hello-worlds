package com.ankur.spark.exception;

public class OpAdjException extends RuntimeException {

    public OpAdjException(String message) {
        super(message);
    }

    public OpAdjException(String message, Throwable cause) {
        super(message, cause);
    }

    public OpAdjException(Throwable cause) {
        super(cause);
    }
}
