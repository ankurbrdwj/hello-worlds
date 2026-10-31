package com.ankur.spark;

import java.util.*;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.conf.HadoopParquetConfiguration;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class GcsParquetReader implements ParquetReader {

    private static final String SEPARATOR = "/";

    private final ParquetStorage storage;

    @Override
    public List<GenericData.Record> readParquet(String bucketName, String fileName) throws IOException {
        byte[] gcsParquetBytes = storage.readBytes(normalize(Paths.get(bucketName, fileName).toString()));
        return readParquet(gcsParquetBytes);
    }

    @Override
    public List<GenericData.Record> readParquet(byte[] content) throws IOException {
        List<GenericData.Record> records = new ArrayList<>();
        try (org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader = getParquetReader(content)) {
            fetchRecords(reader, records);
        }
        return records;
    }

    private org.apache.parquet.hadoop.ParquetReader<GenericRecord> getParquetReader(byte[] gcsParquetBytes) throws IOException {
        ParquetStream parquetStream = new ParquetStream(gcsParquetBytes);
        return AvroParquetReader.genericRecordReader(parquetStream, new HadoopParquetConfiguration());
    }

    private void fetchRecords(org.apache.parquet.hadoop.ParquetReader<GenericRecord> reader, List<GenericData.Record> records) throws IOException {
        GenericData.Record genericRecord = (GenericData.Record) reader.read();
        while (genericRecord != null) {
            records.add(genericRecord);
            genericRecord = (GenericData.Record) reader.read();
        }
    }

    private String normalize(String path) {
        return path.replace("\\", SEPARATOR);
    }
}
