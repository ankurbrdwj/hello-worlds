package com.ankur.price_alert.result;

/**
 * UserError — a sealed type that enumerates every way a user operation can fail.
 *
 * Sealed = the compiler knows ALL possible subtypes at compile time.
 * This means a switch on UserError can be exhaustive — no default branch needed.
 *
 * Each variant is a record, so it carries exactly the data needed to describe
 * that specific failure. This is far more structured than a generic exception
 * with a free-form message string.
 *
 * Pattern:
 *   switch (error) {
 *       case UserError.NotFound      e -> 404
 *       case UserError.DuplicateEmail e -> 409
 *       case UserError.QuotaExceeded  e -> 422
 *       case UserError.ValidationError e -> 400
 *       case UserError.InactiveAccount e -> 403
 *   }
 *   // ← compiler verifies this is exhaustive; no runtime surprises
 */
public sealed interface UserError
        permits UserError.NotFound,
                UserError.DuplicateEmail,
                UserError.QuotaExceeded,
                UserError.ValidationError,
                UserError.InactiveAccount {

    /** Expose a human-readable message without a common base class. */
    String message();

    // ─── Variants ─────────────────────────────────────────────────────────────

    /**
     * Fired when a lookup by id or email finds nothing.
     * identifier: whatever was used to search (id as string, email, etc.)
     */
    record NotFound(String identifier) implements UserError {
        @Override
        public String message() {
            return "User not found: " + identifier;
        }
    }

    /**
     * Fired on CREATE when the email is already taken.
     */
    record DuplicateEmail(String email) implements UserError {
        @Override
        public String message() {
            return "Email already registered: " + email;
        }
    }

    /**
     * Fired when a user tries to create an alert but has hit their limit.
     * Carries current + max so the caller can render a meaningful message.
     */
    record QuotaExceeded(Long userId, int current, int max) implements UserError {
        @Override
        public String message() {
            return "Alert quota exceeded for user %d: %d/%d used".formatted(userId, current, max);
        }
    }

    /**
     * Fired when a request field fails business validation (not just null checks).
     * field: the offending field name; reason: why it failed.
     */
    record ValidationError(String field, String reason) implements UserError {
        @Override
        public String message() {
            return "Validation failed on '%s': %s".formatted(field, reason);
        }
    }

    /**
     * Fired when an operation requires an active account but the user is deactivated.
     */
    record InactiveAccount(Long userId) implements UserError {
        @Override
        public String message() {
            return "Account is inactive: userId=" + userId;
        }
    }
}