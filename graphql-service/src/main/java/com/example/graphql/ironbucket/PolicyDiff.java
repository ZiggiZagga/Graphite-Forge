package com.example.graphql.ironbucket;

import java.util.List;

/**
 * Difference between two policy versions.
 */
public record PolicyDiff(
    String policyId,
    int fromVersion,
    int toVersion,
    List<String> addedRoles,
    List<String> removedRoles,
    List<String> addedBuckets,
    List<String> removedBuckets,
    List<String> addedOperations,
    List<String> removedOperations
) {
    public PolicyDiff {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be null or blank");
        }
    }
    
    // Getter alias for compatibility
    public List<String> changedBuckets() {
        return this.addedBuckets.isEmpty() && this.removedBuckets.isEmpty() ? 
            List.of() : 
            (this.addedBuckets.isEmpty() ? this.removedBuckets : this.addedBuckets);
    }
}

