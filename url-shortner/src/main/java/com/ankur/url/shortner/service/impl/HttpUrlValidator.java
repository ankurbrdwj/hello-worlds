package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.dto.ValidationResult;
import com.ankur.url.shortner.service.UrlValidator;
import org.springframework.stereotype.Service;

@Service
public class HttpUrlValidator implements UrlValidator {
    private static final int MAX_URL_LENGTH = 2048;

    @Override
    public ValidationResult validate(String url) {
        if (url == null || url.isBlank()) {
            return ValidationResult.invalid("URL cannot be empty");
        }
        if (url.length() > MAX_URL_LENGTH) {
            return ValidationResult.invalid("URL exceeds maximum length");
        }
        if (!url.matches("^https?://.*")) {
            return ValidationResult.invalid("URL must start with http:// or https://");
        }
        return ValidationResult.valid();
    }
}