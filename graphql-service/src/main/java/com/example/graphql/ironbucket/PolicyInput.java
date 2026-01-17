package com.example.graphql.ironbucket;

import java.util.List;

/**
 * Input for creating or updating a policy.
 * 
 * @param tenant Tenant ID
 * @param roles List of roles
 * @param buckets List of bucket patterns
 * @param tags List of tag patterns
 * @param operations List of operations
 */
public record PolicyInput(
    String tenant,
    List<String> roles,
    List<String> buckets,
    List<String> tags,
    List<String> operations
) {
    public PolicyInput {
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("Tenant cannot be null or blank");
        }
        if (roles == null || roles.isEmpty()) {
            throw new IllegalArgumentException("Roles cannot be null or empty");
        }
    }
}
