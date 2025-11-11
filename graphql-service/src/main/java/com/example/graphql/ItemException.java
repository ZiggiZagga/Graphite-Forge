package com.example.graphql;

/**
 * Base exception for item-related errors.
 */
public abstract class ItemException extends RuntimeException {
    public ItemException(String message) {
        super(message);
    }

    public ItemException(String message, Throwable cause) {
        super(message, cause);
    }
}
