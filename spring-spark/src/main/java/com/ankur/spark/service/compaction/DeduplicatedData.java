package com.ankur.spark.service.compaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.Collection;
import java.util.Map;

/** Per table key, the deduplicated parquet paths ready for compaction. */
@Getter
@ToString
@AllArgsConstructor
public class DeduplicatedData {

    private final Map<String, Collection<String>> parquetPathsByTableKey;
}
