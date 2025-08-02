-- Add is_active column to user_notifications table
-- Run this in Supabase SQL Editor

-- Add the is_active column with default value true
ALTER TABLE public.user_notifications 
ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT true;

-- Add an index for better performance when filtering by is_active
CREATE INDEX IF NOT EXISTS idx_user_notifications_is_active 
ON public.user_notifications(is_active);

-- Update the composite index to include is_active
DROP INDEX IF EXISTS idx_user_notifications_user_read_created;
CREATE INDEX IF NOT EXISTS idx_user_notifications_user_read_active_created 
ON public.user_notifications(user_id, is_read, is_active, created_at DESC);

-- Add comment to document the new column
COMMENT ON COLUMN public.user_notifications.is_active 
IS 'Whether the notification is active (false when related post is deleted)';

-- Verify the column was added
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'user_notifications' 
AND table_schema = 'public';
