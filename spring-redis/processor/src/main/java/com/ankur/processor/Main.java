package com.ankur.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        String inputDir    = env("PROCESSOR_INPUT_DIR",      "/input");
        String outputDir   = env("PROCESSOR_OUTPUT_DIR",     "/output");
        String cacheUrl    = env("CACHE_REST_BASE_URL",       "http://localhost:8080");
        int concurrency    = Integer.parseInt(env("CACHE_FETCH_CONCURRENCY", "4"));

        log.info("input={} output={} cache={} concurrency={}", inputDir, outputDir, cacheUrl, concurrency);

        Files.createDirectories(Paths.get(outputDir));

        CacheRestClient cacheClient = new CacheRestClient(cacheUrl);
        FileProcessorService service = new FileProcessorService(cacheClient, concurrency);
        service.process(Paths.get(inputDir), Paths.get(outputDir));
    }

    private static String env(String name, String defaultValue) {
        String v = System.getenv(name);
        return (v != null && !v.isEmpty()) ? v : defaultValue;
    }
}