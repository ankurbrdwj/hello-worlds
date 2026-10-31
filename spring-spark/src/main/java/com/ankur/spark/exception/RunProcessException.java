package com.ankur.spark.exception;

public class RunProcessException extends Exception {

    public RunProcessException(String message) {
        super(message);
    }

    public RunProcessException(String message, Throwable cause) {
        super(message, cause);
    }

    public RunProcessException(Throwable cause) {
        super(cause);
    }
}
