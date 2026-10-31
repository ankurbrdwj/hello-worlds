package com.ankur.spark.service.common;

import org.apache.avro.generic.GenericData;

import java.io.IOException;

@FunctionalInterface
public interface ParquetRecordConsumer {
    void accept(GenericData.Record record) throws IOException;
}
