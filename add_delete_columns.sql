-- SQL script to add missing columns for delete functionality
-- Run this in your Supabase SQL Editor

-- Add is_active column if it doesn't exist
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS is_active BOOLEAN DEFAULT TRUE;

-- Add deleted_at column if it doesn't exist (optional, for future use)
ALTER TABLE items 
ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP WITH TIME ZONE;

-- Update existing items to be active by default
UPDATE items 
SET is_active = TRUE 
WHERE is_active IS NULL;

-- Add an index for better performance on filtering active items
CREATE INDEX IF NOT EXISTS idx_items_is_active ON items(is_active);
CREATE INDEX IF NOT EXISTS idx_items_owner_active ON items(owner_id, is_active);

-- Verify the columns were added
\d items;
