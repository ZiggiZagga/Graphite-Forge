package com.example.graphql.ironbucket;

import java.time.Instant;

/**
 * Represents an audit log entry for IronBucket operations.
 * 
 * @param id Unique audit log ID
 * @param timestamp When the operation occurred
 * @param user User who performed the operation
 * @param action Action performed (e.g., s3:GetObject)
 * @param bucket Bucket involved
 * @param objectKey Object key involved (nullable)
 * @param result Result of the operation (SUCCESS, DENIED, ERROR)
 * @param ipAddress Source IP address
 */
public record AuditLogEntry(
    String id,
    Instant timestamp,
    String user,
    String action,
    String bucket,
    String objectKey,
    String result,
    String ipAddress
) {
    public AuditLogEntry {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Audit log ID cannot be null or blank");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("Timestamp cannot be null");
        }
        if (user == null || user.isBlank()) {
            throw new IllegalArgumentException("User cannot be null or blank");
        }
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("Action cannot be null or blank");
        }
    }
}
