-- V1.0__Initial_Schema.sql
-- Initial database schema for Graphite-Forge GraphQL Service

-- Items table
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(36) PRIMARY KEY DEFAULT RANDOM_UUID(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT items_name_check CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT items_description_check CHECK (LENGTH(description) <= 2000 OR description IS NULL)
);

-- Create index on name for faster lookups
CREATE INDEX IF NOT EXISTS idx_items_name ON items(name);

-- Create index on created_at for sorting
CREATE INDEX IF NOT EXISTS idx_items_created_at ON items(created_at DESC);

-- Add comment
COMMENT ON TABLE items IS 'Items managed through GraphQL API';
COMMENT ON COLUMN items.id IS 'Unique identifier (UUID)';
COMMENT ON COLUMN items.name IS 'Item name, required and non-blank';
COMMENT ON COLUMN items.description IS 'Optional item description (max 2000 chars)';
COMMENT ON COLUMN items.created_at IS 'Timestamp when item was created';
COMMENT ON COLUMN items.updated_at IS 'Timestamp when item was last updated';
