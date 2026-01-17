package com.example.graphql.ironbucket;

import java.util.List;

/**
 * Format for exporting audit logs.
 */
public record AuditLogExportFormat(
    String format,
    boolean includeHeaders,
    List<String> fields
) {
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private String format = "JSON";
        private boolean includeHeaders = true;
        private List<String> fields;
        
        public Builder format(String format) {
            this.format = format;
            return this;
        }
        
        public Builder includeHeaders(boolean includeHeaders) {
            this.includeHeaders = includeHeaders;
            return this;
        }
        
        public Builder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }
        
        public AuditLogExportFormat build() {
            return new AuditLogExportFormat(format, includeHeaders, fields);
        }
    }
}

