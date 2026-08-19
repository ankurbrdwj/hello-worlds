package com.ankur.spark.basic;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

public class ReadParquet {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: ReadParquet <parquet-dir>");
            System.exit(1);
        }

        String parquetDir = args[0];

        SparkSession spark = SparkSession.builder()
                .appName("ReadParquet")
                .master("local")
                .config("spark.ui.enabled", "false")
                .getOrCreate();

        Dataset<Row> df = spark.read().parquet(parquetDir);

        System.out.println("Schema:");
        df.printSchema();

        System.out.println("Data (" + df.count() + " rows):");
        df.orderBy(df.col("count").desc()).show(100, false);

        spark.stop();
    }
}