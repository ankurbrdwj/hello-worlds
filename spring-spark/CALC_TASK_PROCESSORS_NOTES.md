# CalcTaskProcessor family — reconstruction notes

## Session 2: line-number cleanup + Parquet/OpAdj vendoring batch

Additional work after the initial 6-file pass (below):

- **Stripped stray OCR line-number lines** from all files — pure-digit lines like `49`, `51`, `4514` that were IDE gutter numbers captured as if they were code tokens. Mechanical `sed` pass, safe (verified it didn't touch any line with digits embedded in real content, e.g. `5 usages`, `value: 8.0d`).
- **Corrected the internal package prefix** from `com.db.ga...` to `com.db.gm...` everywhere. Reason: three literal `package com.db.gm.portfolio...` statements survived (uncorrupted) inside later pasted files (`OpjAdjContext.java`'s `RolloverContext` section, `RolloverZeroScenarioShift.java`'s `TradeFlatten` section, and a breadcrumb-bar artifact inside `CommonShadedParquetWriter.java`) — much stronger evidence than the earlier frequency-based guess. Bulk `sed` across the five processor files; `AggregationCalcTaskProcessor.java` already used `gm` and was untouched. **Open question #1 is now resolved** (high confidence).
- **Vendored more types locally** in `com.ankur.spark`, per the "vendor locally" decision: `ParquetReader`, `ParquetWriter`, `ParquetStream`, `CommonShadedParquetWriter`, `CommonGenericRecordReader`, `NfsParquetReader`, `NfsParquetWriter`, `OpAdjContext`, `FlattenContext`, `RolloverContext`, `OpAdjRequest`, `FullBookRolloverPredicate`, `OpAdjPredicate`, `EnumCalcEventType`. Stripped the corresponding external `com.db.gm...` imports from whichever of the five processor files referenced them (same pattern established for `Profiler`).
- `ParquetReader` (the interface) didn't exist as its own pasted file, but its full contract was unambiguous from the two implementations (`CommonGenericRecordReader`, `NfsParquetReader`) that both `@Override` the exact same two method signatures — reconstructed with high confidence.
- `ParquetStream implements InputFile` and its `newStream()`/`SeekableByteArrayInputStream` maps cleanly onto real Apache Parquet API (`org.apache.parquet.io.InputFile`, `SeekableInputStream`, `DelegatingSeekableInputStream`) — higher confidence than the internal `com.db.*` guesses since these are real, checkable library classes.
- **Found and fixed a naming bug from the first pass**: `CleanupfsCalcTaskProcessor` used `CLEANUP_GRAIN`, but the real `EnumCalcEventType` enum (now recovered in full) defines `CLEAN_UP_GRAIN` (with the underscore). Fixed the import and call site.
- **Deleted `OpjAdjRequest.java`** — an empty, misspelled stub (`public class OpjAdjRequest {}`) left over from an earlier paste; the real `OpAdjRequest` (correctly spelled, with real fields) was recovered from `OpjAdjContext.java` and written to its own correctly-named file.
- **Reconstructed the full `EnumCalcEventType` enum** (~68 constants) faithfully from the garbled source, converting `WORD WORD` OCR spacing to `WORD_WORD` and fixing a handful of clear typos (`DICTIONARTY`→`DICTIONARY`, `OPNTIMIZED`/`OPHTIMIZED`→`OPTIMIZED`, `ONPREN`→`ONPREM`, `OBPRICE`→`DBPRICE` to match its `FIRST_DBPRICE_COMPLETED`/`FAILED` pair). Only ~12 of these constants are used by the six processor classes we've reconstructed; the rest are real business events transcribed for fidelity. `EventType` (the interface it implements) and `Delegator` (its parent/child helper) remain unresolved — forward references, not yet provided.
- **`RolloverZeroScenarioShift.java` split — done.** Four types recovered: `RolloverZeroScenarioShift` (kept in its own file, trivial no-op `OpAdj`), `EditingOpAdj` (abstract base, the real logic), `TradeFlatten`, `RolloverScenarioShift`. Also vendored `OpAdj` itself (the interface `RolloverZeroScenarioShift implements OpAdj` needed but was never pasted on its own — reconstructed with high confidence from its one used method, `adjust(GenericData.Record) -> Stream<GenericData.Record>`, which also exactly matches the `opAdj::adjust` method reference already used in `OpAdjTaskProcessor`).
  - **Confirmed `OpAdjException` is unchecked** (a `RuntimeException`): `RolloverScenarioShift.edit()` throws it without declaring `throws OpAdjException`, while `TradeFlatten.edit()` and the abstract `EditingOpAdj.edit()` do declare it — both are legal only if it's unchecked, since Java doesn't require override signatures to match on unchecked exceptions. Matches every other assumption made about it so far.
  - **Found the real call site for `ParquetSchemaUtils.unwrapNullableType`** — used twice in `EditingOpAdj`. In the first pass through `OpAdjTaskProcessor.java` I'd dropped this exact static import because no call site survived in that file; it turns out it belonged to a different class entirely. Re-added here as `com.db.ge.portfolio.calculator.result.aggregator.parquet.ParquetSchemaUtils` (note: `ge`, not `gm` — plausibly a genuinely different real subsystem/package, since `result.aggregator` is a different area than `gridlib.pipeline`; not contradicted by any of the `gm` evidence, which was all under `gridlib.pipeline...`).
  - **One genuine gap, not a reconstruction:** `EditingOpAdj.getDoubleFieldName(Schema)` is called but its body never appeared anywhere in the pasted source. Rather than invent plausible-looking business logic with zero evidence, left it as a method that compiles but throws `UnsupportedOperationException("source not recovered")` at runtime. This is the one place in the whole exercise where the gap is filled with an honest stub instead of a reconstruction — flagging clearly so it isn't mistaken for a confident decode like everything else.
  - **Inferred, not evidenced:** `TRADE_ID_COLUMN = "tradeid"` (guessed from `OBJECT_TYPE_COLUMN`'s lowercase-no-separator convention `"objecttype"`), and `NUMERICAL_SCHEMA_SET = {INT, LONG, FLOAT, DOUBLE}` (the set's contents were never shown, only that it's checked via `.contains(valueArrayItemType)` — used the standard Avro numeric primitive types as the most defensible guess).
  - Also confirmed (again) that `8.0d` in the original `RolloverScenarioShift` source was the same `0`↔`8` OCR digit-swap seen throughout this whole exercise — fixed to `0.0d`, consistent with the padding semantics (shift the array left by `scenarioShift`, pad the tail with zero-value entries).

**All six original files plus every type they forward-referenced from this batch of pastes are now vendored.** Remaining unresolved forward references are listed above and haven't shrunk much — each new file tends to introduce as many new undefined types as it resolves.

## `NfsParquetStorage.java` — ✅ vendored, resolves a major forward reference

This file finally gave us the **full `ParquetStorage` interface contract** — every method it forward-referenced by name in five earlier files (`move`, `deleteDirectory`, `createDirectory`, `exists`, `list(String)`, `walk`, `list(String, boolean)`, `delete(Set<String>)`, `readBytes`, `writeBytes`, `copy`) is now backed by a real `@Override`d implementation, so the interface could be reconstructed with high confidence purely from the override list. Vendored three new files: `ParquetStorage` (interface), `ParquetStorageException` (a `RuntimeException` with the three constructor shapes the code actually calls — `(String)`, `(String, Throwable)`, `(Throwable)`), and `NfsParquetStorage` itself. Stripped the external `com.db.gm...ParquetStorage` import from `OpAdjTaskProcessor.java`.

Decisions made in the rewrite:
- Deduplicated the same paste-overlap pattern seen throughout: a second, partial `public class NfsParquetStorage implements ParquetStorage { public List<String> walk(...) {` bled in mid-file (source lines ~175–177) — pure noise, dropped. Also a duplicated `throw new ParquetStorageException("empty or null input");` inside `delete(Set<String>)` — kept one.
- `moveFile`: the source had `if (!Files.exists(target)) { }` with an empty body immediately before the `Files.move(..., REPLACE_EXISTING)` call, positioned so it looked like a guard. Dropped the guard rather than keep it: `REPLACE_EXISTING` as a copy option only makes sense if the move is meant to happen unconditionally (overwriting whatever's there), so gating it behind "target doesn't already exist" would make the flag pointless. Treated the empty if-block as an artifact whose real content didn't survive, not as logic to preserve literally. Flagging this as the one path in this file where I diverged from literal brace-reordering into a judgment call.
- Fixed statement ordering that recurred in nearly every method here: success-path `log.debug(...)` calls kept landing *after* the `catch` block in the garbled source (e.g. `createDirectory`, `moveDirectory`) — moved them back inside the `try`, right after the operation they're confirming, matching the started/finished logging convention used everywhere else in this codebase.
- `delete(Set<String> parquetFiles)`'s catch clause had `throw new ParquetStorageException("Error deleting path at [...]", 0)` — the literal `0` doesn't fit either `ParquetStorageException` constructor; replaced with `e` (the caught exception), consistent with the `(String, Throwable)` constructor used identically in `readBytes`/`writeBytes`.
- **New forward reference introduced, not resolved:** `PathHelper.toUnixPathString(Path)` — a static utility used three times here, left unqualified/unimported like every other not-yet-pasted type. Distinct from `CopyPathHelper` (already vendored-as-external in `CopyCalcTaskProcessor`) — this looks like a separate, more generic path utility.

## Compile verification — ✅ ran `./gradlew compileJava`, 100% clean of self-inflicted errors

First real compile attempt against everything vendored so far. Two genuine (non-forward-reference) bugs found and fixed:

1. **`OpjAdjContext.java` was a stale leftover** — its content had already been fully extracted into `OpAdjContext.java`/`FlattenContext.java`/`RolloverContext.java`/etc., but the original garbled file was never deleted, so the build was compiling both the clean version and the raw OCR mess. Deleted it.
2. **`org.apache.parquet.conf.HadoopParquetConfiguration` doesn't exist in Parquet 1.13.1** (verified by downloading and inspecting the actual jars from Maven Central — empty in both `parquet-common:1.13.1` and `parquet-hadoop:1.13.1`). It's new in **1.14.0**: `ParquetConfiguration`/`PlainParquetConfiguration` live in `parquet-common:1.14.0`, the Hadoop-specific adapter `HadoopParquetConfiguration` lives in `parquet-hadoop:1.14.0`. Bumped `parquet-avro`/`parquet-hadoop`/`parquet-common` from 1.13.1 to 1.14.0 in `build.gradle` — Gradle resolves the higher version everywhere, including for Spark's own Parquet usage, which is fine for a study repo.

`GcsParquetStorage.java` (see below) was also caught up in this compile pass and required adding `com.google.cloud:google-cloud-storage:2.36.1` to `build.gradle`.

After both fixes, temporarily raised `-Xmaxerrs` to see the whole picture at once (reverted after): **141 errors, every single one a "cannot find symbol" or "package does not exist" for a type genuinely not yet pasted.** Zero errors traceable to anything in this reconstruction being wrong. Full list of what's still outstanding, deduplicated:

**Types:** `AggregationContext`, `AggregationLauncherService`, `CalcTask`, `CalculationLocation`, `CleanupContext`, `CompactionContext`, `CompactionLauncherService`, `CompactionRequest`, `CompactionResult`, `CopyContext`, `CopyObject`, `CopyPathHelper`, `CopyRequest`, `CopySurface`, `DbaxException`, `DeduplicatedData`, `Delegator`, `EventType`, `FlattenRequest`, `FunctionContext`, `GcsStorageConfig`, `HdfsConfiguration`, `InMemoryOutputFile`, `MonitorableEvent`, `OpAdjException`, `ProfilerCreator` (+ its 4 implementations), `RawAggregationService`, `ReportingSnapshotsTrade`, `RetryTemplate` (the app's own, not Spring's), `RolloverRequest`, `RunProcessException`, `StorageService`.

**Packages referenced but never backed by a real dependency or vendored file:** all the remaining `com.db.gm.portfolio...` and `com.db.ge.portfolio...` subpackages for the five original `*CalcTaskProcessor` classes' domain models (aggregation, compaction, copying, cleanup, opadj) plus `com.db.gm.portfolio.monitoring` (for `FunctionCompletedEvent`, still external).

This list is the authoritative "what's left" — more reliable than the running tally in each section above, since it's compiler-verified rather than manually tracked.

---

## `GcsParquetReader.java` — ✅ vendored

The GCS-backed sibling of `NfsParquetReader`/`CommonGenericRecordReader` — third implementation of `ParquetReader`, same two-method contract. Correction carried over from the user's own fix to `CommonGenericRecordReader.java`: `HadoopParquetConfiguration` really lives in `org.apache.parquet.conf` (the `parquet-common` module), not `org.apache.parquet.hadoop.util` as I'd originally guessed — used the corrected package here too, consistent with `build.gradle` now depending on `parquet-common`.

Decisions made in the rewrite:
- `readParquet(byte[])`: the source had `fetchRecords(reader, records);` positioned *after* the try-with-resources block's closing brace instead of inside it — moved inside, since `fetchRecords` reads from `reader` and the resource must still be open when that happens (same class of bug as `deleteDirectory`'s misplaced `delete(path)` call in `NfsParquetStorage`, but there it was correctly *after* the try because contents needed deleting first; here it must be *inside* because the reader is actively used, not just cleaned up).
- Added `@RequiredArgsConstructor` — the class has a `final` field (`storage`) but no visible constructor or Lombok annotation survived in the source; every sibling class with an identical single-final-field-and-interface shape (`CommonGenericRecordReader`) uses `@RequiredArgsConstructor`, so treated its absence here as another dropped annotation rather than an intentional omission.
- `normalize(String path)`'s second `replace()` argument was a bare `SEPARATOR` identifier, not a string literal like every other `normalize` method in this codebase (which hardcode `"/"`). Declared `private static final String SEPARATOR = "/";` rather than inlining the literal, since GCS keys are always forward-slash-delimited regardless of host OS — closer to what the original author was likely making explicit by naming it, rather than a stray hardcoded value.

## `NfsToHdfsParquetStorage.java` — ✅ vendored

A second `ParquetStorage` implementation, but a deliberately partial one: only `copy()` and `move()` (real HDFS operations via `org.apache.hadoop.fs.FileSystem`) actually do anything — every other interface method throws `UnsupportedOperationException` with a constant message pointing callers at `NfsParquetStorage` instead. Straightforward once the interface was known from the previous file.

Decisions made in the rewrite:
- Class declaration itself was corrupted (`public class NfsToHdfsParquetStorage implements ParquetStorage;` — semicolon instead of opening brace), and a second, duplicate `public class NfsToHdfsParquetStorage implements ParquetStorage {` bled in mid-file (source line 54, same paste-overlap pattern as every large file so far). Used the first declaration (fixed to a proper brace), dropped the duplicate.
- Renamed the "not implemented" constant from the garbled `NO_IMPLEMENTATION_USER_ALTERNATIVE` to `NO_IMPLEMENTATION_USE_ALTERNATIVE` — the message text itself reads "please use NfsParquetStorage" (verb "use"), so `USER` looks like an OCR insertion, not an intentional name.
- **Left a likely real bug untouched, on purpose:** `move()`'s log message says `"Copying file [{}] to [{}]"` — identical wording to `copy()`'s log line, not "Moving". This isn't OCR corruption (both instances of the word are perfectly legible in the source); it reads like a genuine copy-paste mistake in the original code. Reconstructing means recovering what the source says, not fixing pre-existing bugs in it, so left it exactly as written rather than "correcting" it to say "Moving" — worth flagging if you want it fixed once this is treated as working code rather than a recovery exercise.

**New undefined/forward-referenced types accumulated so far** (not yet pasted by the user): `MonitorableEvent`, `ProfilerCreator` (+ 4 implementations), `PerformancePropertiesReader`, `ParquetStorage`, `CalcTask`, `FunctionContext`, `HdfsConfiguration`, `GcsStorageConfig`, `RetryTemplate` (the app's own, distinct from Spring's), `CalculationLocation`, `FlattenRequest`, `RolloverRequest`, `EventType`, `Delegator`, `InMemoryOutputFile`, `ShadedAvroParquetWriter`, `AVRO_CONF`/`LIST_FIELD_NAME` constants, plus the various `*LauncherService`/`*Service` and `*Result`/`*Request`/`*Context` types used by the five original processors.

---

## What happened to these files

All five files in `com.ankur.spark` show the same corruption signature:
- `import` → `Import`/`Isport`/`isport`/`iort`/`lsport` (OCR misreads of lowercase `im`)
- `0`↔`O`, `1`↔`l`, `rn`↔`m` swaps (`Genericlata` = `GenericData`, `0nPrem` = `OnPrem`)
- IntelliJ CodeVision annotations pasted inline as code: `1 usage & Sergey Uhm +2`, `@Getter(AccessLevel. PACKAGE)`
- Blame-author names embedded mid-statement (`"saurabh sharma +5`, `& Ivanalef`, `& Oleksandr Ragulin`)
- Braces/semicolons dropped or misplaced, multiple statements merged onto one line

This is consistent with a screenshot → OCR pipeline over an IntelliJ editor with inline blame/usage hints enabled, not a real refactor. None of these files exist in git history (all untracked/newly added), so there's no clean prior commit to diff against — reconstruction has to come from cross-referencing the five files against each other and against the (uncorrupted) call patterns they share.

## 7. `Profiler.java` / `ProfilerLogger.java` / `ProfilerFactory.java` — ✅ rewritten, split from one pasted file

The user pasted a new garbled file directly into `com.ankur.spark/Profiler.java`, showing the same OCR corruption signature as everything above, plus one new artifact: **stray isolated-number lines** (`49`, `51`, `52`, ... `80`) scattered through the body — these are IDE line-number gutter digits that got OCR'd in as if they were code tokens. Removed all of them; they carry no semantic content.

The single pasted file actually contained **three separate top-level types concatenated together** (Java only allows one public top-level type per file, so this had to be split):
- `Profiler` (interface, extends `ProfilerLogger`) — nested `Level` enum (`ERROR, WARN, INFO, DEBUG, TRACE`) and a large nested `Component` enum (~39 constants, each carrying a log-filename regex), plus `stop()`/`stopAndReport()`/`report()` methods.
- `ProfilerLogger` (separate interface) — `log(Level, String)` and `isEnabled(Level)`. Split into its own file.
- `ProfilerFactory` (class) — static factory that picks a `ProfilerCreator` implementation based on a configured `DestinationType` (`JMS_QUEUE`, `LOG`, `EVENT_LIST`). Split into its own file, `DestinationType` kept as its nested private enum.

Decisions made in the rewrite:
- Deduplicated `MERCURY_ADAPTER` — appeared twice in the source with an identical pattern (same paste-overlap artifact seen in the earlier files).
- Normalized every regex's digit classes: the source has `[8-9]` scattered in place of what's unambiguously meant to be `[0-9]` (a year/month/day digit class can't sensibly restrict to just 8 or 9) — every instance fixed to `[0-9]`. Also normalized spacing/brace glitches (`{ 4}` → `{4}`, `(3,5)` → `{3,5}`, `(10)` → `{10}`) and a stray trailing hyphen before `)?` that appeared inconsistently across entries.
- Fixed enum constant names: `6DM` → `GDM` (two entries had visually merged onto one OCR line: `FRTB_CONSUMER` and `GDM`, now split back into two constants), `GDM ONPREM` → `GDM_ONPREM`, `JNC` → `JMC` (uncertain — pattern is `jm_server_...`, consistent with either), and a broken `EN` / `(filenamePattern: "event_manager_service_...")` pair rejoined into one `EM(...)` constant.
- `.split(regex: "")` in `getDestinationType()` — the `regex:` is an IntelliJ inline-parameter-name hint (same class of artifact as the `usages`/blame noise), and the actual delimiter string OCR'd out empty. Guessed `","` as the delimiter, since the loop is clearly splitting a configured list of `DestinationType` enum names — flagged as a guess, not evidenced.

**New undefined types this file introduces** (referenced but not yet provided — presumably still to come): `MonitorableEvent` (used in `Profiler`'s method signatures), `ProfilerCreator`, `EventListProfilerCreator`, `JmsProfilerCreator`, `LoggingProfilerCreator`, `CompositeProfilerCreator`, `PerformancePropertiesReader` (all used in `ProfilerFactory`).

**Resolved:** decided to vendor these locally in `com.ankur.spark` as the stand-in for the missing internal library. Removed the `import com.db.ga.portfolio.monitoring.profiler.Profiler;` / `...ProfilerFactory;` lines (and the `com.db.gm...` variants in `AggregationCalcTaskProcessor`, which was never normalized to `ga`) from all five `*CalcTaskProcessor` files — `Profiler`/`ProfilerFactory` now resolve as same-package local types, no import needed. `ProfilerHelper.stopAndReport` (a different, still-unprovided class) is untouched and remains an external `com.db.ga...common.ProfilerHelper` static import in all five files — only `Profiler`/`ProfilerLogger`/`ProfilerFactory` themselves got vendored today.

This sets the pattern going forward: as more pasted files provide real classes (`MonitorableEvent`, `ProfilerCreator` and its implementations, `PerformancePropertiesReader`, `ParquetStorage`, `CalcTask`, etc.), vendor them locally in `com.ankur.spark` too and strip the corresponding `com.db.ga...` import line from whichever of the five processor files references them.

---

Files in scope, in the order we should tackle them (simplest → most tangled):
1. `CalcTaskProcessor.java` — interface, trivial
2. `AggregationCalcTaskProcessor.java`
3. `CopyCalcTaskProcessor.java`
4. `CompactionCalcTaskProcessor.java`
5. `CleanupfsCalcTaskProcessor.java`
6. `OpAdjTaskProcessor.java` — most garbled, has generic bounds on the class itself

---

## 1. `CalcTaskProcessor<T>` (interface)

```java
package com.ankur.spark;

import com.db.gm.portfolio.calculator.gridlib.pipeline.exception.RunProcessException;
import jakarta.xml.bind.JAXBException;   // "jbax.dbax.DbaxException" — best guess, see below

public interface CalcTaskProcessor<T> {
    void processCalcTask(T compactionContext) throws RunProcessException, DbaxException;
}
```

**Uncertain:** `jbax.dbax.DbaxException` doesn't match any real Java package. Likely candidates:
- `javax.xml.bind.JAXBException` (old javax XML binding) — OCR'd `javax` → `jbax`, `JAXBException` → `DbaxException`
- Some internal DB-adjacent exception type (`db.dax.DbaxException`?) given this codebase's `com.db.*` internal package prefix

All five files reference this same exception type identically mangled, so whatever it resolves to, it's consistent. Need the real import to confirm — flagging as open question rather than guessing into the reconstruction.

---

## 2. `AggregationCalcTaskProcessor implements CalcTaskProcessor<AggregationContext>` — ✅ rewritten

`build.gradle` has no `com.db.*` dependency at all (only Spring Boot, Spark, Parquet-Avro), so the internal `gridlib`/`portfolio` packages aren't on this module's classpath and `./gradlew compileJava` can't verify real package/class names — it'll fail on missing dependencies regardless of spelling. Reconstruction below is textual (cross-file consistency + Java conventions), not compiler-verified.

Decisions made in the rewrite, cross-checked against every other file's usage of `com.db.gm...`:
- Dropped the OCR'd `import com.db.gm.portfolio.calculator.gridlib.pipeline.service.deduplicationcompaction.common.CalcTaskProcessor;` — `CalcTaskProcessor` is the local interface already in `com.ankur.spark`, so an import of a same-simple-name type from another package would just conflict. Almost certainly a paste artifact from a different file's import block.
- Retry lambda body reordering: the OCR'd source has an empty `request.getProcessRetryTemplate().execute(retryCallback -> {});` immediately followed by a separately-scoped `request.getTablesToLand().forEach(...)`, then `return null;` appearing *before* `calcTask.setResultBytes(...)` (dead code). Reconstructed as one lambda: `execute(retryCallback -> { forEach(...); log(...); calcTask.setResultBytes(...); return null; })` — matches the working pattern in `CompactionCalcTaskProcessor`.
- Moved `stopAndReport(...)` into a `finally` block so it runs on both success and failure paths (it was floating outside any block in the source).
- Added imports for `AggregationLauncherService` and `AggregationResult` (used but never explicitly imported in the OCR'd source) under the same `...service.aggregation` package as `AggregationContext`/`AggregationRequest` — inferred, not confirmed.
- `RetryContext` resolved to `org.springframework.retry.RetryContext` (real Spring Retry class, consistent with `RetryTemplate` usage) rather than an internal type.

**Still unverified:** the `com.db.gm...` package prefix itself, and whether `AggregationLauncherService`/`AggregationResult` really live directly under `.aggregation` rather than a further subpackage.

---

## 2-orig. Original notes (kept for reference)

Fields: `AggregationLauncherService aggregationLauncherService` (constructor-injected via `@AllArgsConstructor`)

`processCalcTask(AggregationContext aggregationContext)`:
1. Start profiler, pull `AggregationRequest request = aggregationContext.getAggregationRequest()`
2. `grainPaths = request.getGrainPaths()` (map of tableKey → path), joined into `grainPathsSet` for logging
3. `totalBytes = new AtomicLong(0)`, `calcTask = aggregationContext.getCalcTask()`
4. Inside `request.getProcessRetryTemplate().execute(...)`, for each `tableKey` in `request.getTablesToLand()`:
   - `currentParquetPath = grainPaths.get(tableKey)`
   - `targetExecutionIdPath = currentParquetPath.replace(request.getInSurface(), request.getOutSurface())`
   - if `request.getAggregationCriteria().containsKey(tableKey)`: call `aggregationLauncherService.processAggregation(targetExecutionIdPath, currentParquetPath, tableKey)`
   - else: call `aggregationLauncherService.processWithNoAggregation(targetExecutionIdPath, currentParquetPath)`
   - either way accumulate `totalBytes.addAndGet(aggregationResult.getTotalBytes())`
5. After the loop: `calcTask.setResultBytes(totalBytes.get())`
6. `catch Exception e`: log error, throw `AggregationException`
7. `finally`/after try: `stopAndReport(profiler, AGGREGATE_GRAIN, aggregationContext.getCalcTask())`

**Uncertain / needs a decision when rewriting:**
- Original has `return null;` *before* `calcTask.setResultBytes(...)` inside the retry lambda (source line 81–83) — that ordering can't be right (dead code after return). Almost certainly `calcTask.setResultBytes` happens first, then `return null;` closes the lambda.
- `stopAndReport(...)` call sits outside any visible `try/finally` in the OCR'd text — needs to be placed in a `finally` block so it runs even on failure, matching the pattern in `CompactionCalcTaskProcessor` and `CopyCalcTaskProcessor`.

---

## 3. `CopyCalcTaskProcessor implements CalcTaskProcessor<CopyContext>` — ✅ rewritten

Decisions made in the rewrite:
- Normalized the internal package prefix to `com.db.ga...` everywhere (the OCR'd source used five different letters — `ga`, `go`, `gm`, `ge`, `gn` — for what's almost certainly one real package; `ga` was the most frequent in this file). Purely a readability choice pending the real value (open question #1) — a single find/replace fixes it once confirmed.
- Dropped the same bogus `...deduplicationcompaction.common.CalcTaskProcessor` import seen in `AggregationCalcTaskProcessor` — same shadowing problem.
- Deduplicated `getObjectsToBeCopiedFromGcs` — the OCR'd source had two overlapping copies (source lines ~102 and ~115); kept one.
- Moved `@AllArgsConstructor` from mid-class-body (source line 35, syntactically invalid there) to its correct position above the class declaration.
- `gcsStorage` and `pathHelper` made `final` (constructor-injected via `@AllArgsConstructor`); `hdfsStorage` kept non-final with `@Setter`, matching the one clear signal in the source (only `hdfsStorage` had an explicit `@Setter` annotation).
- `@VisibleForTesting` resolved to Guava's `com.google.common.annotations.VisibleForTesting` (the conventional source for that annotation) — Guava isn't in `build.gradle` either, so unverified same as everything else here.
- `processCalcTask` has no visible try/catch around `processGrain(...)` in the OCR'd source (unlike Aggregation/Compaction), so left it that way rather than inventing error handling — if `processGrain` throws, `stopAndReport` won't run. Worth confirming against the real source whether that's intentional.

---

## 3-orig. Original notes (kept for reference)

Fields: `gcsStorage` (`StorageService`, likely constructor-injected — the source shows a stray `@AllArgsConstructor` misplaced *inside* the class body which should be a class-level annotation), `hdfsStorage` (`StorageService`, `@Setter`), `pathHelper` (`CopyPathHelper`).

`processCalcTask(CopyContext context)`:
1. Start profiler, call `processGrain(context)`, `stopAndReport(profiler, COPY_GRAIN_TO_ONPREM, context.getCalcTask())`

`processGrain(CopyContext context)`:
- `vcsExecutionId = context.getRequest().getVcsExecutionId()`, `grain = context.getRequest().getGrain()`
- if `context.getCalculationLocation().isOnPrem()`: log "skipped for OnPrem grain", do nothing
- else: log start, call `processGcpGrain(context)`, log finish

`processGcpGrain(CopyContext context)`:
- `request = context.getRequest()`
- for each `CopySurface surface : request.getSurfaces()`:
  - for each `tableKey : surface.getGrainPaths().keySet()`: call `processGrainTableKey(tableKey, request, surface)`

`processGrainTableKey(tableKey, request, surface)`:
1. `cleanupTargetHdfsDirectory(tableKey, request, surface)`
2. `copyList = getCopyObjectList(tableKey, request, surface)`
3. `copyObjectsToHdfs(copyList)`

`cleanupTargetHdfsDirectory(...)`:
- `hdfsDirectory = pathHelper.getHdfsTargetDirectory(tableKey, request, surface)`
- if `!hdfsDirectory.contains(VCS_EXECUTION_ID_PREFIX)`: throw `ConfigurationException` ("HDFS target directory does not contain VcsExecutionId")
- if `hdfsStorage.exists(hdfsDirectory)`: log, `hdfsStorage.deleteDirectory(hdfsDirectory)`

`getCopyObjectList(...)`: `gcsObjects = getObjectsToBeCopiedFromGcs(tableKey, request, surface)`, maps each to a `CopyObject` via `pathHelper.getCopyObject(...)`, collects to list.

`getObjectsToBeCopiedFromGcs(...)`: `sourceDirectory = pathHelper.getGcsSourceDirectory(tableKey, request, surface)`; `objects = gcsStorage.listFileNames(sourceDirectory, true)`; log count; return.

`copyObjectsToHdfs(List<CopyObject> copyList)`: for each `copyObject`: read bytes via `gcsStorage.readParquetAsBytes(copyObject.getFromPath())`, write via `hdfsStorage.writeParquet(copyObject.getToPath(), parquetObject)`.

**Uncertain:**
- Source has **two near-duplicate copies** of `getObjectsToBeCopiedFromGcs` (lines ~102 and ~115) — an OCR/paste artifact duplicating a scroll region, not two real methods. Keep one.
- `@AllArgsConstructor` appears mid-class (line 35) rather than above the class declaration — needs to move to class level, and then `pathHelper`/`gcsStorage` need to be confirmed `final` (constructor-injected) vs `@Setter`-only like `hdfsStorage`.

---

## 4. `CompactionCalcTaskProcessor implements CalcTaskProcessor<CompactionContext>` — ✅ rewritten

This file had no import block at all in the OCR'd source (it started straight at `@slf4j` with garbage before it) — every import below is inferred by cross-referencing the class/method names actually used in the body against the package-naming pattern established in `AggregationCalcTaskProcessor`/`CopyCalcTaskProcessor` (`...gridlib.pipeline.service.<domain>.context.<Domain>Context` etc.), normalized to the same `com.db.ga...` prefix. Nothing here is more than a structurally-consistent guess — treat every import as unverified.

Decisions made in the rewrite:
- Resolved the scrambled catch/finally: source had `log.error(...); throw e;` (lines 69–71) positioned *before* the `catch (final Exception e) {` keyword that should introduce it, and the "has finished" log (line 75) sitting inside what reads like the catch block. Reconstructed as `try { ... } catch (Exception e) { log.error(...); throw e; } finally { log.info("has finished"); }` — the finally guarantees the completion log fires on both success and failure, matching the started/finished logging pattern used in every other processor in this package.
- Resolved the ambiguous on-prem branch: source shows `if (isOnPrem()) { ):  }` immediately followed by an unconditional-looking `copyGrainToHdfs(...)` call. Mirrored `CopyCalcTaskProcessor`'s established pattern (skip the HDFS copy step when already on-prem) → `if (!isOnPrem()) { copyGrainToHdfs(...); }`.
- `RawAggregationService` guessed to live under `...service.aggregation` (sibling to `AggregationLauncherService`) rather than under `...service.compaction`, since "raw aggregation" is conceptually an aggregation concern reused by the compaction flow — genuinely a guess, not derived from any surviving text.
- `ReportingSnapshotsTrade` guessed to live under `...service.compaction` — no textual evidence either way.

**Biggest gap in this file vs. the others:** with zero surviving import lines, more of this reconstruction rests on pattern-matching than on literal (if garbled) text. Worth treating this file as lowest-confidence until the real source or dependency is available to check against.

---

## 4-orig. Original notes (kept for reference)

Fields (`@AllArgsConstructor`, both `@Getter(AccessLevel.PACKAGE)`): `compactionLauncherService` (`CompactionLauncherService`), `rawAggregationService` (`RawAggregationService`).

`processCalcTask(CompactionContext compactionContext)`:
1. `request = compactionContext.getCompactionRequest()`, `grainPaths = request.getGrainPaths()`, `vcsExecutionId = request.getVcsExecutionId()`, `calcTask = compactionContext.getCalcTask()`
2. Inside `request.getProcessRetryTemplate().execute(...)`:
   - `processRawAggregates(compactionContext)`
   - `deduplicatedData`: if `request.isUseReportingSnapshots()` → `loadSnapshotGrain(calcTask, grainPaths, request.getReportingSnapshotsTrades())`; else → `deduplicateGrain(calcTask, grainPaths, vcsExecutionId, getGrainDeduplicationBy(compactionContext))`
   - `compactionResult = compactGrain(calcTask, request, grainPaths, deduplicatedData)`
   - `calcTask.setResultBytes(compactionResult.getTotalBytes())`
   - if `!compactionContext.getCalculationLocation().isOnPrem()`: `copyGrainToHdfs(calcTask, request, grainPaths, compactionResult.getCompactedParquetPaths())`
   - log completion, `return null;`
3. `catch Exception e`: log error, `throw e;`
4. `finally`-ish: log "process has finished"

Helper methods:
- `processRawAggregates(context)`: if `context.getCompactionRequest().isRawAggregationEnabled()`, call `rawAggregationService.processRawAggregates(context)`
- `copyGrainToHdfs(calcTask, request, grainPaths, compactedParquetPaths)`: profiler around `compactionLauncherService.copyGrainToHdfs(grainPaths, request.getVcsExecutionId(), compactedParquetPaths, request.getInSurface())`, `stopAndReport(..., COPY_GRAIN_TO_HDFS, calcTask)`
- `compactGrain(calcTask, compactionRequest, grainPaths, deduplicatedData)`: profiler around `compactionLauncherService.compactGrain(compactionRequest, grainPaths, deduplicatedData, calcTask.getCalcTaskUID())`, `stopAndReport(..., COMPACT_GRAIN, calcTask)`
- `deduplicateGrain(calcTask, grainPaths, vcsExecutionId, grainDeduplicationBy)`: profiler around `compactionLauncherService.deduplicateGrain(grainPaths, vcsExecutionId, grainDeduplicationBy)`, `stopAndReport(..., DEDUPLICATE_GRAIN, calcTask)`
- `loadSnapshotGrain(calcTask, grainPaths, trades)`: profiler around `compactionLauncherService.loadSnapshotGrain(grainPaths, trades)`, `stopAndReport(..., LOAD_SNAPSHOT_GRAIN, calcTask)`
- `getGrainDeduplicationBy(compactionContext)`: null-safe `compactionContext.getCompactionRequest().getGrainDeduplicationBy()`

**Uncertain — this one's control flow is the most scrambled:**
- `catch (final Exception e)` block (line 73) appears *after* the success-path log statement (line 75 "has finished") in source order — the catch body and the finally-equivalent log are interleaved wrong. The "has finished" log almost certainly belongs in a `finally` block that runs on both paths, and `log.error(...) / throw e;` (lines 69–71) is the actual catch body, currently positioned *before* the `catch` keyword that introduces it.
- Line 57 is a stray `):` — noise, no semantic content.

---

## 5. `CleanupfsCalcTaskProcessor implements CalcTaskProcessor<CleanupContext>` — ✅ rewritten

Like Compaction, this file had no surviving import block, so imports are inferred from the domain-package pattern (`...service.cleanup.context.CleanupContext`/`CleanupRequest`, mirroring `copying`/`compaction`/`aggregation`) and normalized to `com.db.ga...`. `getKeyFromPath` and `VCS_EXECUTION_ID_PATTERN` have zero cross-file corroboration — pure guesses at a `PathUtils`-style static import, flagged lowest-confidence.

Decisions made in the rewrite:
- Deduplicated `collectVcsExecutionIdPaths` — the OCR'd source had the same lambda body pasted twice with different levels of corruption overlapping (source lines 27–76 are two garbled passes over one method); kept the more complete pass and used the other as a cross-check for names (`getKeyFromPath`, `VCS_EXECUTION_ID_PATTERN`).
- Fixed the try-with-resources control flow: original had `catch (IOException e) { }` immediately closed (empty body) with the actual log/return-empty-stream logic sitting *after* it as loose statements — moved that logic into the catch body, since "log and return `Stream.empty()`" is the only sensible outcome for a per-path listing failure (matches the surviving comment "Return an empty stream on error to continue processing other paths").
- Added the missing `int exitCode = process.exitValue();` in `deletePath` — the source referenced `exitCode` in a condition (`if (exitCode 1= 0)`) without ever assigning it.
- Renamed `cleanupNFSForOPGrain` → `cleanupNFSForOnPremGrain` (`OP` = OnPrem, matching the surrounding on-prem/GCP branch naming) and `deleteVcsExecutionIdsFrosFS`/`FrosNFS` → `deleteVcsExecutionIdsFromNFS` for consistency.
- Kept the two genuine explanatory comments from the source (Windows `ProcessBuilder` branch is for JUnit testing; `rm -rf` chosen over NIO walk-and-delete because a full NIO-based cleanup took ~10 hours in load testing) — these are non-obvious rationale, not restating-the-code noise, so they earn their place per repo convention.
- `@AllArgsConstructor` on the class has no fields to bind to (none evident anywhere in the file) — kept for fidelity to the source, but it's a no-op as reconstructed. Possible a field was dropped entirely by OCR; flagging rather than inventing one.

---

## 5-orig. Original notes (kept for reference)

`processCalcTask(CleanupContext cleanupContext)`: profiler around `cleanupNFSForGrain(cleanupContext)`, `stopAndReport(..., CLEANUP_GRAIN, cleanupContext.getCalcTask())`

`cleanupNFSForGrain(CleanupContext cleanupContext)`:
- `vcsExecutionId = cleanupContext.getCleanupRequest().getVcsExecutionId()`, `grain = cleanupContext.getCleanupRequest().getGrain()`
- if `cleanupContext.getCalculationLocation().isOnPrem()`: log start, call `cleanupNFSForOnPremGrain(cleanupContext)`, log finish
- else: log "skipped for GCP"

`cleanupNFSForOnPremGrain(CleanupContext context)`:
- `request = context.getCleanupRequest()`, `grainPaths = request.getGrainPaths()` (`Map<String, Map<String,String>>`), `vcsExecutionId = request.getVcsExecutionId()`, `ioRetry = request.getNfsRequest().getIoRetryTemplate()`
- for each `grainPathsForSurface : grainPaths.values()`:
  - `vcsExecutionIdPaths = collectVcsExecutionIdPaths(grainPathsForSurface, vcsExecutionId)`
  - `deleteVcsExecutionIdsFromNFS(vcsExecutionIdPaths, vcsExecutionId, ioRetry)`

`collectVcsExecutionIdPaths(Map<String,String> paths, String vcsExecutionId)`:
- `vcsExecutionIdTarget = Long.parseLong(vcsExecutionId)`, `pathSet = new HashSet<>(paths.values())`
- for each `pathStr` in `pathSet`, list the directory (`Files.list(Paths.get(pathStr))`, try-with-resources), filter to directories whose name matches `VCS_EXECUTION_ID_PREFIX` and whose extracted id (via `getKeyFromPath(..., VCS_EXECUTION_ID_PATTERN)`) parses to `vcsExecutionIdTarget`
- on `IOException` per path: log and treat as empty (skip that path, don't fail the whole task)
- flatten/collect into one `List<Path>`

`deleteVcsExecutionIdsFromNFS(List<Path> vcsExecutionIdPaths, String vcsExecutionId, RetryTemplate ioRetry)`:
- for each path: `deletePath(path.toString(), ioRetry)`
- `catch Exception e`: log error, throw `CleanupNFSException`

`deletePath(String path, RetryTemplate ioRetry)` (`@SneakyThrows`):
- `builder = createProcessBuilder(path)`
- `ioRetry.execute(retryCallback -> { process = builder.start(); process.waitFor(); if (exitCode != 0) throw new IOException("Failed delete directory: " + path); return null; })`
- `catch IOException e`: throw `CleanupNFSException`

`createProcessBuilder(String path)`:
- Windows: `new ProcessBuilder("cmd", "/c", "rmdir", "/s", "/q", "\"" + path + "\"")` — comment notes this is for testing the flow on Windows via JUnit
- else: `new ProcessBuilder("rm", "-rf", path)` — comment notes `rm -rf` chosen over `java.nio.Files` walk-and-delete for performance; load testing showed the NIO approach took ~10 hours to clean a full COB (close-of-business) run's data

**Uncertain — worst brace/scope damage in the package:**
- `collectVcsExecutionIdPaths` appears **duplicated/interleaved** in the source (two overlapping attempts at the same lambda, lines 27–76) — looks like two OCR passes over the same scrolled region got concatenated. Only one clean version needs to survive.
- Exact placement of the try-with-resources close brace vs. the `catch (IOException e)` vs. the `.collect(...)` chain is ambiguous — the logic (list dir → filter dirs matching this vcsExecutionId → on error return empty stream → flatten) is clear, but exact line-level brace nesting will need to be written fresh, not literally "fixed", when we get to this file.
- `int exitCode` is referenced (line 108) but never assigned from `process.exitValue()` — needs `int exitCode = process.exitValue();` before the check.

---

## 6. `OpAdjTaskProcessor<R extends OpAdjRequest, T extends OpAdjContext<R>> implements CalcTaskProcessor<T>` — ✅ rewritten (all 5 files done)

Also fixed `CalcTaskProcessor.java` (the interface) in the same pass — it was still garbled (`mport` typo, missing closing brace, unresolved `DbaxException`) and blocked this file's `throws` clause from compiling.

Decisions made in the rewrite:
- **Resolved open question #3** (the `OpAdj` class): the garbled import consistently traced to `...service.opadj.adjustment.<ClassName>`, and the field/param spellings (`padj`, `opad`, `opAds`, `Opλdj`) all converge on one plausible real name, `OpAdj`, with the injected field named `opAdj`. Used throughout.
- **Resolved open question #2** (`DbaxException`) with a different, more defensible guess than the placeholder floated earlier: rather than assuming an external `javax.xml.bind.JAXBException`, placed it alongside `RunProcessException` in `...pipeline.exception` — this codebase's own internal exceptions (`ConfigurationException`, `CleanupNFSException`, `RunProcessException`) all live there, so an internal `DbaxException` is the more consistent guess than a JAXB dependency this Spark module has no other trace of. Applied the same fix to `CalcTaskProcessor.java`.
- Class-level generics: reconstructed `<R extends OpAdjRequest, T extends OpAdjContext<R>>` from how `T context` and `R request` are used in the method bodies.
- Reordered the mangled write block: `Schema sourceRecordSchema = records.get(0).getSchema()` now runs *before* opening the try-with-resources writer (source had it inside the try body but the actual `writeParquet(...)` call misplaced into the `catch` block) — moved `writeParquet(...)` into the try body where it belongs, `writeProfiler.stop()` + rethrow stayed in `catch`.
- Outer `catch (OpAdjException e)` / `catch (Exception e)` now wrap the whole per-file loop (not just the last iteration), matching the "delete already-written files in `[]`" log message, which only makes sense if it can fire after multiple files were already written in earlier iterations.
- Dropped the `ParquetSchemaUtils.unwrapNullableType` static import from source line 53 — no call site for it survived anywhere in the reconstructed body, so it's either dead/unused in the original or belongs to a part of the file that's genuinely lost. Left out rather than inventing a use for it.
- `EnumCalcEventType.OP_ADJ_TASK_COMPLETED` referenced via the plain (non-static) `EnumCalcEventType` import, matching how the OCR'd source wrote it (`EnumCalcEventType. OP ADJ TASK COMPLETED`) as opposed to the other three `OP_ADJ_*` constants, which had genuine `import static` lines.

**Still unverified:** `OpAdjException`'s exact package (guessed `...service.opadj.exception`, domain-scoped rather than the shared `...pipeline.exception` used by `ConfigurationException`/`RunProcessException`/`DbaxException`) — no direct textual evidence either way, just a naming-convention guess.

---

## 6-orig. Original notes (kept for reference)

Fields: `storage` (`ParquetStorage`), `parquetReader` (`ParquetReader`), `opAdj` (type is one of the garbled imports — `OpAdj` class from `...service.opadj.adjustment.OpAdj`, referenced in source as `Opλες`/`padj`/`opad` — needs confirming against the real package).

`processCalcTask(T context)`:
1. Start profiler, log start (srcPath, targetPath, vcsExecutionId from `context.getRequest()`)
2. `context.getRetryTemplate().execute(retryCallback -> { processOpAdj(opAdj, request); return null; })`
3. Log completion, `stopAndReport(profiler, OP_ADJ_TASK_COMPLETED, context.getRequest().getCalcTask())`

`processOpAdj(OpAdj opAdj, R request)` — main logic:
1. Guard: if `!storage.exists(request.getSourcePath())` → throw `IllegalStateException("Source path doesn't exist: [...]")`
2. `sourceFiles = storage.list(request.getSourcePath())`; if empty → throw `IllegalStateException("Source path is empty: [...]")`
3. Guard: if `storage.exists(request.getTargetPath()) && !storage.list(request.getTargetPath()).isEmpty()` → throw `IllegalStateException("Target path is not empty: [...]")`
4. `recordFilter = request.getRecordFilter()`
5. For each `filePath` in `sourceFiles`:
   - `fileName = Paths.get(filePath).getFileName().toString()`
   - read profiler; `sourceContent = storage.readBytes(filePath)`; `records = parquetReader.readParquet(sourceContent)` — `catch IOException` → throw `OpAdjException("Cannot read the file [...], the file is empty")`
   - stop read profiler, report `OP_ADJ_READ_SOURCE_COMPLETED`
   - `fullTargetPath = Paths.get(request.getTargetPath(), fileName).toString()`
   - `listFieldName = isRFOSParquetSchema(sourceContent) ? LIST_FIELD_NAME_FOR_RFOS_SCHEMA : null`
   - if `records.isEmpty() && recordFilter instanceof FullBookRolloverPredicate`: just copy source bytes straight to target (`storage.writeBytes(fullTargetPath, sourceContent)`) — comment: full-book rollover has no filter by trade id, source is already empty, so copy as-is
   - else (apply adjustment):
     - `targetRecords = records.stream().filter(recordFilter).flatMap(opAdj::adjust).collect(toList())`
     - stop adjustment profiler, report `OP_ADJ_ADJUSTMENT_COMPLETED`
     - if `CollectionUtils.isNotEmpty(targetRecords)`: write via try-with-resources `CommonShadedParquetWriter(fullTargetPath, listFieldName, storage)`, using `sourceRecordSchema = records.get(0).getSchema()`, `parquetWriter.writeParquet(targetRecords, sourceRecordSchema)`; stop write profiler, report `OP_ADJ_WRITE_DESTINATION_COMPLETED`
     - else: log error "no records to adjust"
6. After the file loop: `missingTradeIds = recordFilter.missingTradeIds()`; if not empty: log warn; if `recordFilter.hasError()`: throw `OpAdjException("All trade ids specified for operational adjustment are missing: [...]")`
7. `catch OpAdjException e`: log error "during rollover, deleting already written files"; if `storage.exists(request.getTargetPath())`: `storage.deleteDirectory(request.getTargetPath())`; rethrow
8. `catch Exception e`: log error, throw `new OpAdjException("Rollover function failed", e)`

`isRFOSParquetSchema(byte[] sourceContent)` (static):
- `parquetSchema = parquetSchema(new ParquetStream(sourceContent))` (RFOS = the schema's own naming, comment says "sourceContent may contain no records, therefore analyzing the parquet schema" directly rather than the data)
- `type = parquetSchema.getType(VALUE_COLUMN)`
- `return type.asGroupType().containsField(LIST_FIELD_NAME_FOR_RFOS_SCHEMA)`

**Uncertain — this file needs the most decisions before rewriting:**
- The class-level generic bound is badly mangled (`extends OpAdjRequest, T extends OpAdjContext<R>>`) — reconstructed as `<R extends OpAdjRequest, T extends OpAdjContext<R>>` based on how `T` and `R` are used in method bodies (`T context`, `R request`), matching the pattern of every other processor being generic over a `*Context`.
- `opAdj` field type: three different garbled spellings appear (`Opλες` in the import, `padj`/`opad` in the field declaration and constructor param) — needs the real class name from the actual `...service.opadj.adjustment.*` package before this compiles. This is the single biggest unresolved unknown in the whole package.
- The try/catch nesting inside the per-file loop (step 5) is badly scrambled in the OCR'd source — the `catch (IOException e)` for the read appears to swallow into the *next* statement (`readProfiler.stopAndReport(...)`) rather than closing before it. The logical intent above is clear from the profiler/report pattern used elsewhere in this file and in the sibling classes; the literal brace placement will need to be rewritten, not patched.
- Two outer catches (`OpAdjException` then generic `Exception`) both need to wrap the entire per-file loop, not just the last file — matches the "delete already-written files on failure" comment, which only makes sense if it can fire after multiple files have already been written.

---

## Shared conventions across all five files (useful once we start rewriting)

- Every `processCalcTask` wraps its core work with `Profiler profiler = ProfilerFactory.createAndStart()` / `stopAndReport(profiler, <EnumCalcEventType constant>, calcTask)`.
- Every implementation logs an explicit "started" / "finished" pair around the main method using `{}`-style SLF4J placeholders, always including `vcsExecutionId` and usually `grain` or `grainPath`.
- Retryable outer work goes through a `*RetryTemplate().execute(retryCallback -> { ...; return null; })` lambda — this is `org.springframework.retry` or an internal equivalent, consistent across Aggregation/Compaction/OpAdj.
- `@AllArgsConstructor` + `lombok.extern.slf4j.Slf4j` (`log`) are the standard Lombok annotations on every impl; static `log`/`Log` casing in the OCR text is just corruption, not two different loggers.
- Static import `ProfilerHelper.stopAndReport` and `EnumCalcEventType.*` constants are used identically across files — confirms these live in shared internal packages (`com.db.g*.portfolio.calculator.gridlib.pipeline.service.common` / `com.db.g*.portfolio.monitoring.metric`), and the `g*` segment (`gm`, `ga`, `go`, `gn`, `gs`, `gu`) is itself probably one single consistent package segment that OCR randomized letter-by-letter — worth checking a `build.gradle`/dependency listing for the real value.

## Open questions before any rewrite is "done"

1. Real value of the `com.db.g?.portfolio...` package segment (single letter varies wildly across files — `gm`, `ga`, `go`, `gn`, `gs`, `gu`, `gl`, `gc`, `gg`).
2. Real type/import for `DbaxException` (interface's second checked exception).
3. Real class name for `OpAdj` (the adjustment engine `OpAdjTaskProcessor` delegates to).
4. Whether `gcsStorage`/`pathHelper` in `CopyCalcTaskProcessor` are constructor-injected (`final`) or field-injected — affects whether `@AllArgsConstructor` at class level is even correct once moved.
