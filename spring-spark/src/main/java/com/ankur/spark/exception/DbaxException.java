package com.ankur.spark.exception;

public class DbaxException extends Exception {

    public DbaxException(String message) {
        super(message);
    }

    public DbaxException(String message, Throwable cause) {
        super(message, cause);
    }

    public DbaxException(Throwable cause) {
        super(cause);
    }
}
