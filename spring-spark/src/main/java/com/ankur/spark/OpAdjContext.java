package com.ankur.spark;

import java.util.*;

import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.retry.support.RetryTemplate;

@Getter
@SuperBuilder
@ToString
public abstract class OpAdjContext<T extends OpAdjRequest> implements FunctionContext {

    private T request;
    private HdfsConfiguration hdfsConfig;
    private GcsStorageConfig gcsConfig;
    private RetryTemplate retryTemplate;

    public boolean isOffPrem() {
        return gcsConfig != null;
    }

    @Override
    public void cleanup() {
        if (hdfsConfig != null) {
            hdfsConfig.cleanup();
        }
    }
}
