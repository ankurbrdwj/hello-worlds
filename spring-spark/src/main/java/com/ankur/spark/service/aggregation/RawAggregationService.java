package com.ankur.spark.service.aggregation;

import com.ankur.spark.service.compaction.CompactionContext;

public interface RawAggregationService {

    void processRawAggregates(CompactionContext compactionContext);
}
