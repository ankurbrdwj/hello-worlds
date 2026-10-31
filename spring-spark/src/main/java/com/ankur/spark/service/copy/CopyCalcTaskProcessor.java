package com.ankur.spark.service.copy;

import java.util.*;

import com.ankur.spark.CalcTaskProcessor;
import com.ankur.spark.Profiler;
import com.ankur.spark.ProfilerFactory;
import com.ankur.spark.exception.ConfigurationException;
import com.ankur.spark.service.common.StorageService;
import com.google.common.annotations.VisibleForTesting;

import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static com.ankur.spark.ProfilerHelper.stopAndReport;
import static com.ankur.spark.service.common.FunctionConstants.VCS_EXECUTION_ID_PREFIX;
import static com.ankur.spark.EnumCalcEventType.COPY_GRAIN_TO_ONPREM;

@Slf4j
@AllArgsConstructor
public class CopyCalcTaskProcessor implements CalcTaskProcessor<CopyContext> {

    private final StorageService gcsStorage;

    @Setter
    private StorageService hdfsStorage;

    private final CopyPathHelper pathHelper;

    @Override
    public void processCalcTask(CopyContext context) {
        Profiler profiler = ProfilerFactory.createAndStart();
        processGrain(context);
        stopAndReport(profiler, COPY_GRAIN_TO_ONPREM, context.getCalcTask());
    }

    @VisibleForTesting
    public void processGrain(CopyContext context) {
        String vcsExecutionId = context.getRequest().getVcsExecutionId();
        String grain = context.getRequest().getGrain();

        if (context.getCalculationLocation().isOnPrem()) {
            log.info("CopyToOnPrem calc task is skipped for OnPrem grain={} of vcs_exec_id={}", grain, vcsExecutionId);
        } else {
            log.info("CopyToOnPrem calc task for grain={} of vcs_exec_id={} has started", grain, vcsExecutionId);
            processGcpGrain(context);
            log.info("CopyToOnPrem calc task for grain={} of vcs_exec_id={} has finished", grain, vcsExecutionId);
        }
    }

    private void processGcpGrain(CopyContext context) {
        CopyRequest request = context.getRequest();
        for (CopySurface surface : request.getSurfaces()) {
            log.info("Processing surface mapping: inSurface={}, outSurface={}", surface.getInSurface(), surface.getOutSurface());
            for (String tableKey : surface.getGrainPaths().keySet()) {
                processGrainTableKey(tableKey, request, surface);
            }
        }
    }

    private void processGrainTableKey(String tableKey, CopyRequest request, CopySurface surface) {
        log.info("Starting tableKey={} processing", tableKey);
        cleanupTargetHdfsDirectory(tableKey, request, surface);
        List<CopyObject> copyList = getCopyObjectList(tableKey, request, surface);
        copyObjectsToHdfs(copyList);
        log.info("Processing of tableKey={} is completed", tableKey);
    }

    @VisibleForTesting
    public void cleanupTargetHdfsDirectory(String tableKey, CopyRequest request, CopySurface surface) {
        String hdfsDirectory = pathHelper.getHdfsTargetDirectory(tableKey, request, surface);
        log.info("HDFS target directory for tableKey={} is {}", tableKey, hdfsDirectory);

        if (!hdfsDirectory.contains(VCS_EXECUTION_ID_PREFIX)) {
            throw new ConfigurationException("HDFS target directory does not contain VcsExecutionId! Check valuation context!");
        }

        if (hdfsStorage.exists(hdfsDirectory)) {
            log.info("HDFS target directory already exists! Starting cleanup!");
            hdfsStorage.deleteDirectory(hdfsDirectory);
        }
    }

    private List<CopyObject> getCopyObjectList(String tableKey, CopyRequest request, CopySurface surface) {
        List<String> gcsObjects = getObjectsToBeCopiedFromGcs(tableKey, request, surface);
        return gcsObjects.stream()
                .map(gcsObject -> pathHelper.getCopyObject(gcsObject, tableKey, request, surface))
                .collect(Collectors.toList());
    }

    private List<String> getObjectsToBeCopiedFromGcs(String tableKey, CopyRequest request, CopySurface surface) {
        String sourceDirectory = pathHelper.getGcsSourceDirectory(tableKey, request, surface);
        List<String> objects = gcsStorage.listFileNames(sourceDirectory, true);
        log.info("GCS grain path for tableKey={} contains {} objects and is {}", tableKey, objects.size(), sourceDirectory);
        return objects;
    }

    private void copyObjectsToHdfs(List<CopyObject> copyList) {
        for (CopyObject copyObject : copyList) {
            log.info("Copying object {} from {} to {}", copyObject.getObjectName(), copyObject.getFromDirectory(), copyObject.getToDirectory());
            byte[] parquetObject = gcsStorage.readParquetAsBytes(copyObject.getFromPath());
            log.info("Object size is {} bytes", parquetObject.length);
            hdfsStorage.writeParquet(copyObject.getToPath(), parquetObject);
        }
    }
}
