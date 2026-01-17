package com.example.graphql.ironbucket;

/**
 * Thrown when an S3 service operation fails.
 */
public class S3ServiceException extends RuntimeException {
    
    public S3ServiceException(String message) {
        super(message);
    }
    
    public S3ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
