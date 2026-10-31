package com.ankur.spark.avro;

import java.util.*;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Operations for adjusting the contents of a Parquet file as Avro {@link GenericRecord}s.
 * Records are treated as immutable: every operation returns new records and leaves its input untouched.
 */
public interface ParquetRecordOperations {

    /** Avro schema derived from the Parquet file footer. */
    Schema readSchema(Path parquetFile) throws IOException;

    List<GenericRecord> readAll(Path parquetFile) throws IOException;

    /** Writes {@code records} to a new file; fails if {@code parquetFile} already exists. */
    void writeAll(Path parquetFile, Schema schema, List<GenericRecord> records) throws IOException;

    /** Keeps only the records matching {@code predicate}. */
    List<GenericRecord> filter(List<GenericRecord> records, Predicate<GenericRecord> predicate);

    /** Returns a copy of {@code record} with {@code field} set to {@code value}. */
    GenericRecord withField(GenericRecord record, String field, Object value);

    /** Sorts by the natural Avro order of {@code field}. */
    List<GenericRecord> sortBy(List<GenericRecord> records, String field, boolean ascending);

    /** Checks each record against {@code schema}; returns the records that do not conform. */
    List<GenericRecord> findInvalid(Schema schema, List<GenericRecord> records);

    /**
     * Read {@code source}, apply {@code adjustment} to each record, and write the result to {@code target}.
     * {@code adjustment} receives a copy, so it may mutate it in place.
     */
    default int adjust(Path source, Path target, UnaryOperator<GenericRecord> adjustment) throws IOException {
        Schema schema = readSchema(source);
        List<GenericRecord> adjusted = readAll(source).stream().map(adjustment).toList();
        writeAll(target, schema, adjusted);
        return adjusted.size();
    }

    /** Convenience: rewrite one field on every record matching {@code predicate}. */
    default int setWhere(Path source, Path target, Predicate<GenericRecord> predicate, String field, Object value)
            throws IOException {
        return adjust(source, target,
                r -> predicate.test(r) ? withField(r, field, value) : r);
    }

    static Comparator<GenericRecord> byField(String field, boolean ascending) {
        Comparator<GenericRecord> c = (a, b) ->
                org.apache.avro.generic.GenericData.get()
                        .compare(a.get(field), b.get(field), a.getSchema().getField(field).schema());
        return ascending ? c : c.reversed();
    }
}
