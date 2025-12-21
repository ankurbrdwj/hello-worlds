package com.ankur.price_alert.exception;

/**
 * Exception thrown when a user is not found.
 */
public class UserNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "USER_NOT_FOUND";

    public UserNotFoundException(Long userId) {
        super("User", userId, ERROR_CODE);
    }

    public UserNotFoundException(String email) {
        super("User", email, ERROR_CODE);
    }
}
