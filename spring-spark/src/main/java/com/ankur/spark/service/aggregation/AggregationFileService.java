package com.ankur.spark.service.aggregation;

import java.util.*;

import com.ankur.spark.ParquetStream;
import com.ankur.spark.service.common.StorageService;

import org.apache.avro.generic.GenericData;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.conf.HadoopParquetConfiguration;
import org.apache.parquet.hadoop.ParquetReader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.ankur.spark.service.common.FunctionConstants.AGGREGATION_FILE_NAME_PREFIX;
import static com.ankur.spark.service.common.FunctionConstants.PARQUET_FILE_NAME_EXT;
import static com.ankur.spark.service.common.FunctionConstants.TABLEKEY_PATTERN;

@Slf4j
@AllArgsConstructor
public class AggregationFileService {

    private final StorageService fileService;

    private String getKeyFromPath(String pathString) {
        final Matcher matcher = TABLEKEY_PATTERN.matcher(pathString);

        if (matcher.find()) {
            return matcher.group().toLowerCase();
        }

        log.warn("Path [" + pathString + "] doesn't match with pattern [" + TABLEKEY_PATTERN + "]");
        return null;
    }

    public String copyParquet(String sourceDir, String targetDir, String parquetFileName) {
        String sourcePath = sourceDir + File.separator + parquetFileName;
        String targetPath = targetDir + File.separator + parquetFileName;

        fileService.copyParquet(sourcePath, targetPath);
        return targetPath;
    }

    public AggregationResult saveParquets(String targetVcsExecIdDir, int recordCount, byte[] records) {
        Set<String> aggregatedParquets = new HashSet<>();
        long totalBytes = 0L;
        if (records != null) {
            String tableKey = getKeyFromPath(targetVcsExecIdDir);
            String fileName = AGGREGATION_FILE_NAME_PREFIX + tableKey + PARQUET_FILE_NAME_EXT;
            String targetPath = targetVcsExecIdDir + File.separator + fileName;

            log.info("Going to save [{}] aggregated records at target path [{}]", recordCount, targetPath);

            fileService.deletePath(targetPath);
            fileService.writeParquet(targetPath, records);
            totalBytes = records.length;

            aggregatedParquets.add(targetPath);
        } else {
            log.warn("No aggregated records created for path [{}]", targetVcsExecIdDir);
        }
        return AggregationResult.builder().aggregatedParquetPaths(aggregatedParquets).totalBytes(totalBytes).build();
    }

    public List<String> listFileNames(String currentVcsExecutionIdPath) {
        if (!fileService.exists(currentVcsExecutionIdPath)) {
            log.warn("Skipping to aggregate as current directory [{}] does not exist.", currentVcsExecutionIdPath);
            return Collections.emptyList();
        }
        return fileService.listFileNames(currentVcsExecutionIdPath);
    }

    public ParquetReader<GenericData.Record> getParquetReader(String parquetFile) throws IOException {
        return AvroParquetReader.<GenericData.Record>builder(getParquetStream(parquetFile), new HadoopParquetConfiguration()).build();
    }

    private ParquetStream getParquetStream(String parquetFile) {
        byte[] inputBytes = fileService.readParquetAsBytes(parquetFile);

        if (inputBytes == null) {
            throw new AggregationException("No parquet bytes received from " + parquetFile);
        }

        return new ParquetStream(inputBytes);
    }

    public long getFileSize(String path) {
        byte[] bytes = fileService.readParquetAsBytes(path);
        return bytes == null ? 0L : bytes.length;
    }
}
