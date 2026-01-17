package com.example.graphql.ironbucket;

import java.time.Instant;
import java.util.Map;

/**
 * Represents an S3-compatible object in IronBucket storage.
 * 
 * @param key Object key (path within bucket)
 * @param bucketName Bucket containing this object
 * @param size Size in bytes
 * @param lastModified When the object was last modified
 * @param contentType MIME type
 * @param metadata Custom metadata key-value pairs
 */
public record S3Object(
    String key,
    String bucketName,
    Long size,
    Instant lastModified,
    String contentType,
    Map<String, String> metadata
) {
    public S3Object {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Object key cannot be null or blank");
        }
        if (bucketName == null || bucketName.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or blank");
        }
        if (size != null && size < 0) {
            throw new IllegalArgumentException("Object size cannot be negative");
        }
    }
    
    // Alias for tests
    public String bucket() {
        return bucketName;
    }
}
