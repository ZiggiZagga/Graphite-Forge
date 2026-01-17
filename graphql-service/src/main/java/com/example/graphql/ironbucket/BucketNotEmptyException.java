package com.example.graphql.ironbucket;

/**
 * Thrown when attempting to delete a bucket that still contains objects.
 */
public class BucketNotEmptyException extends RuntimeException {
    
    public BucketNotEmptyException(String bucketName) {
        super("Bucket is not empty: " + bucketName);
    }
    
    public BucketNotEmptyException(String bucketName, Throwable cause) {
        super("Bucket is not empty: " + bucketName, cause);
    }
}
