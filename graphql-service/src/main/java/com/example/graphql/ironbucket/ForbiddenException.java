package com.example.graphql.ironbucket;

/**
 * Thrown when a user attempts an operation they don't have permission for.
 */
public class ForbiddenException extends RuntimeException {
    
    public ForbiddenException(String message) {
        super(message);
    }
    
    public ForbiddenException(String message, Throwable cause) {
        super(message, cause);
    }
}
