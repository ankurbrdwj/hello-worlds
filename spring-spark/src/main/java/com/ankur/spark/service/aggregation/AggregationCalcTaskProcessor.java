package com.ankur.spark.service.aggregation;

import java.util.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.ankur.spark.CalcTaskProcessor;
import com.ankur.spark.Profiler;
import com.ankur.spark.ProfilerFactory;
import com.ankur.spark.service.common.CalcTask;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;

import static com.ankur.spark.EnumCalcEventType.AGGREGATE_GRAIN;
import static com.ankur.spark.ProfilerHelper.stopAndReport;

@Slf4j
@AllArgsConstructor
public class AggregationCalcTaskProcessor implements CalcTaskProcessor<AggregationContext> {

    private final AggregationLauncherService aggregationLauncherService;

    @Override
    public void processCalcTask(AggregationContext aggregationContext) {
        Profiler profiler = ProfilerFactory.createAndStart();
        AggregationRequest request = aggregationContext.getAggregationRequest();
        final Map<String, String> grainPaths = request.getGrainPaths();
        String grainPathsSet = String.join(";", request.getGrainPaths().values());
        AtomicLong totalBytes = new AtomicLong(0L);
        CalcTask calcTask = aggregationContext.getCalcTask();

        try {
            request.getProcessRetryTemplate().execute((RetryContext retryCallback) -> {
                request.getTablesToLand().forEach(tableKey -> {
                    log.info("Start processing [{}].", tableKey);
                    String currentParquetPath = grainPaths.get(tableKey);
                    AggregationResult aggregationResult;
                    String targetExecutionIdPath = currentParquetPath.replace(request.getInSurface(), request.getOutSurface());

                    if (request.getAggregationCriteria().containsKey(tableKey)) {
                        log.info("Start grain aggregation for tableKey [{}], grain_path [{}]", tableKey, currentParquetPath);
                        aggregationResult = aggregationLauncherService.processAggregation(targetExecutionIdPath, currentParquetPath, tableKey);
                        log.info("Aggregation is completed for grain path [{}], type [{}], generated [{}] aggregated files.",
                                currentParquetPath, tableKey,
                                aggregationResult.getAggregatedParquetPaths() != null ? aggregationResult.getAggregatedParquetPaths().size() : 0);
                    } else {
                        log.info("Start grain copy with no aggregation for tableKey [{}], grain path [{}]", tableKey, currentParquetPath);
                        aggregationResult = aggregationLauncherService.processWithNoAggregation(targetExecutionIdPath, currentParquetPath);
                        log.info("Copy with no aggregation is completed for grain path [{}], type [{}], generated [{}] files.",
                                currentParquetPath, tableKey,
                                aggregationResult.getAggregatedParquetPaths() != null ? aggregationResult.getAggregatedParquetPaths().size() : 0);
                    }

                    totalBytes.addAndGet(aggregationResult.getTotalBytes());
                });

                log.info("Completed processing aggregation for grain path [{}] and with criteria [{}]. Total bytes written [{}]",
                        grainPathsSet, request.getAggregationCriteria(), totalBytes.get());
                calcTask.setResultBytes(totalBytes.get());
                return null;
            });
        } catch (final Exception e) {
            log.error("Aggregation for grain_path [{}] failed", grainPathsSet, e);
            throw new AggregationException("Aggregation failed for grain path [" + grainPathsSet + "]", e);
        } finally {
            stopAndReport(profiler, AGGREGATE_GRAIN, aggregationContext.getCalcTask());
        }
    }

}
