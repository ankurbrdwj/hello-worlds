package com.ankur.spark;

import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.IOException;
import java.util.List;

public interface ParquetWriter extends AutoCloseable {

    void writeParquet(List<GenericData.Record> records) throws IOException;

    default void writeParquet(List<GenericData.Record> records, Schema schema) throws IOException {
        writeParquet(records);
    }

    long getDataSize();

    default String normalize(String path) {
        return path.replace("\\", "/");
    }
}
