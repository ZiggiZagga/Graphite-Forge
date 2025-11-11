package com.example.graphql;

/**
 * Exception thrown when a database operation fails.
 */
public class ItemDatabaseException extends ItemException {
    public ItemDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ItemDatabaseException(String message) {
        super(message);
    }
}
