package com.ankur.spark;

import java.util.*;

import com.google.api.gax.paging.Page;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.CopyWriter;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.parquet.io.InputFile;

import static com.google.cloud.storage.Storage.BlobField.NAME;
import static com.google.cloud.storage.Storage.BlobListOption.fields;
import static com.google.cloud.storage.Storage.BlobListOption.prefix;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
@RequiredArgsConstructor
public class GcsParquetStorage implements ParquetStorage {

    private static final String SEPARATOR = "/";

    private final Storage storage;

    @Override
    public boolean exists(String path) {
        GcsParquetPath gcsParquetPath = validateAndCreateParquetPath(path);
        Page<Blob> blobPage = storage.list(gcsParquetPath.getBucketName(), prefix(gcsParquetPath.getPath()), fields(NAME));
        return blobPage.getValues().iterator().hasNext();
    }

    @Override
    public List<String> list(String fullPath) {
        return list(fullPath, false);
    }

    @Override
    public List<String> list(String fullPath, boolean recursive) {
        // Inferred by analogy to NfsParquetStorage's recursive-vs-flat listing; the body never
        // appeared in the pasted source, only its two call sites (list(String), walk(...)).
        GcsParquetPath gcsParquetPath = validateAndCreateParquetPath(fullPath);
        Storage.BlobListOption[] options = recursive
                ? new Storage.BlobListOption[]{prefix(gcsParquetPath.getPath())}
                : new Storage.BlobListOption[]{prefix(gcsParquetPath.getPath()), Storage.BlobListOption.currentDirectory()};

        Page<Blob> blobPage = storage.list(gcsParquetPath.getBucketName(), options);
        return StreamSupport.stream(blobPage.iterateAll().spliterator(), false)
                .map(blob -> gcsParquetPath.getBucketName() + SEPARATOR + blob.getName())
                .collect(Collectors.toList());
    }

    @Override
    public List<String> walk(String fullPath, List<Predicate<String>> predicates) {
        Predicate<String> combinedPredicate = predicates.stream()
                .reduce(Predicate::and)
                .orElse(p -> true);

        return list(fullPath, true).stream()
                .filter(combinedPredicate)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(Set<String> parquetFiles) {
        List<Boolean> deleteStatus = storage.delete(parquetFiles.stream()
                .map(parquetFile -> {
                    GcsParquetPath gcsParquetPath = splitBucketNameAndPath(parquetFile);
                    return BlobId.of(gcsParquetPath.getBucketName(), gcsParquetPath.getPath());
                })
                .collect(Collectors.toList()));

        if (deleteStatus.contains(Boolean.FALSE)) {
            log.warn("Unable to delete all of the parquet files {}", parquetFiles);
        }
    }

    @Override
    public void deleteDirectory(final String directoryPath) {
        List<String> files = list(directoryPath);
        delete(new HashSet<>(files));
    }

    @Override
    public void move(String sourcePath, String targetPath) {
        throw new UnsupportedOperationException("Implementation delayed till required.");
    }

    @Override
    public void createDirectory(String path) {
        log.debug("Call createDirectory on GCS bucket with {} path", path);
    }

    @Override
    public byte[] readBytes(String fullPath) {
        GcsParquetPath gcsParquetPath = validateAndCreateParquetPath(fullPath);
        BlobId blobId = BlobId.of(gcsParquetPath.getBucketName(), gcsParquetPath.getPath());
        try {
            Blob blob = storage.get(blobId);
            if (null != blob && blob.exists()) {
                return blob.getContent();
            } else {
                throw new ParquetStorageException(
                        String.format("No file found in bucket=%s, path=%s", gcsParquetPath.getBucketName(), gcsParquetPath.getPath()));
            }
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public void writeBytes(String fullPath, byte[] data) {
        checkIfEmpty(fullPath, data);
        GcsParquetPath gcsParquetPath = validateAndCreateParquetPath(fullPath);
        BlobId blobId = BlobId.of(gcsParquetPath.getBucketName(), gcsParquetPath.getPath());
        try (WriteChannel writer = storage.writer(BlobInfo.newBuilder(blobId).build())) {
            writer.write(ByteBuffer.wrap(data));
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public void copy(String sourceFullPath, String targetFullPath) {
        GcsParquetPath sourceParquetPath = validateAndCreateParquetPath(sourceFullPath);
        GcsParquetPath targetParquetPath = validateAndCreateParquetPath(targetFullPath);

        try {
            Page<Blob> blobPage = storage.list(sourceParquetPath.getBucketName(), prefix(sourceParquetPath.getPath()));
            StreamSupport.stream(blobPage.iterateAll().spliterator(), true).forEach(blob -> {
                String targetFileName;
                String name = blob.getName();

                if (sourceParquetPath.getPath().equals(name)) {
                    // sourceParquetPath is a file, i.e. storage.list() returns only one file which is sourceParquetPath
                    targetFileName = targetParquetPath.getPath();
                } else {
                    // sourceParquetPath is a directory which contains other files,
                    // i.e. storage.list() returns a list of files inside the directory
                    String fileName = name.contains(SEPARATOR) ? name.substring(name.lastIndexOf(SEPARATOR)) : SEPARATOR + name;
                    targetFileName = targetParquetPath.getPath() + fileName;
                }

                CopyWriter copyWriter = blob.copyTo(targetParquetPath.getBucketName(), targetFileName);
                if (!copyWriter.isDone()) {
                    throw new StorageException(500, "gcs parquet copy failed path=" + targetFullPath + targetFileName);
                }
            });
        } catch (Exception e) {
            throw new ParquetStorageException(e);
        }
    }

    @Override
    public InputFile getInputFile(String fullPath) {
        return new ParquetStream(readBytes(fullPath));
    }

    // Private helper methods.

    private String normalizePath(String path) {
        return path.replace("\\", "/");
    }

    private GcsParquetPath validateAndCreateParquetPath(String path) {
        checkIfEmpty(path);
        String normalizePath = normalizePath(path);
        GcsParquetPath gcsParquetPath = splitBucketNameAndPath(normalizePath);
        checkIfEmpty(gcsParquetPath);
        return gcsParquetPath;
    }

    private GcsParquetPath splitBucketNameAndPath(String fullPath) {
        String[] splits = fullPath.split("/", 2);
        if (splits.length != 2) {
            throw new ParquetStorageException(String.format("Unable to extract bucket name from path provided %s", fullPath));
        }
        return GcsParquetPath.with(splits);
    }

    private void checkIfEmpty(String pathForMessage, byte[] data) {
        if (data == null) {
            throw new ParquetStorageException(String.format("null data supplied to store in path=%s", pathForMessage));
        }
    }

    private void checkIfEmpty(GcsParquetPath path) {
        checkIfEmpty(path.getBucketName(), path.getPath());
    }

    private void checkIfEmpty(String bucketName, String filePath) {
        if (isEmpty(bucketName)) {
            throw new ParquetStorageException("bucket name is mandatory!");
        }
        if (isEmpty(filePath)) {
            throw new ParquetStorageException("bucket file path is mandatory!");
        }
    }

    private void checkIfEmpty(String fullPath) {
        if (isEmpty(fullPath)) {
            throw new ParquetStorageException("Parquet full path name is mandatory!");
        }
    }

    @Getter
    @RequiredArgsConstructor
    private static class GcsParquetPath {

        private final String bucketName;
        private final String path;

        public static GcsParquetPath with(String[] parts) {
            return new GcsParquetPath(parts[0], parts[1]);
        }
    }
}
