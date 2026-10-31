package com.ankur.spark.service.aggregation;

import org.apache.hadoop.conf.Configuration;

public final class AggregationConstants {

    /** Hadoop configuration used when writing aggregated Avro records as Parquet. */
    public static final Configuration AVRO_CONF = new Configuration(false);

    /** Name of the list element field written by the Parquet Avro writer; see CommonShadedParquetWriter. */
    public static final String LIST_FIELD_NAME = "parquet.avro.list.field.name";

    static {
        AVRO_CONF.setBoolean("parquet.avro.write-old-list-structure", false);
    }

    private AggregationConstants() {
    }
}
