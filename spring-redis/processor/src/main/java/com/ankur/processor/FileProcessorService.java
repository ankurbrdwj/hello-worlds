package com.ankur.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileProcessorService {

    private static final Logger log = LoggerFactory.getLogger(FileProcessorService.class);

    private final CacheRestClient cacheClient;
    private final int queueCapacity;

    // Sentinel that tells each processor thread to stop draining the queue.
    private static final FetchedItem POISON = new FetchedItem(null, null);

    public FileProcessorService(CacheRestClient cacheClient, int queueCapacity) {
        this.cacheClient = cacheClient;
        this.queueCapacity = queueCapacity;
        log.info("queue-capacity={}", queueCapacity);
    }

    public void process(Path inputDir, Path outputDir) throws InterruptedException, IOException {
        List<Path> files;
        try (Stream<Path> stream = Files.list(inputDir)) {
            files = stream.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }

        if (files.isEmpty()) {
            log.info("No files to process in {}", inputDir);
            return;
        }

        log.info("Processing {} files — queue-capacity={}", files.size(), queueCapacity);

        // Bounded queue: put() blocks the calling fetcher thread when full.
        // That IS the backpressure — fetchers cannot race ahead of processors.
        // Max items in memory at any moment = queueCapacity (queued) + queueCapacity (being processed).
        BlockingQueue<FetchedItem> queue = new ArrayBlockingQueue<>(queueCapacity);

        ExecutorService fetcherPool = Executors.newFixedThreadPool(
                queueCapacity, new NamedThreadFactory("cache-fetcher"));
        ExecutorService processorPool = Executors.newFixedThreadPool(
                queueCapacity, new NamedThreadFactory("processor"));

        // Start processors first — they block on take() until items arrive.
        List<Future<?>> processorFutures = new ArrayList<>();
        for (int i = 0; i < queueCapacity; i++) {
            processorFutures.add(processorPool.submit((Callable<Void>) () -> {
                while (true) {
                    FetchedItem item = queue.take();
                    if (item == POISON) break;
                    log.info("{} processing '{}'", Thread.currentThread().getName(),
                            item.inputFile.getFileName());
                    writeOutput(item.inputFile, item.cached, outputDir);
                }
                return null;
            }));
        }

        // Submit one fetch task per file.
        // Each task does: fetch → queue.put()   ← blocks here if queue is full (backpressure)
        List<Future<?>> fetchFutures = new ArrayList<>();
        for (Path file : files) {
            fetchFutures.add(fetcherPool.submit((Callable<Void>) () -> {
                String key = file.getFileName().toString();
                log.info("{} fetching '{}'", Thread.currentThread().getName(), key);
                byte[] data = cacheClient.get(key);
                if (data == null) {
                    log.warn("Cache miss: {}", key);
                    return null;
                }
                log.info("{} fetched '{}' — {} bytes",
                        Thread.currentThread().getName(), key, data.length);
                queue.put(new FetchedItem(file, data)); // blocks when queue is full
                return null;
            }));
        }

        // Wait for all fetches to finish, then propagate any errors.
        fetcherPool.shutdown();
        fetcherPool.awaitTermination(1, TimeUnit.HOURS);
        for (Future<?> f : fetchFutures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                log.error("Fetch failed", e.getCause());
                throw new RuntimeException(e.getCause());
            }
        }

        // Tell each processor thread to stop.
        for (int i = 0; i < queueCapacity; i++) {
            queue.put(POISON);
        }

        // Wait for all processors to finish, then propagate any errors.
        processorPool.shutdown();
        processorPool.awaitTermination(1, TimeUnit.HOURS);
        for (Future<?> f : processorFutures) {
            try {
                f.get();
            } catch (ExecutionException e) {
                log.error("Processor failed", e.getCause());
                throw new RuntimeException(e.getCause());
            }
        }

        log.info("Processing complete");
    }

    private static void writeOutput(Path inputFile, byte[] cached, Path outputDir) {
        try {
            byte[] input = Files.readAllBytes(inputFile);
            byte[] merged = merge(input, cached);
            Files.write(outputDir.resolve(inputFile.getFileName()), merged);
            log.debug("{} wrote {}", Thread.currentThread().getName(), inputFile.getFileName());
        } catch (IOException e) {
            throw new RuntimeException("I/O error: " + inputFile, e);
        }
    }

    private static byte[] merge(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private static final class FetchedItem {
        final Path inputFile;
        final byte[] cached;

        FetchedItem(Path inputFile, byte[] cached) {
            this.inputFile = inputFile;
            this.cached = cached;
        }
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, prefix + "-" + counter.getAndIncrement());
        }
    }
}