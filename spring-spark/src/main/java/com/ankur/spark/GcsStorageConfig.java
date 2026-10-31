package com.ankur.spark;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.retry.support.RetryTemplate;

@Getter

@Builder

@ToString

public class GcsStorageConfig {

    private String keyPath;

    @NonNull

    private String projectId;

    @NonNull

    private Integer connectionTimeout;

    @NonNull

    private Integer readTimeout;

    private RetryTemplate gcsRetryTemplate;

// Wif CPV Cache settings

    private String cpvcTokenName;

    private String adfCacheUrl;

    private String adfCacheEnv;

    @Builder.Default


    private boolean wifEnabled = false;

    public boolean hasAccessTokenProviderConfiguration() {


        return StringUtils.isNotEmpty(projectId) && StringUtils.isNotEmpty(cpvcTokenName) && StringUtils.isNotEmpty(adfCacheUrl) && StringUtils.isNotEmpty(adfCacheEnv);


    }
}

