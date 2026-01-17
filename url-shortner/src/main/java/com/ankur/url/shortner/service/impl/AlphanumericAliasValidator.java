package com.ankur.url.shortner.service.impl;

import com.ankur.url.shortner.dto.ValidationResult;
import com.ankur.url.shortner.service.AliasValidator;
import org.springframework.stereotype.Service;

@Service
public class AlphanumericAliasValidator implements AliasValidator {
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 20;

    @Override
    public ValidationResult validate(String alias) {
        if (alias == null || alias.isBlank()) {
            return ValidationResult.invalid("Alias cannot be empty");
        }
        if (alias.length() < MIN_LENGTH) {
            return ValidationResult.invalid("Alias must be at least " + MIN_LENGTH + " characters");
        }
        if (alias.length() > MAX_LENGTH) {
            return ValidationResult.invalid("Alias cannot exceed " + MAX_LENGTH + " characters");
        }
        if (!alias.matches("^[a-zA-Z0-9-_]+$")) {
            return ValidationResult.invalid("Alias can only contain letters, numbers, hyphens, and underscores");
        }
        return ValidationResult.valid();
    }
}