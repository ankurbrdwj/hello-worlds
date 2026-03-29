package com.ankur.bdd.exception;

public class UserNotFoundException extends ResourceNotFoundException {

    public static final String ERROR_CODE = "USER_NOT_FOUND";

    public UserNotFoundException(Long userId) {
        super("User", userId, ERROR_CODE);
    }

    public UserNotFoundException(String email) {
        super("User", email, ERROR_CODE);
    }
}