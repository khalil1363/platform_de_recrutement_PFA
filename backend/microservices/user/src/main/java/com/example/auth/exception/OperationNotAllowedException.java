package com.example.auth.exception;

/**
 * Thrown when an operation is not allowed for the current user.
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
