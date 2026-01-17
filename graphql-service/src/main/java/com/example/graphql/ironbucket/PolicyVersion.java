package com.example.graphql.ironbucket;

import java.time.Instant;

/**
 * Represents a version of a policy.
 */
public record PolicyVersion(
    String policyId,
    int version,
    Instant createdAt,
    String createdBy,
    String changeDescription
) {
    public PolicyVersion {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be null or blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1");
        }
    }
}
