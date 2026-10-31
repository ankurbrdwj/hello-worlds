package com.ankur.spark;

import java.io.IOException;
import java.util.*;

import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;

import static com.ankur.spark.service.aggregation.AggregationConstants.AVRO_CONF;

@Slf4j
@Getter
@RequiredArgsConstructor
public class NfsToHdfsParquetStorage implements ParquetStorage {

    private static final String NO_IMPLEMENTATION_USE_ALTERNATIVE = "No Implementation, please use NfsParquetStorage.class";

    private final FileSystem hadoopFileSystem;
    private final String targetBasePath;

    @Override
    public void copy(String sourcePath, String destinationPath) {
        try {
            log.info("Copying file [{}] to [{}]", sourcePath, destinationPath);
            hadoopFileSystem.copyFromLocalFile(new Path(sourcePath), new Path(destinationPath));
            log.debug("File [{}] was successfully copied to [{}]", sourcePath, destinationPath);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public InputFile getInputFile(String fullPath) {
        try {
            return HadoopInputFile.fromPath(new Path(fullPath), hadoopFileSystem.getConf());
        } catch (IOException ioe) {
            throw new ParquetStorageException("Error opening parquet input  path[{" + fullPath + "}]", ioe);
        }
    }


    @Override
    public void move(String sourcePath, String destinationPath) {
        try {
            log.info("Copying file [{}] to [{}]", sourcePath, destinationPath);
            hadoopFileSystem.moveFromLocalFile(new Path(sourcePath), new Path(destinationPath));
            log.debug("File [{}] was successfully copied to [{}]", sourcePath, destinationPath);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public void createDirectory(String newDirectoryPath) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public List<String> list(String path) {
        return list(path, false);
    }

    @Override
    public List<String> list(String path, boolean recursive) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public List<String> walk(String path, List<Predicate<String>> predicates) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public boolean exists(String path) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public void delete(Set<String> paths) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public void deleteDirectory(final String directoryPath) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public byte[] readBytes(String path) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }

    @Override
    public void writeBytes(String path, byte[] data) {
        throw new UnsupportedOperationException(NO_IMPLEMENTATION_USE_ALTERNATIVE);
    }
}
