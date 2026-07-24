package com.example.auth.exception;

/**
 * Thrown when attempting to register with a CIN that already exists.
 */
public class CinAlreadyExistsException extends RuntimeException {

    public CinAlreadyExistsException(String message) {
        super(message);
    }
}
