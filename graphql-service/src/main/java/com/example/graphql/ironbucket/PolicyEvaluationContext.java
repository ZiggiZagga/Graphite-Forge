package com.example.graphql.ironbucket;

import java.util.List;
import java.util.Map;

/**
 * Context for evaluating a policy against a request.
 */
public record PolicyEvaluationContext(
    String tenant,
    List<String> roles,
    String operation,
    String resource
) {
    public PolicyEvaluationContext {
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalArgumentException("Tenant cannot be null or blank");
        }
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("Operation cannot be null or blank");
        }
    }
}
