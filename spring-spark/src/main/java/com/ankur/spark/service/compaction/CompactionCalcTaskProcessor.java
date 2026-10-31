package com.ankur.spark.service.compaction;

import java.util.*;

import com.ankur.spark.CalcTaskProcessor;
import com.ankur.spark.Profiler;
import com.ankur.spark.ProfilerFactory;
import com.ankur.spark.service.aggregation.RawAggregationService;
import com.ankur.spark.service.common.CalcTask;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;

import static com.ankur.spark.ProfilerHelper.stopAndReport;
import static com.ankur.spark.EnumCalcEventType.COMPACT_GRAIN;
import static com.ankur.spark.EnumCalcEventType.COPY_GRAIN_TO_HDFS;
import static com.ankur.spark.EnumCalcEventType.DEDUPLICATE_GRAIN;
import static com.ankur.spark.EnumCalcEventType.LOAD_SNAPSHOT_GRAIN;

@Slf4j
@AllArgsConstructor
public class CompactionCalcTaskProcessor implements CalcTaskProcessor<CompactionContext> {

    @Getter(AccessLevel.PACKAGE)
    private final CompactionLauncherService compactionLauncherService;

    @Getter(AccessLevel.PACKAGE)
    private final RawAggregationService rawAggregationService;

    @Override
    public void processCalcTask(CompactionContext compactionContext) {
        CompactionRequest request = compactionContext.getCompactionRequest();
        Map<String, String> grainPaths = request.getGrainPaths();
        String grainPathsSet = String.join(":", request.getGrainPaths().values());
        String vcsExecutionId = request.getVcsExecutionId();
        log.info("Deduplication & compaction process for vcs_execution_id [{}] has started", vcsExecutionId);
        CalcTask calcTask = compactionContext.getCalcTask();

        try {
            request.getProcessRetryTemplate().execute((RetryContext retryCallback) -> {
                processRawAggregates(compactionContext);

                DeduplicatedData deduplicatedData;
                if (request.isUseReportingSnapshots()) {
                    deduplicatedData = loadSnapshotGrain(calcTask, grainPaths, request.getReportingSnapshotsTrades());
                } else {
                    deduplicatedData = deduplicateGrain(calcTask, grainPaths, vcsExecutionId, getGrainDeduplicationBy(compactionContext));
                }

                CompactionResult compactionResult = compactGrain(calcTask, request, grainPaths, deduplicatedData);
                calcTask.setResultBytes(compactionResult.getTotalBytes());

                if (!compactionContext.getCalculationLocation().isOnPrem()) {
                    copyGrainToHdfs(calcTask, request, grainPaths, compactionResult.getCompactedParquetPaths());
                }

                log.info("Compaction processing is completed for grain path [{}], vcs_execution_id [{}]. Total bytes written [{}]",
                        grainPaths, vcsExecutionId, compactionResult.getTotalBytes());
                return null;
            });
        } catch (final Exception e) {
            log.error("Deduplication & compaction for grain paths [{}], vcs_execution_id [{}] failed", grainPathsSet, vcsExecutionId, e);
            throw e;
        } finally {
            log.info("Deduplication & compaction process for vcs_execution_id [{}] has finished", vcsExecutionId);
        }
    }

    private void processRawAggregates(CompactionContext compactionContext) {
        if (compactionContext.getCompactionRequest().isRawAggregationEnabled()) {
            log.info("Starting raw aggregation");
            rawAggregationService.processRawAggregates(compactionContext);
            log.info("Raw aggregation is completed");
        }
    }

    private void copyGrainToHdfs(CalcTask calcTask, CompactionRequest request, Map<String, String> grainPaths,
                                  Collection<String> compactedParquetPaths) {
        Profiler profiler = ProfilerFactory.createAndStart();
        compactionLauncherService.copyGrainToHdfs(grainPaths, request.getVcsExecutionId(), compactedParquetPaths, request.getInSurface());
        stopAndReport(profiler, COPY_GRAIN_TO_HDFS, calcTask);
    }

    private CompactionResult compactGrain(CalcTask calcTask, CompactionRequest compactionRequest, Map<String, String> grainPaths,
                                           DeduplicatedData deduplicatedData) {
        Profiler profiler = ProfilerFactory.createAndStart();
        CompactionResult compactionResult =
                compactionLauncherService.compactGrain(compactionRequest, grainPaths, deduplicatedData, calcTask.getCalcTaskUID());
        stopAndReport(profiler, COMPACT_GRAIN, calcTask);
        return compactionResult;
    }

    private DeduplicatedData deduplicateGrain(CalcTask calcTask, Map<String, String> grainPaths, String vcsExecutionId,
                                               String grainDeduplicationBy) {
        Profiler profiler = ProfilerFactory.createAndStart();
        DeduplicatedData deduplicatedData = compactionLauncherService.deduplicateGrain(grainPaths, vcsExecutionId, grainDeduplicationBy);
        stopAndReport(profiler, DEDUPLICATE_GRAIN, calcTask);
        return deduplicatedData;
    }

    private DeduplicatedData loadSnapshotGrain(CalcTask calcTask, Map<String, String> grainPaths, List<ReportingSnapshotsTrade> trades) {
        Profiler profiler = ProfilerFactory.createAndStart();
        DeduplicatedData deduplicatedData = compactionLauncherService.loadSnapshotGrain(grainPaths, trades);
        stopAndReport(profiler, LOAD_SNAPSHOT_GRAIN, calcTask);
        return deduplicatedData;
    }

    private static String getGrainDeduplicationBy(CompactionContext compactionContext) {
        return (compactionContext != null && compactionContext.getCompactionRequest() != null)
                ? compactionContext.getCompactionRequest().getGrainDeduplicationBy()
                : null;
    }
}
