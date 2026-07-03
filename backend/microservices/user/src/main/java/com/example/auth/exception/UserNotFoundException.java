package com.example.auth.exception;

/**
 * Thrown when a user cannot be found by the given identifier.
 */
public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(String message) {
        super(message);
    }
}
