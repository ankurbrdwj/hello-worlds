package com.ankur.spark.service.common;

import java.util.List;

public interface StorageService {

    boolean exists(String path);

    List<String> listFileNames(String path);

    List<String> listFileNames(String path, boolean recursive);

    byte[] readParquetAsBytes(String path);

    void writeParquet(String path, byte[] content);

    void copyParquet(String sourcePath, String targetPath);

    void deletePath(String path);

    void deleteDirectory(String path);
}
