package com.ankur.spark.service.aggregation;

import com.ankur.spark.service.common.InMemoryOutputFile;
import org.apache.avro.generic.GenericData;
import org.apache.parquet.hadoop.ParquetWriter;

import java.io.IOException;

public interface ParquetWriterHelper {

    InMemoryOutputFile createOutputFile();

    ParquetWriter<GenericData.Record> createParquetWriter(InMemoryOutputFile outputFile) throws IOException;
}
