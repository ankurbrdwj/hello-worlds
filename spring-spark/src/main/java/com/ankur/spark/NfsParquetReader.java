package com.ankur.spark;

import java.util.*;

import static com.ankur.spark.service.aggregation.AggregationConstants.AVRO_CONF;

import org.apache.avro.generic.GenericData;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class NfsParquetReader implements ParquetReader {

    @Override
    public List<GenericData.Record> readParquet(String basePath, String fileName) throws IOException {
        return readParquet(getParquetReader(basePath, fileName));
    }

    @Override
    public List<GenericData.Record> readParquet(byte[] content) throws IOException {
        return readParquet(getParquetReader(content));
    }

    private List<GenericData.Record> readParquet(org.apache.parquet.hadoop.ParquetReader<GenericData.Record> parquetReader) throws IOException {
        List<GenericData.Record> records = new ArrayList<>();

        try (org.apache.parquet.hadoop.ParquetReader<GenericData.Record> incomingParquetReader = parquetReader) {
            GenericData.Record genericDataRecord = incomingParquetReader.read();
            while (genericDataRecord != null) {
                records.add(genericDataRecord);
                genericDataRecord = incomingParquetReader.read();
            }
        }

        return records;
    }

    private org.apache.parquet.hadoop.ParquetReader<GenericData.Record> getParquetReader(String basePath, String fileName) throws IOException {
        Path incomingParquetPath = new Path(normalize(basePath, fileName));
        InputFile incomingParquetFile = HadoopInputFile.fromPath(incomingParquetPath, AVRO_CONF);
        return AvroParquetReader.<GenericData.Record>builder(incomingParquetFile).build();
    }

    private org.apache.parquet.hadoop.ParquetReader<GenericData.Record> getParquetReader(byte[] content) throws IOException {
        return AvroParquetReader.<GenericData.Record>builder(new ParquetStream(content))
                .withConf(AVRO_CONF)
                .build();
    }

    private String normalize(String basePath, String fileName) {
        return Paths.get(basePath).resolve(fileName).toString().replace("\\", "/");
    }
}
