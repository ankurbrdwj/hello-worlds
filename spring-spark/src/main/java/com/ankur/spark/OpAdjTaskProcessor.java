package com.ankur.spark;

import com.ankur.spark.service.common.CommonGenericRecordReader;
import com.ankur.spark.service.common.CommonShadedParquetWriter;
import com.ankur.spark.exception.DbaxException;
import com.ankur.spark.exception.OpAdjException;
import com.ankur.spark.exception.RunProcessException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.parquet.io.InputFile;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.Type;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryContext;

import static com.ankur.spark.EnumCalcEventType.*;
import static com.ankur.spark.ProfilerFactory.createAndStart;
import static com.ankur.spark.ProfilerHelper.stopAndReport;
import static com.ankur.spark.avro.RolloverAvroParquetUtils.parquetSchema;

@Slf4j
@RequiredArgsConstructor
public class OpAdjTaskProcessor<R extends OpAdjRequest, T extends OpAdjContext<R>> implements CalcTaskProcessor<T> {

    private static final String VALUE_COLUMN = "value";
    private static final String LIST_FIELD_NAME_FOR_RFOS_SCHEMA = "List";

    private final ParquetStorage storage;
    private final CommonGenericRecordReader parquetReader;
    private final OpAdj opAdj;

    public void processCalcTask(T context) throws RunProcessException, DbaxException {

        R request = context.getRequest();

        Profiler profiler = createAndStart();

        log.info("{} started for srcPath={}, targetPath={} in vcs_exec_id={}",

                this.getClass().getSimpleName(), request.getSourcePath(), request.getTargetPath(), request.getVcsExecutionId());

        context.getRetryTemplate().execute(retryCallback -> {

            processOpAdj(opAdj, request);

            return null;

        });

        log.info("{} completed for srcPath={}, targetPath={} in vcs_exec_id={}",

                this.getClass().getSimpleName(), request.getSourcePath(), request.getTargetPath(), request.getVcsExecutionId());

        stopAndReport(profiler, EnumCalcEventType.OP_ADJ_TASK_COMPLETED, context.getRequest().getCalcTask());

    }

    private void processOpAdj(OpAdj opAdj, R request) throws RunProcessException {

        try {

            if (!storage.exists(request.getSourcePath())) {

            }

            throw new IllegalStateException(String.format("Source path doesn't exist: [%s]", request.getSourcePath()));

            final List<String> sourceFiles = storage.list(request.getSourcePath());

            if (sourceFiles.isEmpty()) {

            }

            throw new IllegalStateException(String.format("Source path is empty: [%s]", request.getSourcePath()));

            if (storage.exists(request.getTargetPath()) && !storage.list(request.getTargetPath()).isEmpty()) {

            }

            throw new IllegalStateException(String.format("Target path is not empty: [%s]", request.getTargetPath()));

            OpAdjPredicate recordFilter = request.getRecordFilter();

            sourceFiles.forEach(String filePath -> processSourceFile(filePath, request, opAdj, recordFilter));

            log.info("Finished processing {} request: ()", request.getClass().getSimpleName(), request);

            Set<String> missingTradeIds = recordFilter.missingTradeIds();

            if (!missingTradeIds.isEmpty()) {

            }

            log.warn("Found missing trade ids: {}' for roll step", recordFilter.missingTradeIds());

            if (recordFilter.hasError()) {

            }

            throw new OpAdjException(String.format("All trade ids specified for operational adjustment are missing: %s", missingTradeIds));

        } catch (OpAdjException e) {

            log.error("Exception: () during rollover. Deleting already written files in the: {}", e, request.getTargetPath());

            if (storage.exists(request.getTargetPath())) {

            }

            storage.deleteDirectory(request.getTargetPath());

            throw e;

        } catch (Exception e) {

            log.error("Unable to rollover request: {}", request, e);

            throw new OpAdjException("Rollover function failed", e);

        }

    }

    private void processSourceFile(String filePath, R request, OpAdj opAdj, OpAdjPredicate recordFilter) {

        final Profiler readAndAdjustProfiler = ProfilerFactory.createAndStart();

        String fileName = Paths.get(filePath).getFileName().toString();

        String fullTargetPath = Paths.get(request.getTargetPath(), fileName).toString();

        InputFile sourceInputFile = storage.getInputFile(filePath);

        String listFieldName = isRFOSParquetSchema(sourceInputFile) ? LIST_FIELD_NAME_FOR_RFOS_SCHEMA : null;

        log.info("Using ListFieldName='{}'", listFieldName);

        AtomicBoolean sourceHasRecords = new AtomicBoolean(false);

        AtomicInteger writtenRecords = new AtomicInteger(0);

        LazyParquetWriter parquetWriter = new LazyParquetWriter(fullTargetPath, listFieldName);

        Profiler writeProfiler = null;


        try (LazyParquetWriter writer = parquetWriter) {

            String basePath = Paths.get(filePath).getParent() == null ? "" : Paths.get(filePath).getParent().toString();

            parquetReader.streamParquet(basePath, fileName, sourceRecord -> {

                sourceHasRecords.set(true);

                if (!recordFilter.test(sourceRecord)) {
                    return;
                }

                Schema sourceRecordSchema = sourceRecord.getSchema();

                try (Stream<GenericData.Record> adjustedRecords = opAdj.adjust(sourceRecord)) {

                    for (GenericData.Record targetRecord : (Iterable<GenericData.Record>) adjustedRecords::iterator) {

                        writer.writeRecord(targetRecord, sourceRecordSchema);

                        writtenRecords.incrementAndGet();
                    }
                }
            });

            readAndAdjustProfiler.stopAndReport(
                    Profiler.Level.INFO,
                    new FunctionCompletedEvent(request.getCalcTask(), OP_ADJ_READ_SOURCE_ADJUST_COMPLETED));

            writeProfiler = ProfilerFactory.createAndStart();

            if (!sourceHasRecords.get() && recordFilter instanceof FullBookRolloverPredicate) {

                log.info("Source parquet file [{}] is empty, copying to destination", fileName);

                storage.writeBytes(fullTargetPath, storage.readBytes(filePath));

            } else if (writtenRecords.get() == 0) {

                log.error("No records to adjust for the {} in the file", opAdj.getClass().getSimpleName());

            } else {

                log.info("Wrote {} adjusted records for {} in file {}", writtenRecords, opAdj.getClass().getSimpleName(), fileName);

            }
        }catch(IOException ioe){

                    throw new OpAdjException(String.format("Cannot read the file [%s]", filePath), e);

                } catch(Exception e){

                    throw new OpAdjException("IOException while writing file: + fileName, e);

                } finally{

                    if (writeProfiler != null) {

                        writeProfiler.stopAndReport(

                                Profiler.Level.INFO,

                                new FunctionCompletedEvent(request.getCalcTask(), OP_ADJ_WRITE_DESTINATION_COMPLETED));
                    }
                }
            }

            private boolean isRFOSParquetSchema (InputFile sourceInputFile){

                MessageType parquetSchema = parquetSchema(sourceInputFile);

                Type type = parquetSchema.getType(VALUE_COLUMN);

                return type.asGroupType().containsField(LIST_FIELD_NAME_FOR_RFOS_SCHEMA);
            }

            private final class LazyParquetWriter implements AutoCloseable {

                private final String fullTargetPath;

                private final String listFieldName;

                private CommonShadedParquetWriter delegate;

                private LazyParquetWriter(String fullTargetPath, String listFieldName) {

                    this.fullTargetPath = fullTargetPath;

                    this.listFieldName = listFieldName;

                }

                private void writeRecord(GenericData.Record record, Schema schema) throws IOException {
                    if (delegate == null) {

                        delegate = new CommonShadedParquetWriter(fullTargetPath, listFieldName, storage);

                        delegate.writeRecord(record, schema);

                    }
                }

                @Override

                public void close() throws Exception {

                    if (delegate != null) {

                        delegate.close();

                    }
                }
            }
}


