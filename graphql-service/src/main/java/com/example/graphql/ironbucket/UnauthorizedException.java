package com.example.graphql.ironbucket;

/**
 * Thrown when a user is not authorized to perform an operation.
 */
public class UnauthorizedException extends RuntimeException {
    
    public UnauthorizedException(String message) {
        super(message);
    }
    
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
