package com.ankur.spark.avro;

import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GenericDataParquetOperationsTest {

    private static final Schema SCHEMA = SchemaBuilder.record("WordCount").fields()
            .requiredString("word")
            .requiredLong("count")
            .endRecord();

    private final ParquetRecordOperations ops = new GenericDataParquetOperations();

    @TempDir
    Path dir;
    Path source;

    @BeforeEach
    void seed() throws Exception {
        source = dir.resolve("source.parquet");
        ops.writeAll(source, SCHEMA, List.of(rec("spark", 5), rec("avro", 2), rec("parquet", 9)));
    }

    @Test
    void roundTripsRecordsAndSchema() throws Exception {
        assertThat(ops.readAll(source)).hasSize(3);
        assertThat(ops.readSchema(source).getFields()).extracting(Schema.Field::name)
                .containsExactly("word", "count");
    }

    @Test
    void adjustRewritesEveryRecordIntoNewFile() throws Exception {
        Path target = dir.resolve("doubled.parquet");

        int n = ops.adjust(source, target, r -> ops.withField(r, "count", (Long) r.get("count") * 2));

        assertThat(n).isEqualTo(3);
        assertThat(ops.readAll(target)).extracting(r -> r.get("count")).containsExactly(10L, 4L, 18L);
        // source file untouched
        assertThat(ops.readAll(source)).extracting(r -> r.get("count")).containsExactly(5L, 2L, 9L);
    }

    @Test
    void setWhereOnlyTouchesMatchingRecords() throws Exception {
        Path target = dir.resolve("zeroed.parquet");

        ops.setWhere(source, target, r -> (Long) r.get("count") < 5, "count", 0L);

        assertThat(ops.readAll(target)).extracting(r -> r.get("count")).containsExactly(5L, 0L, 9L);
    }

    @Test
    void filterAndSort() throws Exception {
        var all = ops.readAll(source);

        assertThat(ops.filter(all, r -> (Long) r.get("count") >= 5)).hasSize(2);
        assertThat(ops.sortBy(all, "count", false)).extracting(r -> r.get("word").toString())
                .containsExactly("parquet", "spark", "avro");
    }

    @Test
    void withFieldDoesNotMutateInput() {
        GenericRecord original = rec("spark", 5);

        GenericRecord changed = ops.withField(original, "count", 6L);

        assertThat(original.get("count")).isEqualTo(5L);
        assertThat(changed.get("count")).isEqualTo(6L);
    }

    @Test
    void findInvalidFlagsWrongTypes() {
        GenericRecord bad = rec("x", 1);
        bad.put("count", "not-a-long");

        assertThat(ops.findInvalid(SCHEMA, List.of(rec("ok", 1), bad))).containsExactly(bad);
    }

    private static GenericRecord rec(String word, long count) {
        GenericRecord r = new GenericData.Record(SCHEMA);
        r.put("word", word);
        r.put("count", count);
        return r;
    }
}
