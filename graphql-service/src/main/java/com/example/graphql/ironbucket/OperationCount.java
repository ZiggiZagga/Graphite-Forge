package com.example.graphql.ironbucket;

/**
 * Count of operations by type.
 */
public record OperationCount(
    String operation,
    long count
) {
    public OperationCount {
        if (operation == null || operation.isBlank()) {
            throw new IllegalArgumentException("Operation cannot be null or blank");
        }
    }
}
