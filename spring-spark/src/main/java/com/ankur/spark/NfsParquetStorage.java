package com.ankur.spark;

import java.util.*;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ankur.spark.service.common.PathHelper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.hadoop.util.HadoopInputFile;
import org.apache.parquet.io.InputFile;

import static com.ankur.spark.service.aggregation.AggregationConstants.AVRO_CONF;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
public class NfsParquetStorage implements ParquetStorage {

    @Override
    public void move(String sourcePath, String targetPath) {
        if (Files.isDirectory(Paths.get(sourcePath))) {
            moveDirectory(sourcePath, targetPath);
        } else {
            moveFile(sourcePath, targetPath);
        }
    }

    private void moveFile(final String sourcePath, final String targetPath) {
        log.debug("Moving file [{}] to [{}]", sourcePath, targetPath);
        try {
            Path target = Paths.get(targetPath);
            Files.move(Paths.get(sourcePath), target, REPLACE_EXISTING);
            log.debug("File [{}] was successfully moved to [{}]", sourcePath, targetPath);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    private void moveDirectory(final String sourcePath, final String targetPath) {
        log.debug("Moving directory [{}] to [{}]", sourcePath, targetPath);
        try {
            Files.move(Paths.get(sourcePath), Paths.get(targetPath), ATOMIC_MOVE);
            log.debug("Directory [{}] was successfully moved to [{}]", sourcePath, targetPath);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public void deleteDirectory(final String directoryPath) {
        Path path = Paths.get(directoryPath);
        log.debug("Deleting directory [{}]", directoryPath);
        try (Stream<Path> pathStream = Files.list(path)) {
            pathStream.forEach(this::delete);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
        delete(path);
        log.debug("Directory [{}] was deleted successfully", directoryPath);
    }

    @SneakyThrows
    private void delete(Path path) {
        try {
            Files.delete(path);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public void createDirectory(final String path) {
        log.debug("Creating directories [{}]", path);
        try {
            Files.createDirectories(Paths.get(path));
            log.debug("Path [{}] was created successfully", path);
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public List<String> list(String filePath) {
        checkPathIsNotEmpty(filePath);
        try (Stream<Path> pathStream = Files.list(Paths.get(filePath))) {
            return pathStream.map(PathHelper::toUnixPathString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public List<String> walk(String path, List<Predicate<String>> predicates) {
        checkPathIsNotEmpty(path);
        Predicate<String> combinedPredicate = predicates.stream()
                .reduce(Predicate::and)
                .orElse(p -> true);

        try (Stream<Path> pathStream = Files.walk(Paths.get(path))) {
            return pathStream.map(PathHelper::toUnixPathString)
                    .filter(combinedPredicate)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public List<String> list(String filePath, boolean recursive) {
        if (isEmpty(filePath)) {
            throw new ParquetStorageException("Paths must not be null or empty, filePath=" + filePath);
        }

        if (recursive) {
            return walk(filePath, new ArrayList<>());
        }

        Path path = Paths.get(filePath);
        try (Stream<Path> pathStream = Files.list(path)) {
            return pathStream.map(PathHelper::toUnixPathString)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public void delete(Set<String> parquetFiles) {
        if (null == parquetFiles || parquetFiles.isEmpty()) {
            throw new ParquetStorageException("empty or null input");
        }

        parquetFiles.forEach(path -> {
            try {
                if (null == path) {
                    throw new ParquetStorageException("empty or null input");
                }

                if (Files.isDirectory(Paths.get(path))) {
                    deleteDirectory(path);
                } else {
                    delete(Paths.get(path));
                }
            } catch (Exception e) {
                throw new ParquetStorageException(String.format("Error deleting path at [%s]", path), e);
            }
        });
    }

    @Override
    public byte[] readBytes(String fullPath) {
        try {
            return FileUtils.readFileToByteArray(new File(fullPath));
        } catch (IOException e) {
            throw new ParquetStorageException(String.format("Error reading byte array for path [%s]", fullPath), e);
        }
    }

    @Override
    public void writeBytes(String fullPath, byte[] data) {
        try {
            FileUtils.writeByteArrayToFile(new File(fullPath), data);
        } catch (IOException e) {
            throw new ParquetStorageException(String.format("Error writing byte array for path [%s]", fullPath), e);
        }
    }

    @Override
    public void copy(String sourceFullPath, String targetFullPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputFile getInputFile(String fullPath) {
        try {
            return HadoopInputFile.fromPath(new org.apache.hadoop.fs.Path(fullPath), AVRO_CONF);
        } catch (IOException ioe) {
            throw new ParquetStorageException("Error opening parquet input  path[{" + fullPath + "}]", ioe);
        }
    }

    private void checkPathIsNotEmpty(String path) {
        if (isEmpty(path)) {
            throw new ParquetStorageException("Path must not be null or empty, path=" + path);
        }
    }
}
