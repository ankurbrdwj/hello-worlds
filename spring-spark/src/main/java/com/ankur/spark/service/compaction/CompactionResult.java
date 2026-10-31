package com.ankur.spark.service.compaction;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;

@Getter
@Builder
@ToString
public class CompactionResult {

    private final Collection<String> compactedParquetPaths;
    private final long totalBytes;
}
