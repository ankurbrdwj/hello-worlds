package com.ankur.spark.basic;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import scala.Tuple2;

import java.util.Arrays;
import java.util.regex.Pattern;

public class WordCount {

    private static final Pattern SPACE = Pattern.compile(" ");

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Usage: WordCount <input-file> <output-dir>");
            System.exit(1);
        }

        String inputFile = args[0];
        String outputDir = args[1];

        SparkSession spark = SparkSession.builder()
                .appName("WordCount")
                .master("local")
                .config("spark.ui.enabled", "false")
                .getOrCreate();

        // Count words via RDD
        JavaRDD<String> lines = spark.read().textFile(inputFile).javaRDD();
        JavaPairRDD<String, Integer> wordCounts = lines
                .flatMap(s -> Arrays.asList(SPACE.split(s)).iterator())
                .filter(w -> !w.isEmpty())
                .mapToPair(word -> new Tuple2<>(word, 1))
                .reduceByKey(Integer::sum);

        // Convert to DataFrame: schema is (word STRING, count INT)
        Dataset<Row> df = spark.createDataFrame(
                wordCounts.map(t -> new WordCountRow(t._1(), t._2())),
                WordCountRow.class
        );

        // Write as Parquet
        df.write()
                .mode("overwrite")
                .parquet(outputDir);

        System.out.println("Written to Parquet: " + outputDir);
        spark.stop();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WordCountRow implements java.io.Serializable {
        private String word;
        private int count;
    }
}