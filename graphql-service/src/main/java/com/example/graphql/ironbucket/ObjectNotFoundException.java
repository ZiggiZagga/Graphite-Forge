package com.example.graphql.ironbucket;

/**
 * Thrown when attempting to access an object that does not exist.
 */
public class ObjectNotFoundException extends RuntimeException {
    
    public ObjectNotFoundException(String bucketName, String objectKey) {
        super(String.format("Object not found: %s/%s", bucketName, objectKey));
    }
    
    public ObjectNotFoundException(String bucketName, String objectKey, Throwable cause) {
        super(String.format("Object not found: %s/%s", bucketName, objectKey), cause);
    }
}
