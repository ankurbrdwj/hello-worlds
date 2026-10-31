package com.ankur.spark;

import org.apache.parquet.io.InputFile;

import java.util.*;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public interface ParquetStorage {

    void move(String sourcePath, String targetPath);

    void deleteDirectory(String directoryPath);

    void createDirectory(String path);

    boolean exists(String path);

    List<String> list(String filePath);

    List<String> walk(String path, List<Predicate<String>> predicates);

    List<String> list(String filePath, boolean recursive);

    void delete(Set<String> parquetFiles);

    byte[] readBytes(String fullPath);

    void writeBytes(String fullPath, byte[] data);

    void copy(String sourceFullPath, String targetFullPath);

    InputFile getInputFile(String fullPath);
}
