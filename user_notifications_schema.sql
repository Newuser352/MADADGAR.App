-- Complete schema for user_notifications table
-- Run this in Supabase SQL Editor

-- Drop existing table if you want to recreate it
-- DROP TABLE IF EXISTS public.user_notifications;

-- Create the user_notifications table with all necessary columns
CREATE TABLE IF NOT EXISTS public.user_notifications (
    -- Primary key
    id BIGSERIAL PRIMARY KEY,
    
    -- User who will receive this notification
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    
    -- Notification type (new_listing, post_deleted, system_alert, etc.)
    type VARCHAR(50) NOT NULL,
    
    -- Notification title (shown in notification list)
    title VARCHAR(255) NOT NULL,
    
    -- Notification body/message
    body TEXT NOT NULL,
    
    -- JSON payload with additional data (item_id, uploader_id, etc.)
    payload JSONB,
    
    -- Whether the notification has been read
    is_read BOOLEAN NOT NULL DEFAULT false,
    
    -- Priority level (optional - for future use)
    priority VARCHAR(20) DEFAULT 'normal' CHECK (priority IN ('low', 'normal', 'high', 'urgent')),
    
    -- Category for grouping notifications (optional)
    category VARCHAR(50),
    
    -- Action URL or deep link (optional)
    action_url TEXT,
    
    -- Expiration date (optional - for time-sensitive notifications)
    expires_at TIMESTAMPTZ,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_notifications_user_id ON public.user_notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_user_notifications_created_at ON public.user_notifications(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_user_notifications_is_read ON public.user_notifications(is_read);
CREATE INDEX IF NOT EXISTS idx_user_notifications_type ON public.user_notifications(type);
CREATE INDEX IF NOT EXISTS idx_user_notifications_user_unread ON public.user_notifications(user_id, is_read) WHERE is_read = false;

-- Create composite index for efficient querying
CREATE INDEX IF NOT EXISTS idx_user_notifications_user_read_created ON public.user_notifications(user_id, is_read, created_at DESC);

-- Create function to automatically update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Create trigger to auto-update updated_at
DROP TRIGGER IF EXISTS update_user_notifications_updated_at ON public.user_notifications;
CREATE TRIGGER update_user_notifications_updated_at
    BEFORE UPDATE ON public.user_notifications
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments to document the table structure
COMMENT ON TABLE public.user_notifications IS 'Stores user notifications for the MADADGAR app';
COMMENT ON COLUMN public.user_notifications.id IS 'Primary key';
COMMENT ON COLUMN public.user_notifications.user_id IS 'ID of the user who will receive this notification';
COMMENT ON COLUMN public.user_notifications.type IS 'Type of notification (new_listing, post_deleted, system_alert)';
COMMENT ON COLUMN public.user_notifications.title IS 'Notification title shown to user';
COMMENT ON COLUMN public.user_notifications.body IS 'Notification message body';
COMMENT ON COLUMN public.user_notifications.payload IS 'JSON data with additional context (item_id, uploader_id, etc.)';
COMMENT ON COLUMN public.user_notifications.is_read IS 'Whether the user has read this notification';
COMMENT ON COLUMN public.user_notifications.priority IS 'Priority level for the notification';
COMMENT ON COLUMN public.user_notifications.category IS 'Category for grouping notifications';
COMMENT ON COLUMN public.user_notifications.action_url IS 'Deep link or URL for notification action';
COMMENT ON COLUMN public.user_notifications.expires_at IS 'When this notification expires (optional)';
COMMENT ON COLUMN public.user_notifications.created_at IS 'When the notification was created';
COMMENT ON COLUMN public.user_notifications.updated_at IS 'When the notification was last updated';

-- Example payload structures for different notification types:
/*
For new_listing notifications:
{
  "item_id": "123",
  "uploader_id": "user-uuid",
  "category": "Electronics",
  "subcategory": "Smartphones",
  "location": "New York",
  "title": "iPhone 13",
  "item_title": "iPhone 13 Pro Max"
}

For post_deleted notifications:
{
  "item_id": "123",
  "uploader_id": "user-uuid",
  "deletion_reason": "Item no longer available",
  "deleted_at": "2023-12-01T10:00:00Z",
  "item_title": "iPhone 13 Pro Max"
}

For system_alert notifications:
{
  "alert_type": "maintenance",
  "severity": "info",
  "maintenance_start": "2023-12-01T02:00:00Z",
  "maintenance_end": "2023-12-01T06:00:00Z"
}
*/
