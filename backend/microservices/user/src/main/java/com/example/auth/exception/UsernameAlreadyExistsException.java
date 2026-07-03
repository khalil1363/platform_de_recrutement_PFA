package com.example.auth.exception;

/**
 * Thrown when attempting to register with a username that already exists.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
