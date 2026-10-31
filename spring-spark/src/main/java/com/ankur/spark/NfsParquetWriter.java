package com.ankur.spark;

import java.util.*;

import static com.ankur.spark.service.aggregation.AggregationConstants.AVRO_CONF;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;
import org.apache.parquet.hadoop.util.HadoopOutputFile;
import org.apache.parquet.io.OutputFile;

import java.io.IOException;
import java.util.List;

import javax.annotation.concurrent.NotThreadSafe;
import lombok.RequiredArgsConstructor;

@NotThreadSafe
@RequiredArgsConstructor
public class NfsParquetWriter implements ParquetWriter {

    private final String fileName;

    private org.apache.parquet.hadoop.ParquetWriter<GenericData.Record> writer;

    @Override
    public void writeParquet(List<GenericData.Record> records) throws IOException {
        if (CollectionUtils.isEmpty(records)) {
            return;
        }

        initWriter(records.get(0).getSchema());
        for (GenericData.Record genericRecord : records) {
            writer.write(genericRecord);
        }
    }

    private void initWriter(Schema schema) throws IOException {
        if (writer == null) {
            OutputFile outputFile = HadoopOutputFile.fromPath(new org.apache.hadoop.fs.Path(normalize(fileName)), AVRO_CONF);
            this.writer = AvroParquetWriter.<GenericData.Record>builder(outputFile)
                    .withSchema(schema)
                    .withCompressionCodec(CompressionCodecName.SNAPPY)
                    .withConf(AVRO_CONF)
                    // Overwrite to allow for grid task resubmission and avoid org.apache.hadoop.fs.FileAlreadyExistsException
                    .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)
                    .build();
        }
    }

    @Override
    public long getDataSize() {
        if (writer != null) {
            return writer.getDataSize();
        } else {
            return 0L;
        }
    }

    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
