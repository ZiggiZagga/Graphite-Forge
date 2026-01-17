package com.example.graphql.ironbucket;

import java.time.Duration;

/**
 * Policy for audit log retention.
 */
public record AuditRetentionPolicy(
    Duration retentionPeriod,
    boolean archiveEnabled,
    String archiveLocation
) {
    public AuditRetentionPolicy {
        if (retentionPeriod == null) {
            throw new IllegalArgumentException("Retention period cannot be null");
        }
    }
    
    public long retentionDays() {
        return retentionPeriod.toDays();
    }
    
    public static AuditRetentionPolicy days(int days) {
        return new AuditRetentionPolicy(Duration.ofDays(days), false, null);
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private Duration retentionPeriod = Duration.ofDays(90);
        private boolean archiveEnabled = false;
        private String archiveLocation;
        
        public Builder retentionDays(int days) {
            this.retentionPeriod = Duration.ofDays(days);
            return this;
        }
        
        public Builder retentionPeriod(Duration period) {
            this.retentionPeriod = period;
            return this;
        }
        
        public Builder archiveEnabled(boolean enabled) {
            this.archiveEnabled = enabled;
            return this;
        }
        
        public Builder archiveLocation(String location) {
            this.archiveLocation = location;
            return this;
        }
        
        public AuditRetentionPolicy build() {
            return new AuditRetentionPolicy(retentionPeriod, archiveEnabled, archiveLocation);
        }
    }
}

