package com.example.graphql.ironbucket;

/**
 * General exception for IronBucket service errors.
 */
public class IronBucketServiceException extends RuntimeException {
    
    public IronBucketServiceException(String message) {
        super(message);
    }
    
    public IronBucketServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
