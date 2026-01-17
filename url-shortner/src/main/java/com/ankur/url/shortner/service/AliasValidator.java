package com.ankur.url.shortner.service;

import com.ankur.url.shortner.dto.ValidationResult;

public interface AliasValidator {
    ValidationResult validate(String alias);
}
