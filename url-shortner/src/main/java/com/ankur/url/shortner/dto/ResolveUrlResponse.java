package com.ankur.url.shortner.dto;

import lombok.Data;

@Data
public class ResolveUrlResponse {
    private final String originalUrl;
    private final String shortCode;
}
