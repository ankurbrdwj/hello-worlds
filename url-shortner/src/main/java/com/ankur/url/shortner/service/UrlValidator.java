package com.ankur.url.shortner.service;

import com.ankur.url.shortner.dto.ValidationResult;

public interface UrlValidator {
    ValidationResult validate(String url);
}
