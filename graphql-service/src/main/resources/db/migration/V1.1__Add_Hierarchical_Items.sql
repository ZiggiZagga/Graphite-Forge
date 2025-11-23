-- V1.1__Add_Hierarchical_Items.sql
-- Adds parent-child relationships and hierarchy support

-- Add parent_id column
ALTER TABLE items ADD COLUMN parent_id VARCHAR(36);

-- Add foreign key constraint for parent relationship
ALTER TABLE items ADD CONSTRAINT fk_items_parent_id 
    FOREIGN KEY (parent_id) REFERENCES items(id) ON DELETE CASCADE;

-- Create index on parent_id for faster hierarchy queries
CREATE INDEX IF NOT EXISTS idx_items_parent_id ON items(parent_id);

-- Create composite index for finding children of a parent
CREATE INDEX IF NOT EXISTS idx_items_parent_id_created_at ON items(parent_id, created_at DESC);

-- Add comments
COMMENT ON COLUMN items.parent_id IS 'Optional reference to parent item for hierarchical organization';

-- Add constraint to prevent self-referencing (parent_id cannot be same as id)
ALTER TABLE items ADD CONSTRAINT items_no_self_parent CHECK (parent_id IS NULL OR parent_id != id);
