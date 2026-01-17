package com.example.graphql.ironbucket;

import java.time.Instant;

/**
 * Result of archiving audit logs.
 */
public record ArchiveResult(
    boolean success,
    long archivedCount,
    String archiveLocation,
    Instant archiveTime
) {
    public ArchiveResult {
        if (archiveTime == null) {
            throw new IllegalArgumentException("Archive time cannot be null");
        }
    }
}
