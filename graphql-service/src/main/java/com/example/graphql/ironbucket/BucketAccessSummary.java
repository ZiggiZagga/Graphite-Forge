package com.example.graphql.ironbucket;

/**
 * Summary of bucket access from audit logs.
 */
public record BucketAccessSummary(
    String bucket,
    long accessCount,
    long uniqueUsers
) {
    public BucketAccessSummary {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket cannot be null or blank");
        }
    }
}
