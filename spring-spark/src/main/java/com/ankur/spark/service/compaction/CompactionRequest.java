package com.ankur.spark.service.compaction;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;
import java.util.Map;

@Getter
@Builder
@ToString
public class CompactionRequest {

    private Map<String, String> grainPaths;
    private String vcsExecutionId;
    private String grain;
    private String inSurface;
    private String outSurface;
    private RetryTemplate processRetryTemplate;
    private boolean useReportingSnapshots;
    private List<ReportingSnapshotsTrade> reportingSnapshotsTrades;
    private String grainDeduplicationBy;
    private boolean rawAggregationEnabled;
}
