package com.example.graphql.ironbucket;

/**
 * Thrown when attempting to create a bucket that already exists.
 */
public class BucketAlreadyExistsException extends RuntimeException {
    
    public BucketAlreadyExistsException(String bucketName) {
        super("Bucket already exists: " + bucketName);
    }
    
    public BucketAlreadyExistsException(String bucketName, Throwable cause) {
        super("Bucket already exists: " + bucketName, cause);
    }
}
