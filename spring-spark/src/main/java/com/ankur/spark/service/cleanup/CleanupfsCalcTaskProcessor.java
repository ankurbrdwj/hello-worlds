package com.ankur.spark.service.cleanup;

import java.util.*;

import com.ankur.spark.CalcTaskProcessor;
import com.ankur.spark.Profiler;
import com.ankur.spark.ProfilerFactory;
import com.ankur.spark.exception.CleanupNFSException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;

import static com.ankur.spark.ProfilerHelper.stopAndReport;
import static com.ankur.spark.service.common.FunctionConstants.VCS_EXECUTION_ID_PATTERN;
import static com.ankur.spark.service.common.FunctionConstants.VCS_EXECUTION_ID_PREFIX;
import static com.ankur.spark.service.common.PathUtils.getKeyFromPath;
import static com.ankur.spark.EnumCalcEventType.CLEAN_UP_GRAIN;

@Slf4j
@AllArgsConstructor
public class CleanupfsCalcTaskProcessor implements CalcTaskProcessor<CleanupContext> {

    @Override
    public void processCalcTask(CleanupContext cleanupContext) {
        Profiler profiler = ProfilerFactory.createAndStart();
        cleanupNFSForGrain(cleanupContext);
        stopAndReport(profiler, CLEAN_UP_GRAIN, cleanupContext.getCalcTask());
    }

    public void cleanupNFSForGrain(CleanupContext cleanupContext) {
        String vcsExecutionId = cleanupContext.getCleanupRequest().getVcsExecutionId();
        String grain = cleanupContext.getCleanupRequest().getGrain();

        if (cleanupContext.getCalculationLocation().isOnPrem()) {
            log.info("cleanupNFS calc task for grain={} of vcs_exec_id={} has started", grain, vcsExecutionId);
            cleanupNFSForOnPremGrain(cleanupContext);
            log.info("cleanupNFS calc task for grain={} of vcs_exec_id={} has finished", grain, vcsExecutionId);
        } else {
            log.info("cleanupNFS calc task is skipped for GCP grain={} of vcs_exec_id={}", grain, vcsExecutionId);
        }
    }

    private void cleanupNFSForOnPremGrain(CleanupContext context) {
        CleanupRequest request = context.getCleanupRequest();
        Map<String, Map<String, String>> grainPaths = request.getGrainPaths();
        String vcsExecutionId = request.getVcsExecutionId();
        RetryTemplate ioRetry = request.getNfsRequest().getIoRetryTemplate();

        for (Map<String, String> grainPathsForSurface : grainPaths.values()) {
            log.debug("Start collecting vcs_execution_id paths for grain in [{}], vcs_execution_id [{}]",
                    String.join(";", grainPathsForSurface.values()), vcsExecutionId);
            final List<Path> vcsExecutionIdPaths = collectVcsExecutionIdPaths(grainPathsForSurface, vcsExecutionId);
            deleteVcsExecutionIdsFromNFS(vcsExecutionIdPaths, vcsExecutionId, ioRetry);
        }
    }

    @SneakyThrows
    public List<Path> collectVcsExecutionIdPaths(final Map<String, String> paths, final String vcsExecutionId) {
        final long vcsExecutionIdTarget = Long.parseLong(vcsExecutionId);
        Set<String> pathSet = new HashSet<>(paths.values());

        return pathSet.stream()
                .flatMap(pathStr -> {
                    Path path = Paths.get(pathStr);
                    // Use try-with-resources to ensure the directory stream is closed
                    try (Stream<Path> files = Files.list(path)) {
                        // The stream from Files.list() is consumed and closed here; collect into a
                        // list and open a new in-memory stream for flatMap to return.
                        return files
                                .filter(Files::isDirectory)
                                .filter(p -> p.toString().contains(VCS_EXECUTION_ID_PREFIX))
                                .filter(p -> Optional.ofNullable(getKeyFromPath(p.toString(), VCS_EXECUTION_ID_PATTERN))
                                        .filter(id -> Long.parseLong(id) == vcsExecutionIdTarget)
                                        .isPresent())
                                .collect(Collectors.toList())
                                .stream();
                    } catch (IOException e) {
                        log.info("Could not list files for path {} for cleanup, skipping it. Reason: {}", pathStr, e.getMessage());
                        // Return an empty stream on error to continue processing other paths
                        return Stream.empty();
                    }
                })
                .collect(Collectors.toList());
    }

    public void deleteVcsExecutionIdsFromNFS(final List<Path> vcsExecutionIdPaths, final String vcsExecutionId, RetryTemplate ioRetry) {
        log.info("deleteVcsExecutionIdsFromNFS for paths [{}] has started", vcsExecutionIdPaths);
        try {
            for (Path vcsExecutionIdPath : vcsExecutionIdPaths) {
                deletePath(vcsExecutionIdPath.toString(), ioRetry);
            }
            log.info("deleteVcsExecutionIdsFromNFS for paths [{}] has finished", vcsExecutionIdPaths);
        } catch (Exception e) {
            log.error("Error deleting paths: vcsExecutionIdPaths=[{}] and vcsExecutionId=[{}]", vcsExecutionIdPaths, vcsExecutionId, e);
            throw new CleanupNFSException(e);
        }
    }

    @SneakyThrows
    private void deletePath(String path, RetryTemplate ioRetry) {
        log.info("Path deletion has started {}", path);
        ProcessBuilder builder = createProcessBuilder(path);
        try {
            ioRetry.execute(retryCallback -> {
                Process process = builder.start();
                process.waitFor();
                int exitCode = process.exitValue();
                if (exitCode != 0) {
                    throw new IOException(String.format("Failed to delete directory: [%s]", path));
                }
                return null;
            });
            log.info("Path deletion has finished {}", path);
        } catch (IOException e) {
            throw new CleanupNFSException(String.format("Error deleting path: [%s]", path), e);
        }
    }

    protected ProcessBuilder createProcessBuilder(String path) {
        String os = System.getProperty("os.name").toLowerCase();
        return os.contains("windows")
                // Implemented to test the flow with JUnit on Windows
                ? new ProcessBuilder("cmd", "/c", "rmdir", "/s", "/q", "\"" + path + "\"")
                // "rm -rf" is used instead of java.nio.Files operations for performance: load
                // testing took around 10 hours to clean up data for a full COB run otherwise.
                : new ProcessBuilder("rm", "-rf", path);
    }
}
