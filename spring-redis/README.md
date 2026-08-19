# Spring Redis — OOM Demo

Demonstrates how unbounded concurrent heap allocations cause `OutOfMemoryError` in a
multi-threaded Java processor, and how industry-standard patterns fix it.

---

## Architecture

```
┌─────────────┐        HTTP GET /cache/{key}        ┌──────────────┐      GET/SET
│  processor  │  ──────────────────────────────────▶ │ cache-service│ ◀──────────── Redis
│             │  ◀──────────────────────────────────  │  (Spring Boot│
│  16 files   │        20 MB byte[] response          │   port 8080) │
└─────────────┘                                       └──────────────┘
```

### Thread model — bounded queue + backpressure (current implementation)

```
cache-fetcher-1 \
cache-fetcher-2  ──put()──▶  [ArrayBlockingQueue(N)]  ──take()──▶  processor-1
cache-fetcher-3 /                                                   processor-2
cache-fetcher-4 /              ▲ blocks here                        processor-3
                               when full                            processor-4
```

- **`cache-fetcher-N`** threads fetch from the cache service and `put()` into the queue.
  `put()` **blocks** when the queue is full — that is the backpressure.
- **`processor-N`** threads `take()` from the queue, merge with the input file, and write output.
- Both pools are sized to `CACHE_FETCH_CONCURRENCY`. The queue capacity is the same value.

---

## Why OOM occurs (the demo)

With `CACHE_FETCH_CONCURRENCY=16` and 16 files, the queue capacity equals the file count.
All 16 fetches complete before any `put()` ever blocks — backpressure never triggers.
All 16 processor threads then call `writeOutput` simultaneously:

```
per processor during writeOutput:
  cached[]  = 20 MB
  input[]   = 20 MB   (Files.readAllBytes)
  merged[]  = 40 MB   (merge result)
  ─────────────────
  per task  = 80 MB

16 tasks × 80 MB = 1280 MB  >  -Xmx256m  →  OOM
```

| | OOM run | Fixed run |
|---|---|---|
| `CACHE_FETCH_CONCURRENCY` | 16 | 4 |
| Queue capacity | 16 (= file count, no backpressure) | 4 (backpressure active) |
| Peak concurrent `writeOutput` | 16 | 4 |
| Peak heap | 16 × 80 MB = **1280 MB** | 4 × 80 MB = 320 MB |
| `-Xmx` | 256 MB | 512 MB |
| Result | OOM | completes |

### Why `-XX:+ExitOnOutOfMemoryError` prevents the OOM from being logged

The `catch (Throwable t)` block in the fetcher cannot log the OOM because
`-XX:+ExitOnOutOfMemoryError` calls `os::exit()` at the JVM level the moment OOM is thrown —
before the JVM unwinds the stack and before any `catch` block gets control.
Even without that flag, `log.error()` itself needs heap to format the message, so it would
throw a second OOM. The heap dump (`-XX:+HeapDumpOnOutOfMemoryError`) is the right
post-mortem tool — it is written by the OS, not the JVM heap.

---

## Prerequisites

- Docker Desktop (with Compose v2)
- 2 GB free RAM recommended

---

## Steps to reproduce

### 1. Create input files and host directories

```bash
bash setup-input.sh
```

Creates `data/input/file_00.dat` .. `file_15.dat` and the `dumps/` directories.

### 2. Build images

```bash
docker compose build
```

### 3. Start Redis and cache-service

```bash
docker compose up -d redis cache-service
```

On startup `cache-service` pre-loads **16 keys × 20 MB = 320 MB** into Redis.
Wait until healthy:

```bash
docker compose ps          # cache-service should show (healthy)
```

### 4. Run the OOM demo

```bash
rm -f dumps/processor-oom/heapdump.hprof
docker compose --profile oom up processor-oom
```

The container exits with code **3** (JVM killed by `-XX:+ExitOnOutOfMemoryError`).

### 5. Inspect the heap dump

```bash
ls -lh dumps/processor-oom/
# heapdump.hprof  ~250 MB
```

Open with Eclipse MAT or VisualVM. Look for `byte[]` instances dominating the heap —
each ~20 MB, one per processor thread.

### 6. Run the fixed version

```bash
rm -f dumps/processor-fixed/heapdump.hprof
docker compose --profile fixed up --build processor-fixed
```

`CACHE_FETCH_CONCURRENCY=4` → queue capacity 4 → fetchers block on `put()` when full →
only 4 × 80 MB = 320 MB live at any time → fits in -Xmx512m.
Output files appear in `data/output-fixed/`.

---

## Key JVM flags

| Flag | Effect |
|---|---|
| `-Xmx256m` / `-Xmx512m` | Hard heap ceiling for the processor |
| `-XX:+HeapDumpOnOutOfMemoryError` | Write heap dump on OOM (OS-level, always works) |
| `-XX:HeapDumpPath=/dumps/heapdump.hprof` | Dump lands in the mounted `dumps/` volume |
| `-XX:+ExitOnOutOfMemoryError` | Shut JVM down immediately on OOM (no zombie state) |

---

## Clean up

```bash
docker compose --profile oom --profile fixed down -v
rm -rf data/output-oom data/output-fixed dumps/processor-oom/heapdump.hprof dumps/processor-fixed/heapdump.hprof
```

---

## Industry-standard solutions for concurrent OOM

### 1. Bounded queue + backpressure *(implemented here)*

The fetchers are producers, the processors are consumers, connected by a
`ArrayBlockingQueue` with a fixed capacity. When the queue is full, `put()` blocks
the fetcher — no more data enters memory until a processor frees a slot.

```
[cache-fetcher-N] ──put()──▶ [ArrayBlockingQueue(4)] ──take()──▶ [processor-N]
                               ▲ blocks here when full
```

```java
BlockingQueue<FetchedItem> queue = new ArrayBlockingQueue<>(4);

// Fetcher — blocks here if queue is full (backpressure)
queue.put(new FetchedItem(file, data));

// Processor — blocks here if queue is empty
FetchedItem item = queue.take();
```

Backpressure flows automatically: producers can never race ahead of consumers.
Used by: Kafka consumers, Apache Flink, Apache Spark.

---

### 2. Semaphore on in-flight

A `Semaphore` is a counter with `acquire()` and `release()`. Set it to N permits —
that becomes a hard cap on how many operations hold memory simultaneously.

```java
Semaphore inFlight = new Semaphore(4); // only 4 fetches can hold memory at once

for (Path file : files) {
    inFlight.acquire();                // blocks if 4 are already running
    fetcherPool.submit(() -> {
        try {
            byte[] data = cacheClient.get(key);
            process(data);
        } finally {
            inFlight.release();        // free the slot when done
        }
    });
}
```

**vs. bounded queue:** a semaphore couples fetch and process into one step.
A bounded queue keeps them as separate pipeline stages that can scale independently.
Use a semaphore when the operation is a single unit (e.g. "limit concurrent DB calls to 10").
Use a bounded queue when fetch and process are naturally separate stages.

---

### 3. Streaming / no materialization

**Materializing** means loading the entire response into a `byte[]` before doing anything.
Your `CacheRestClient.drain()` does this — it reads the full 20 MB into a
`ByteArrayOutputStream` before returning:

```java
// Current — materializes 20 MB into heap before returning
private static byte[] drain(InputStream in) throws IOException {
    ByteArrayOutputStream buf = new ByteArrayOutputStream(); // grows to 20 MB
    byte[] chunk = new byte[8192];
    int n;
    while ((n = in.read(chunk)) != -1) buf.write(chunk, 0, n);
    return buf.toByteArray(); // second 20 MB copy
}
```

**Streaming alternative** — pass the `InputStream` directly to the writer and pipe
bytes to disk as they arrive, never holding the full payload in memory:

```java
// Streaming — only an 8 KB chunk lives in heap at a time
void mergeToFile(InputStream cached, Path inputFile, Path outputFile) throws IOException {
    try (OutputStream out = Files.newOutputStream(outputFile)) {
        Files.copy(inputFile, out);  // stream input file straight to output
        cached.transferTo(out);      // stream cache response straight to output
    }
}
```

| Approach | Heap per file |
|---|---|
| Current (materialize) | 20 MB cached + 20 MB input + 40 MB merged = **80 MB** |
| Streaming merge | ~8 KB chunk buffer = **~8 KB** |

Note: streaming requires changing the `merge` contract from `byte[] → byte[]` to
`InputStream → OutputStream`. It is the highest-leverage fix per file but requires
rethinking the processing model end-to-end.

---

### 4. Calculated concurrency

Pick the thread/pool count from actual resource constraints rather than an arbitrary number.

**Formula:**
```
max_concurrent = (available_heap × headroom_factor) / memory_per_task
```

For this processor (80 MB per task during `writeOutput`):
```
-Xmx256m:  256 × 0.7 / 80 ≈ 2 concurrent tasks
-Xmx512m:  512 × 0.7 / 80 ≈ 4 concurrent tasks
-Xmx1g:   1024 × 0.7 / 80 ≈ 8 concurrent tasks
```

Applied in code — calculate at startup instead of hardcoding:

```java
long heapBytes    = Runtime.getRuntime().maxMemory(); // respects -Xmx
long bytesPerTask = (20 + 20 + 40) * 1024L * 1024L;  // 80 MB
double headroom   = 0.70;                              // reserve 30% for GC overhead
int safeConcurrency = (int) Math.max(1, (heapBytes * headroom) / bytesPerTask);

log.info("heap={}MB  bytesPerTask={}MB  safeConcurrency={}",
        heapBytes / 1024 / 1024, bytesPerTask / 1024 / 1024, safeConcurrency);
```

The same JAR at `-Xmx256m` runs 2 concurrent tasks; at `-Xmx2g` it runs 17 — no code change.

Calculated concurrency **tells you what N to use**. Bounded queue or semaphore **enforces
that N at runtime**. You need both.

---

### 5. Reactive Streams / Project Reactor

Reactive frameworks have backpressure built into the protocol. The `Publisher/Subscriber`
contract includes a `request(n)` signal — downstream processors tell upstream producers
exactly how many items they can handle. Producers never send more.

```java
// Spring WebFlux example
Flux.fromIterable(files)
    .flatMap(file -> fetchFromCache(file), 4)  // max 4 concurrent fetches
    .flatMap(item -> processItem(item), 4)      // max 4 concurrent processors
    .blockLast();
```

The `4` argument to `flatMap` is the concurrency limit — backpressure is handled
automatically by the framework. Used heavily in Spring WebFlux + R2DBC stacks.

---

### 6. Off-heap / Memory-mapped buffers

Avoid the Java heap entirely for large buffers. The GC never sees these allocations,
so they cannot trigger OOM or GC pressure:

| Mechanism | API | Used by |
|---|---|---|
| Direct byte buffer | `ByteBuffer.allocateDirect()` | NIO, Netty |
| Memory-mapped file | `FileChannel.map()` | Kafka log segments |
| Pooled buffers | Netty `PooledByteBufAllocator` | Netty, gRPC |

Kafka stores its log segments as memory-mapped files for exactly this reason —
hundreds of GB of data managed by the OS paging system, invisible to the JVM heap.

---

### Summary

| Pattern | Addresses | Where used |
|---|---|---|
| Bounded queue + backpressure | Memory accumulation between pipeline stages | Kafka, Flink, Spark |
| Semaphore on in-flight | Concurrent heap spikes for single-unit operations | Most microservices |
| Streaming / no materialization | Per-item memory size | HTTP clients, ETL pipelines |
| Reactive Streams | End-to-end flow control with protocol-level backpressure | Spring WebFlux |
| Calculated concurrency | Deriving the right N from heap budget | Any JVM service |
| Off-heap buffers | Removing large buffers from GC visibility entirely | Kafka, Netty, Cassandra |