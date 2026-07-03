package com.example.auth.exception;

/**
 * Thrown when an invalid role is provided.
 */
public class InvalidRoleException extends RuntimeException {

    public InvalidRoleException(String message) {
        super(message);
    }
}
