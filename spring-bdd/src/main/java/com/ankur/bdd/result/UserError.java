package com.ankur.bdd.result;

public sealed interface UserError
        permits UserError.NotFound,
                UserError.DuplicateEmail,
                UserError.QuotaExceeded,
                UserError.ValidationError,
                UserError.InactiveAccount {

    String message();

    record NotFound(String identifier) implements UserError {
        @Override public String message() { return "User not found: " + identifier; }
    }

    record DuplicateEmail(String email) implements UserError {
        @Override public String message() { return "Email already registered: " + email; }
    }

    record QuotaExceeded(Long userId, int current, int max) implements UserError {
        @Override public String message() {
            return "Alert quota exceeded for user %d: %d/%d used".formatted(userId, current, max);
        }
    }

    record ValidationError(String field, String reason) implements UserError {
        @Override public String message() {
            return "Validation failed on '%s': %s".formatted(field, reason);
        }
    }

    record InactiveAccount(Long userId) implements UserError {
        @Override public String message() { return "Account is inactive: userId=" + userId; }
    }
}