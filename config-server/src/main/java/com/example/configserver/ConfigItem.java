package com.example.configserver;

import jakarta.validation.constraints.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * ConfigItem represents a configuration entry in the config server.
 * 
 * Supports hierarchical organization:
 * - Root level: Applications
 * - Second level: Environments (dev, staging, prod)
 * - Third level: Configuration keys (server.port, timeout, etc.)
 * 
 * Sensitive values are encrypted at rest and can be decrypted on retrieval.
 */
@Table("config_items")
public record ConfigItem(
    @Id
    String id,
    
    @NotBlank(message = "Configuration key cannot be blank")
    @Size(min = 1, max = 255, message = "Configuration key must be between 1 and 255 characters")
    String key,
    
    @NotNull(message = "Configuration value cannot be null")
    @Size(max = 65535, message = "Configuration value cannot exceed 65535 characters")
    String value,
    
    @NotBlank(message = "Environment cannot be blank")
    @Pattern(regexp = "^(dev|staging|production|test|qa)$", 
             message = "Environment must be one of: dev, staging, production, test, qa")
    String environment,
    
    @Column("parent_id")
    String parentId,
    
    @NotNull(message = "Enabled flag cannot be null")
    Boolean enabled,
    
    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    String description,
    
    @Column("is_encrypted")
    Boolean isEncrypted,
    
    @Column("created_at")
    LocalDateTime createdAt,
    
    @Column("updated_at")
    LocalDateTime updatedAt
) {
    
    /**
     * Constructor for creating a new config item with minimal fields.
     * Automatically sets defaults for optional fields.
     */
    public ConfigItem(String id, String key, String value, String environment) {
        this(id, key, value, environment, null, true, null, false, LocalDateTime.now(), LocalDateTime.now());
    }
    
    /**
     * Constructor with encryption support and parent hierarchy.
     */
    public ConfigItem(String id, String key, String value, String environment, 
                     String parentId, Boolean enabled, String description, 
                     Boolean isEncrypted) {
        this(id, key, value, environment, parentId, enabled, description, 
             isEncrypted, LocalDateTime.now(), LocalDateTime.now());
    }
    
    /**
     * Check if this is a root-level config item (no parent).
     */
    public boolean isRootItem() {
        return parentId == null || parentId.isBlank();
    }
    
    /**
     * Check if this config value requires encryption (secrets, passwords, etc).
     */
    public boolean shouldBeEncrypted() {
        return key.toLowerCase().contains("password") ||
               key.toLowerCase().contains("secret") ||
               key.toLowerCase().contains("token") ||
               key.toLowerCase().contains("apikey") ||
               key.toLowerCase().contains("api_key");
    }
}
