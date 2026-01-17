package com.example.graphql.ironbucket;

import java.util.List;

/**
 * Represents a policy rule in IronBucket's ABAC/RBAC system.
 * 
 * @param id Unique policy identifier
 * @param tenant Tenant ID this policy applies to
 * @param roles List of roles this policy applies to
 * @param buckets List of bucket patterns (supports wildcards)
 * @param tags List of object tags to match
 * @param operations List of allowed S3 operations (e.g., s3:GetObject, s3:PutObject)
 * @param version Policy version number
 * @param deleted Whether this policy is marked as deleted
 */
public record PolicyRule(
    String id,
    String tenant,
    List<String> roles,
    List<String> buckets,
    List<String> tags,
    List<String> operations,
    int version,
    boolean deleted
) {
    public PolicyRule {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Policy ID cannot be null or blank");
        }
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("Tenant cannot be null or blank");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Roles cannot be null or empty");
        }
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("Operations cannot be null or empty");
        }
    }
    
    // Aliases for tests
    public List<String> allowedBuckets() {
        return buckets;
    }
    
    public boolean isDefault() {
        return id != null && id.startsWith("default-");
    }
}

