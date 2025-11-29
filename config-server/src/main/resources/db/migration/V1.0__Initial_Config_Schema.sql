-- V1.0__Initial_Config_Schema.sql
-- Initial schema for Spring Cloud Config Server backed by Graphite-Forge

CREATE TABLE IF NOT EXISTS config_items (
    id VARCHAR(36) PRIMARY KEY,
    key VARCHAR(255) NOT NULL,
    value TEXT NOT NULL,
    environment VARCHAR(50) NOT NULL,
    parent_id VARCHAR(36),
    enabled BOOLEAN NOT NULL DEFAULT true,
    description VARCHAR(1000),
    is_encrypted BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT config_items_no_self_parent CHECK (parent_id IS NULL OR parent_id != id),
    CONSTRAINT config_items_valid_env CHECK (environment IN ('dev', 'staging', 'production', 'test', 'qa')),
    
    -- Foreign key for hierarchy
    CONSTRAINT fk_config_items_parent_id 
        FOREIGN KEY (parent_id) REFERENCES config_items(id) ON DELETE CASCADE
);

-- Indices for optimal query performance
CREATE INDEX idx_config_items_environment ON config_items(environment);
CREATE INDEX idx_config_items_parent_id ON config_items(parent_id);
CREATE INDEX idx_config_items_key_environment ON config_items(key, environment);
CREATE INDEX idx_config_items_enabled ON config_items(enabled);
CREATE INDEX idx_config_items_is_encrypted ON config_items(is_encrypted);
CREATE INDEX idx_config_items_created_at ON config_items(created_at DESC);

-- Index for hierarchy queries
CREATE INDEX idx_config_items_env_parent ON config_items(environment, parent_id);

-- Unique constraint for key per environment and parent (to prevent duplicates)
CREATE UNIQUE INDEX uidx_config_items_key_env_parent 
    ON config_items(key, environment, COALESCE(parent_id, ''));
