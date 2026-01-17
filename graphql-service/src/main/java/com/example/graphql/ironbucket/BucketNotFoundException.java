package com.example.graphql.ironbucket;

/**
 * Thrown when attempting to access a bucket that does not exist.
 */
public class BucketNotFoundException extends RuntimeException {
    
    public BucketNotFoundException(String bucketName) {
        super("Bucket not found: " + bucketName);
    }
    
    public BucketNotFoundException(String bucketName, Throwable cause) {
        super("Bucket not found: " + bucketName, cause);
    }
}
