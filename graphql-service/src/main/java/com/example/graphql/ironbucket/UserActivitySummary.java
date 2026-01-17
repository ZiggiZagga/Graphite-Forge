package com.example.graphql.ironbucket;

/**
 * Summary of user activity from audit logs.
 */
public record UserActivitySummary(
    String user,
    long totalOperations,
    long successfulOperations,
    long failedOperations
) {
    public UserActivitySummary {
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("User cannot be null or blank");
        }
    }
}
