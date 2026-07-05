package com.example.auth.exception;

/**
 * Thrown when an uploaded file is invalid.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
