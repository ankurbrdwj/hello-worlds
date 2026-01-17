package com.ankur.url.shortner.dto;

import lombok.Data;

import java.time.Instant;
import java.util.Optional;

@Data
public final class CreateShortUrlRequest {
    private final String originalUrl;
    private final Optional<String> customAlias;
    private final Optional<Instant> expiresAt;

}
