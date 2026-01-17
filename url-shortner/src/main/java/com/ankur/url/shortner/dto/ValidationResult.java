package com.ankur.url.shortner.dto;

import java.util.Optional;

public class ValidationResult {
    private final boolean valid;
    private final Optional<String> errorMessage;

    private ValidationResult(boolean valid, Optional<String> errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static ValidationResult valid() {
        return new ValidationResult(true, Optional.empty());
    }

    public static ValidationResult invalid(String message) {
        return new ValidationResult(false, Optional.of(message));
    }

    public boolean isValid() { return valid; }
    public Optional<String> getErrorMessage() { return errorMessage; }
}
