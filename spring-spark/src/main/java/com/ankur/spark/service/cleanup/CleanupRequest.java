package com.ankur.spark.service.cleanup;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.retry.support.RetryTemplate;

import java.util.Map;

@Getter
@Builder
@ToString
public class CleanupRequest {

    private String vcsExecutionId;
    private String grain;
    // surface -> (table key -> path)
    private Map<String, Map<String, String>> grainPaths;
    private NfsRequest nfsRequest;

    @Getter
    @Builder
    public static class NfsRequest {
        private RetryTemplate ioRetryTemplate;
    }
}
