package com.ankur.spark.avro;

import com.ankur.spark.ParquetStream;
import com.ankur.spark.exception.OpAdjException;
import org.apache.parquet.hadoop.ParquetFileReader;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.schema.MessageType;

import java.io.IOException;

public final class RolloverAvroParquetUtils {

    private RolloverAvroParquetUtils() {
    }

    /** The Parquet schema from the file footer; works even when the file holds no records. */
    public static MessageType parquetSchema(ParquetStream parquetStream) {
        try (ParquetFileReader reader = ParquetFileReader.open(parquetStream)) {
            return reader.getFileMetaData().getSchema();
        } catch (IOException e) {
            throw new OpAdjException("Cannot read the parquet schema", e);
        }
    }


    public static MessageType parquetSchema(InputFile inputFile) {
        try (ParquetFileReader reader = ParquetFileReader.open(inputFile)) {
            return reader.getFileMetaData().getSchema();
        } catch (IOException e) {
            throw new OpAdjException("Cannot read the parquet schema", e);
        }
    }
}
