package com.ankur.url.shortner.dto;

import jakarta.persistence.Column;
import lombok.Data;

import java.time.Instant;
@Data
public class ShortenedUrlReponse {

    private final String originalUrl;
    private final String shortCode;
    private final String customAlias;
    private final Instant expiresAt;
    private final Instant createdAt;

}
