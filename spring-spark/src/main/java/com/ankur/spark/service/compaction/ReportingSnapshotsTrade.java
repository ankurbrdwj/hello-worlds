package com.ankur.spark.service.compaction;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@AllArgsConstructor
public class ReportingSnapshotsTrade {

    private final String tradeId;
    private final String snapshotPath;
}
