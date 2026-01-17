package com.example.graphql.ironbucket;

import java.time.Instant;
import java.util.List;

/**
 * Filter for querying audit logs.
 */
public record AuditLogFilter(
    List<String> users,
    List<String> actions,
    List<String> buckets,
    List<String> results,
    Instant startDate,
    Instant endDate
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<String> users;
        private List<String> actions;
        private List<String> buckets;
        private List<String> results;
        private Instant startDate;
        private Instant endDate;
        
        public Builder users(List<String> users) {
            this.users = users;
            return this;
        }
        
        public Builder user(String user) {
            this.users = List.of(user);
            return this;
        }
        
        public Builder actions(List<String> actions) {
            this.actions = actions;
            return this;
        }
        
        public Builder action(String action) {
            this.actions = List.of(action);
            return this;
        }
        
        public Builder buckets(List<String> buckets) {
            this.buckets = buckets;
            return this;
        }
        
        public Builder bucket(String bucket) {
            this.buckets = List.of(bucket);
            return this;
        }
        
        public Builder results(List<String> results) {
            this.results = results;
            return this;
        }
        
        public Builder result(String result) {
            this.results = List.of(result);
            return this;
        }
        
        public Builder startDate(Instant startDate) {
            this.startDate = startDate;
            return this;
        }
        
        public Builder endDate(Instant endDate) {
            this.endDate = endDate;
            return this;
        }
        
        public AuditLogFilter build() {
            return new AuditLogFilter(users, actions, buckets, results, startDate, endDate);
        }
    }
}
