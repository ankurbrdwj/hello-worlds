package com.ankur.spark.service.aggregation;

import java.util.*;

import com.ankur.spark.AggregationFunction;
import com.google.common.annotations.VisibleForTesting;
import com.ankur.spark.service.common.InMemoryOutputFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.commons.lang3.StringUtils;
import org.apache.parquet.hadoop.ParquetWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ankur.spark.avro.ParquetSchemaUtils.getFieldOrNull;
import static com.ankur.spark.service.aggregation.AggregationConstants.AVRO_CONF;
import static com.ankur.spark.service.aggregation.AggregationCriteriaResolver.buildAggregationCriteriaMap;

@Slf4j
public final class ParquetAggregationService {

    private final Map<String, AggregationMetadata> tablesMetadata;
    private final AggregationFunctionProvider functionProvider;

    public static ParquetAggregationService fromAggregationCriteria(Map<String, String> aggregationCriteriaMap) {
        AggregationMetadataFactory metadataFactory = new AggregationMetadataFactory();

        Map<String, AggregationMetadata> tablesMetadata = new HashMap<>();
        for (Map.Entry<String, String> entry : aggregationCriteriaMap.entrySet()) {
            tablesMetadata.put(entry.getKey(), metadataFactory.create(entry.getValue()));
        }

        return new ParquetAggregationService(tablesMetadata, new AggregationFunctionProvider());
    }

    public static ParquetAggregationService fromConfiguration(String aggregationCriteria, Map<String, String> functionProperties) {
        return fromAggregationCriteria(buildAggregationCriteriaMap(aggregationCriteria, functionProperties));
    }

    private ParquetAggregationService(Map<String, AggregationMetadata> tablesMetadata,
                                      AggregationFunctionProvider functionProvider) {
        this.tablesMetadata = tablesMetadata;
        this.functionProvider = functionProvider;
    }

    public boolean hasAggregation(String tableName) {
        return tablesMetadata.get(tableName) != null;
    }

    public Collector<Map.Entry<String, GenericData.Record>, ?, Map<String, GenericData.Record>> mergeMapCollector(String tableName) {
        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (baseRecord, patchRecord) -> mergeRecords(tableName, baseRecord, patchRecord));
    }

    public Map<String, GenericData.Record> mergeAggregation(String tableName,
                                                            Map<String, GenericData.Record> newRecordsMap,
                                                            Map<String, GenericData.Record> aggregatedRecordsMap) {

        return Stream.of(newRecordsMap, aggregatedRecordsMap)
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (baseRecord, patchRecord) -> mergeRecords(tableName, baseRecord, patchRecord)));

    }

    public GenericData.Record mergeRecords(String tableName, GenericData.Record baseRecord, GenericData.Record patchRecord) {
        AggregationMetadata aggregationMetadata = tablesMetadata.get(tableName);

        GenericData.Record returnRecord = baseRecord;
        for (AggregationFunctionType functionType : aggregationMetadata.getFunctionTypes()) {
            AggregationFunction function = functionProvider.getFunction(functionType);
            List<String> functionColumns = aggregationMetadata.getFunctionColumns().get(functionType);
            returnRecord = function.apply(functionColumns, baseRecord, patchRecord);
        }

        return returnRecord;
    }

    public String makeGroupByKey(String tableName, GenericData.Record record) {
        List<String> groupByColumns = tablesMetadata.get(tableName).getGroupByColumns();
        StringBuilder keyBuilder = new StringBuilder();
        groupByColumns.forEach(columnName -> keyBuilder.append(columnName)
                .append('=')
                .append(getAggColValue(record, columnName))
                .append(',')
        );
        return keyBuilder.toString();
    }

    private String getAggColValue(GenericData.Record agRecord, String columnName) {
        Object s = getFieldOrNull(agRecord, columnName);
        return s == null ? StringUtils.EMPTY : s.toString();
    }

    public GenericData.Record emptyNonCriteriaColumns(String tableName, GenericData.Record record) {
        Schema schema = record.getSchema();
        List<String> allColumns = tablesMetadata.get(tableName).getAllColumns();

        for (Schema.Field field : schema.getFields()) {
            String fieldName = field.name();
            if (!allColumns.contains(fieldName)) {
                record.put(fieldName, StringUtils.EMPTY);
            }
        }
        return record;
    }

    public Map<String, GenericData.Record> aggregate(String tableKey, Iterable<GenericData.Record> recordIterable) {
        return aggregate(tableKey, () -> new CollectionGenericDataReader("GenericDataReader", recordIterable));
    }

    public Map<String, GenericData.Record> aggregate(String tableKey, GenericDataReaderSupplier supplier) {
        Map<String, GenericData.Record> aggregatedMap = new HashMap<>();
        try (GenericDataReader dataReader = supplier.get()) {
            log.info("Reading records from [{}]", dataReader.getName());

            GenericData.Record dataRecord = dataReader.read();
            while (dataRecord != null) {
                GenericData.Record emptyColsRecord = emptyNonCriteriaColumns(tableKey, dataRecord);
                String key = makeGroupByKey(tableKey, emptyColsRecord);
                if (aggregatedMap.containsKey(key)) {
                    mergeRecords(tableKey, aggregatedMap.get(key), emptyColsRecord);
                } else {
                    aggregatedMap.put(key, emptyColsRecord);
                }

                dataRecord = dataReader.read();
            }

        } catch (IOException e) {
            throw new AggregationException("Error aggregating records", e);
        }
        return aggregatedMap;
    }

    public byte[] getParquetData(Iterable<GenericData.Record> records) {
        return getParquetData(records, new DefaultParquetWriterHelper(AVRO_CONF, records));
    }

    @VisibleForTesting
    public byte[] getParquetData(Iterable<GenericData.Record> records, ParquetWriterHelper parquetWriterFactory) {

        if (records == null) {
            return null; // NOSONAR
        }

        Iterator<GenericData.Record> iterator = records.iterator();
        if (!iterator.hasNext()) {
            return null; // NOSONAR
        }

        InMemoryOutputFile outputFile = parquetWriterFactory.createOutputFile();
        try (ParquetWriter<GenericData.Record> writer = parquetWriterFactory.createParquetWriter(outputFile)) {
            while (iterator.hasNext()) {
                writer.write(iterator.next());
            }
        } catch (Exception e) {
            throw new AggregationException("Can't create parquet for aggregation!", e);
        }

        return outputFile.toArray();
    }
}





