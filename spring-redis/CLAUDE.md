# file-processor

Standalone plain Java 8 worker service (no Spring). Reads files from an input directory, fetches enrichment data from an external cache service over HTTP, merges them, and writes results to an output directory.

## Build

```bash
./gradlew shadowJar       # produces build/libs/file-processor-1.0.0.jar
```

Uses the Shadow plugin to produce a fat JAR. No test suite — run via Docker.

## Run (Docker)

```bash
docker-compose up processor
```

Environment variables (all have defaults):

| Variable | Default | Purpose |
|---|---|---|
| `PROCESSOR_INPUT_DIR` | `/input` | Directory of input files to process |
| `PROCESSOR_OUTPUT_DIR` | `/output` | Directory for merged output files |
| `CACHE_REST_BASE_URL` | `http://localhost:8080` | Base URL of the cache service |
| `CACHE_FETCH_CONCURRENCY` | `4` | Max concurrent cache fetches (see OOM note below) |
| `JAVA_XMX` | `512m` | JVM max heap |

JVM flags `-XX:+HeapDumpOnOutOfMemoryError` and `-XX:+ExitOnOutOfMemoryError` are always active. Heap dumps land in `/dumps/heapdump.hprof`.

## Architecture

```
Main
 └── FileProcessorService
      ├── processorExecutor  (16 threads, "processor-oom-N")
      │    └── SplitTask — iterates files, calls merge, writes output
      └── cacheExecutor      (CACHE_FETCH_CONCURRENCY threads, "cache-fetcher-N")
           └── CacheRestClient.get(key) — HTTP GET /cache/{key}
```

- Processor threads submit cache fetches to the `cacheExecutor` pool and block on `Future.get()`.
- The cache client uses plain `HttpURLConnection` (Java 8, zero external deps).
- Files are partitioned across 16 processor threads; each thread handles its split sequentially.

## OOM Scenario (intentional demo)

Setting `CACHE_FETCH_CONCURRENCY=16` (equal to `THREAD_COUNT`) causes all 16 fetcher threads to load ~20 MB each simultaneously (~320 MB+), then all 16 processor threads hold those byte arrays while sleeping before merge, then each tries to allocate another 20 MB for the merged result — blowing past `-Xmx256m`.

Setting `CACHE_FETCH_CONCURRENCY=4` bounds in-flight heap to `4 × 20 MB = 80 MB` and avoids OOM. This is the backpressure fix: the `cacheExecutor` pool size acts as a semaphore on concurrent allocations.

## Key Files

- `Main.java` — entry point, reads env vars, wires dependencies
- `FileProcessorService.java` — threading model, split/merge logic, OOM demo code
- `CacheRestClient.java` — HTTP client, returns `null` on 404
- `Dockerfile` — two-stage build (gradle:7.6.3-jdk8 → amazoncorretto:8)

