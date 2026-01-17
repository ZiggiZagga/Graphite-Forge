package com.example.graphql.ironbucket;

/**
 * Statistics summary for audit logs.
 */
public record AuditStatistics(
    long totalOperations,
    long successfulOperations,
    long failedOperations,
    long uniqueUsers
) {
    public AuditStatistics {
        if (totalOperations < 0 || successfulOperations < 0 || failedOperations < 0 || uniqueUsers < 0) {
            throw new IllegalArgumentException("Statistics values cannot be negative");
        }
    }
    
    // Explicit getter aliases for compatibility
    public long totalOperations() { return this.totalOperations; }
    public long successfulOperations() { return this.successfulOperations; }
    public long failedOperations() { return this.failedOperations; }
    public long uniqueUsers() { return this.uniqueUsers; }
}

