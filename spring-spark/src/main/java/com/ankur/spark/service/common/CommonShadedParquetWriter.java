package com.ankur.spark.service.common;

import java.util.*;

import com.ankur.spark.ParquetStorage;
import com.ankur.spark.ParquetWriter;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.parquet.avro.AvroParquetWriter;
import org.apache.parquet.hadoop.ParquetFileWriter;
import org.apache.parquet.hadoop.metadata.CompressionCodecName;

import java.io.IOException;
import java.util.List;

import lombok.RequiredArgsConstructor;

import static com.ankur.spark.service.aggregation.AggregationConstants.AVRO_CONF;
import static com.ankur.spark.service.aggregation.AggregationConstants.LIST_FIELD_NAME;

@RequiredArgsConstructor
public class CommonShadedParquetWriter implements ParquetWriter {

    private final String fullPath;
    private final String listFieldName;
    private final ParquetStorage storage;

    private InMemoryOutputFile outputFile;
    private org.apache.parquet.hadoop.ParquetWriter<GenericData.Record> writer;

    @Override
    public void writeParquet(List<GenericData.Record> records) throws IOException {
        writeParquet(records, records.get(0).getSchema());
    }

    @Override
    public void writeParquet(List<GenericData.Record> records, Schema schema) throws IOException {
        this.outputFile = new InMemoryOutputFile();
        Configuration conf = new Configuration(AVRO_CONF);

        if (!StringUtils.isEmpty(this.listFieldName)) {
            conf.set(LIST_FIELD_NAME, listFieldName);
        }

        this.writer = AvroParquetWriter.<GenericData.Record>builder(outputFile)
                .withSchema(schema)
                .withConf(conf)
                .withCompressionCodec(CompressionCodecName.SNAPPY)
                .withWriteMode(ParquetFileWriter.Mode.CREATE)
                .build();

        for (GenericData.Record genericRecord : records) {
            writer.write(genericRecord);
        }
    }

    @Override
    public long getDataSize() {
        return outputFile.size();
    }

    @Override
    public void close() throws Exception {
        if (writer != null) {
            writer.close();
            storage.writeBytes(normalize(fullPath), outputFile.toArray());
        }
    }

    public void writeRecord(GenericData.Record record, Schema schema) {
        initWriter(schema);
        writer.write(record);
    }

    private void initWriter(Schema schema) {
        if(writer!=null){
            return;
        }
        this.outputFile = new InMemoryOutputFile();
        Configuration conf= new Configuration(AVRO_CONF);
        if(!StringUtils.isEmpty(this.listFieldName)){
            conf.set(LIST_FIELD_NAME,listFieldName);
        }
        this.writer= ShadedAvroParquetWriter.

    }
}
