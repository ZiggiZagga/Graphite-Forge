package com.example.graphql.ironbucket;

import java.time.Instant;

/**
 * Represents an S3-compatible bucket in IronBucket storage.
 * 
 * @param name Bucket name (must be DNS-compatible, lowercase)
 * @param creationDate When the bucket was created
 * @param ownerTenant Tenant ID that owns this bucket
 */
public record S3Bucket(
    String name,
    Instant creationDate,
    String ownerTenant
) {
    public S3Bucket {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be null or blank");
        }
        if (!isValidBucketName(name)) {
            throw new IllegalArgumentException("Invalid bucket name: " + name);
        }
        if (ownerTenant == null || ownerTenant.isBlank()) {
            throw new IllegalArgumentException("Owner tenant cannot be null or blank");
        }
    }

    /**
     * Validates bucket name according to S3 naming rules:
     * - Lowercase letters, numbers, hyphens, dots
     * - 3-63 characters
     * - Must start/end with letter or number
     */
    private static boolean isValidBucketName(String name) {
        if (name.length() < 3 || name.length() > 63) {
            return false;
        }
        return name.matches("^[a-z0-9][a-z0-9.-]*[a-z0-9]$");
    }
}
