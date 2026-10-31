package com.ankur.spark.service.aggregation;

import org.apache.avro.generic.GenericData;

import java.io.Closeable;
import java.io.IOException;

public interface GenericDataReader extends Closeable {

    String getName();

    /** The next record, or {@code null} when exhausted. */
    GenericData.Record read() throws IOException;
}
