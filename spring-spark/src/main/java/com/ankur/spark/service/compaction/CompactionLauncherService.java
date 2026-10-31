package com.ankur.spark.service.compaction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CompactionLauncherService {

    DeduplicatedData deduplicateGrain(Map<String, String> grainPaths, String vcsExecutionId, String grainDeduplicationBy);

    DeduplicatedData loadSnapshotGrain(Map<String, String> grainPaths, List<ReportingSnapshotsTrade> trades);

    CompactionResult compactGrain(CompactionRequest request, Map<String, String> grainPaths, DeduplicatedData deduplicatedData,
                                  String calcTaskUid);

    void copyGrainToHdfs(Map<String, String> grainPaths, String vcsExecutionId, Collection<String> compactedParquetPaths,
                         String inSurface);
}
