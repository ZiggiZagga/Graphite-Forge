package com.example.graphql.ironbucket;

import java.time.Instant;

/**
 * Result of committing policies to Git.
 */
public record GitCommitResult(
    boolean success,
    String commitHash,
    String branch,
    Instant commitTime,
    String message,
    int filesCount
) {
    public GitCommitResult {
        if (commitTime == null) {
            throw new IllegalArgumentException("Commit time cannot be null");
        }
    }
    
    // Getter aliases for compatibility
    public String commitSha() {
        return this.commitHash;
    }
    
    public int filesChanged() {
        return this.filesCount;
    }
}


