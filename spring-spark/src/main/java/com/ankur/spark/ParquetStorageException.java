package com.ankur.spark;

import java.util.*;

public class ParquetStorageException extends RuntimeException {

    public ParquetStorageException(String message) {
        super(message);
    }

    public ParquetStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public ParquetStorageException(Throwable cause) {
        super(cause);
    }
}
