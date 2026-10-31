package com.ankur.spark.service.common;

import java.util.*;

import com.ankur.spark.ParquetReader;
import com.ankur.spark.ParquetStorage;
import com.ankur.spark.ParquetStream;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.conf.HadoopParquetConfiguration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.apache.parquet.io.InputFile;

@RequiredArgsConstructor
public class CommonGenericRecordReader implements ParquetReader {

    private final ParquetStorage parquetStorage;

    @Override
    public List<GenericData.Record> readParquet(String basePath, String fileName) throws IOException {
        String fullPath = Paths.get(basePath, fileName).toString();
        byte[] content = parquetStorage.readBytes(fullPath);
        return readParquet(content);
    }

    public void streamParquet(String basePath, String fileName, ParquetRecordConsumer consumer) throws IOException {
        String fullPath = Paths.get(basePath, fileName).toString();

        try (org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader = getParquetReader(parquetStorage.getInputFile(fullPath))) {

            GenericRecord record;

            while ((record = reader.read()) != null) {

                consumer.accept((GenericData.Record) record);
            }
        }
    }


    @Override
    public List<GenericData.Record> readParquet(byte[] content) throws IOException {
        List<GenericData.Record> records = new ArrayList<>();
        ParquetStream parquetStream = new ParquetStream(content);

        try (org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader =
                     AvroParquetReader.genericRecordReader(parquetStream, new HadoopParquetConfiguration())) {
            GenericData.Record genericDataRecord = (GenericData.Record) reader.read();
            while (genericDataRecord != null) {
                records.add(genericDataRecord);
                genericDataRecord = (GenericData.Record) reader.read();
            }
        }

        return records;
    }

    public org.apache.parquet.hadoop.ParquetReader<GenericRecord> getParquetReader(InputFile inputFile) throws IOException {
        return AvroParquetReader.genericRecordReader(inputFile, new HadoopParquetConfiguration());
    }
}
