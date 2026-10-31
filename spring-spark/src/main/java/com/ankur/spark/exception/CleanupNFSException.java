package com.ankur.spark.exception;

public class CleanupNFSException extends RuntimeException {

    public CleanupNFSException(String message) {
        super(message);
    }

    public CleanupNFSException(String message, Throwable cause) {
        super(message, cause);
    }

    public CleanupNFSException(Throwable cause) {
        super(cause);
    }
}
