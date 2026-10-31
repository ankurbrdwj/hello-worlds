package com.ankur.spark.avro;

import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetReader;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.hadoop.util.HadoopOutputFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/** {@link ParquetRecordOperations} backed by {@link GenericData} and parquet-avro. */
public class GenericDataParquetOperations implements ParquetRecordOperations {

    private static final GenericData MODEL = GenericData.get();

    private final Configuration conf;

    public GenericDataParquetOperations() {
        this(new Configuration());
    }

    public GenericDataParquetOperations(Configuration conf) {
        this.conf = conf;
    }

    @Override
    public Schema readSchema(Path parquetFile) throws IOException {
        // Reading the first record is the simplest way to get the Avro schema parquet-avro derives.
        try (ParquetReader<GenericRecord> reader = reader(parquetFile)) {
            GenericRecord first = reader.read();
            if (first == null) {
                throw new IOException("Parquet file has no records, cannot derive schema: " + parquetFile);
            }
            return first.getSchema();
        }
    }

    @Override
    public List<GenericRecord> readAll(Path parquetFile) throws IOException {
        List<GenericRecord> out = new ArrayList<>();
        try (ParquetReader<GenericRecord> reader = reader(parquetFile)) {
            for (GenericRecord r = reader.read(); r != null; r = reader.read()) {
                // parquet-avro may reuse the record instance between reads
                out.add(MODEL.deepCopy(r.getSchema(), r));
            }
        }
        return out;
    }

    @Override
    public void writeAll(Path parquetFile, Schema schema, List<GenericRecord> records) throws IOException {
        try (ParquetWriter<GenericRecord> writer = AvroParquetWriter.<GenericRecord>builder(
                        HadoopOutputFile.fromPath(new org.apache.hadoop.fs.Path(parquetFile.toUri()), conf))
                .withSchema(schema)
                .withDataModel(MODEL)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withWriteMode(ParquetFileWriter.Mode.CREATE)
                .build()) {
            for (GenericRecord r : records) {
                writer.write(r);
            }
        }
    }

    @Override
    public List<GenericRecord> filter(List<GenericRecord> records, Predicate<GenericRecord> predicate) {
        return records.stream().filter(predicate).toList();
    }

    @Override
    public GenericRecord withField(GenericRecord record, String field, Object value) {
        if (record.getSchema().getField(field) == null) {
            throw new IllegalArgumentException("No field '" + field + "' in " + record.getSchema().getFullName());
        }
        GenericRecord copy = (GenericRecord) MODEL.deepCopy(record.getSchema(), record);
        copy.put(field, value);
        return copy;
    }

    @Override
    public List<GenericRecord> sortBy(List<GenericRecord> records, String field, boolean ascending) {
        return records.stream().sorted(ParquetRecordOperations.byField(field, ascending)).toList();
    }

    @Override
    public List<GenericRecord> findInvalid(Schema schema, List<GenericRecord> records) {
        return records.stream().filter(r -> !MODEL.validate(schema, r)).toList();
    }

    private ParquetReader<GenericRecord> reader(Path file) throws IOException {
        return AvroParquetReader.<GenericRecord>builder(
                        HadoopInputFile.fromPath(new org.apache.hadoop.fs.Path(file.toUri()), conf))
                .withDataModel(MODEL)
                .build();
    }
}
