package com.ankur.spark.service.aggregation;

import com.ankur.spark.service.common.InMemoryOutputFile;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.ParquetWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.io.IOException;

/** Writes Parquet using the schema of the first record. */
public class DefaultParquetWriterHelper implements ParquetWriterHelper {

    private final Configuration conf;
    private final Schema schema;

    public DefaultParquetWriterHelper(Configuration conf, Iterable<GenericData.Record> records) {
        this.conf = conf;
        this.schema = records.iterator().hasNext() ? records.iterator().next().getSchema() : null;
    }

    @Override
    public InMemoryOutputFile createOutputFile() {
        return new InMemoryOutputFile();
    }

    @Override
    public ParquetWriter<GenericData.Record> createParquetWriter(InMemoryOutputFile outputFile) throws IOException {
        return AvroParquetWriter.<GenericData.Record>builder(outputFile)
                .withSchema(schema)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withWriteMode(ParquetFileWriter.Mode.CREATE)
                .build();
    }
}
