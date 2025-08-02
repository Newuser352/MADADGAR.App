-- Complete setup for user_notifications table with self-notification prevention
-- Run this in Supabase SQL Editor

-- ====================================
-- STEP 1: CREATE TABLE AND INDEXES
-- ====================================

-- Drop existing table if you want to recreate it (CAREFUL - this will delete all data!)
-- DROP TABLE IF EXISTS public.user_notifications CASCADE;

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

-- ====================================
-- STEP 2: AUTO-UPDATE TRIGGERS
-- ====================================

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

-- ====================================
-- STEP 3: SELF-NOTIFICATION PREVENTION
-- ====================================

-- Create function to prevent users from receiving notifications about their own posts
CREATE OR REPLACE FUNCTION prevent_self_notifications()
RETURNS TRIGGER AS $$
BEGIN
    -- Check if this is a new_listing notification
    IF NEW.type = 'new_listing' THEN
        -- Extract the uploader_id from the payload JSON
        IF NEW.payload ? 'uploader_id' THEN
            -- Compare the notification recipient (user_id) with the uploader_id from payload
            IF NEW.user_id::text = (NEW.payload->>'uploader_id') THEN
                -- Log the prevention for debugging
                RAISE NOTICE '🚫 BLOCKED SELF-NOTIFICATION: user_id=%, uploader_id=%, title=%', 
                    NEW.user_id, (NEW.payload->>'uploader_id'), NEW.title;
                
                -- Return NULL to prevent the insertion
                RETURN NULL;
            ELSE
                -- Log allowed notifications for debugging
                RAISE NOTICE '✅ ALLOWED NOTIFICATION: user_id=%, uploader_id=%, title=%', 
                    NEW.user_id, (NEW.payload->>'uploader_id'), NEW.title;
            END IF;
        END IF;
    END IF;
    
    -- For all other cases, allow the insertion
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Drop the trigger if it exists and recreate it
DROP TRIGGER IF EXISTS prevent_self_notifications_trigger ON public.user_notifications;

-- Create the trigger that calls our function before each INSERT
CREATE TRIGGER prevent_self_notifications_trigger
    BEFORE INSERT ON public.user_notifications
    FOR EACH ROW
    EXECUTE FUNCTION prevent_self_notifications();

-- ====================================
-- STEP 4: ROW LEVEL SECURITY (RLS)
-- ====================================

-- Enable RLS on the table
ALTER TABLE public.user_notifications ENABLE ROW LEVEL SECURITY;

-- Drop existing conflicting INSERT policies
DROP POLICY IF EXISTS "Authenticated users can create notifications" ON public.user_notifications;
DROP POLICY IF EXISTS "Users can create notifications for themselves" ON public.user_notifications;
DROP POLICY IF EXISTS "Authenticated users can create notifications excluding self" ON public.user_notifications;
DROP POLICY IF EXISTS "Block self notifications policy" ON public.user_notifications;

-- Create new INSERT policy that prevents self-notifications at RLS level
CREATE POLICY "Prevent self notifications RLS"
    ON public.user_notifications
    FOR INSERT
    WITH CHECK (
        auth.role() = 'authenticated'
        AND (
            -- Allow if it's not a new_listing notification
            type != 'new_listing'
            OR
            -- Allow if payload doesn't contain uploader_id
            payload->>'uploader_id' IS NULL
            OR
            -- Allow if user_id is different from uploader_id
            user_id::text != (payload->>'uploader_id')
        )
    );

-- Policy for SELECT (users can view their own notifications)
DROP POLICY IF EXISTS "Users can view their own notifications" ON public.user_notifications;
CREATE POLICY "Users can view their own notifications"
    ON public.user_notifications
    FOR SELECT
    USING (auth.uid()::text = user_id::text);

-- Policy for UPDATE (users can update their own notifications)  
DROP POLICY IF EXISTS "Users can update their own notifications" ON public.user_notifications;
CREATE POLICY "Users can update their own notifications"
    ON public.user_notifications
    FOR UPDATE
    USING (auth.uid()::text = user_id::text)
    WITH CHECK (auth.uid()::text = user_id::text);

-- Policy for DELETE (users can delete their own notifications)
DROP POLICY IF EXISTS "Users can delete their own notifications" ON public.user_notifications;
CREATE POLICY "Users can delete their own notifications"
    ON public.user_notifications
    FOR DELETE
    USING (auth.uid()::text = user_id::text);

-- ====================================
-- STEP 5: DOCUMENTATION
-- ====================================

-- Add comments to document the table structure
COMMENT ON TABLE public.user_notifications IS 'Stores user notifications for the MADADGAR app with self-notification prevention';
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

-- Add documentation for functions
COMMENT ON FUNCTION prevent_self_notifications() IS 'Prevents users from receiving notifications about their own posts by checking uploader_id in payload. Blocks new_listing notifications where user_id matches uploader_id.';
COMMENT ON FUNCTION update_updated_at_column() IS 'Automatically updates the updated_at timestamp when a row is modified';

-- ====================================
-- STEP 6: VERIFICATION
-- ====================================

-- Verify the policies are applied
SELECT 
    schemaname,
    tablename,
    policyname,
    permissive,
    roles,
    cmd,
    qual,
    with_check
FROM pg_policies 
WHERE tablename = 'user_notifications'
ORDER BY cmd, policyname;

-- Show the triggers
SELECT 
    trigger_name,
    event_manipulation,
    action_timing,
    action_statement
FROM information_schema.triggers 
WHERE event_object_table = 'user_notifications'
ORDER BY trigger_name;

-- Show table structure
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns 
WHERE table_name = 'user_notifications' 
ORDER BY ordinal_position;

-- ====================================
-- STEP 7: SUCCESS MESSAGE
-- ====================================

DO $$
BEGIN
    RAISE NOTICE '✅ SUCCESS: user_notifications table setup complete!';
    RAISE NOTICE '🔧 Table created with all necessary columns and indexes';
    RAISE NOTICE '🚫 Self-notification prevention trigger active';
    RAISE NOTICE '🔒 Row Level Security policies applied';
    RAISE NOTICE '⚡ Auto-update triggers configured';
    RAISE NOTICE '📋 Ready for use with MADADGAR app!';
END $$;

-- ====================================
-- EXAMPLE PAYLOAD STRUCTURES
-- ====================================

/*
For new_listing notifications:
{
  "item_id": "123",
  "uploader_id": "user-uuid-here",
  "category": "Electronics", 
  "subcategory": "Smartphones",
  "location": "New York",
  "title": "iPhone 13",
  "item_title": "iPhone 13 Pro Max"
}

For post_deleted notifications:
{
  "item_id": "123",
  "uploader_id": "user-uuid-here",
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

-- Test queries (optional - you can run these after the table is created)
/*
-- Count notifications by type
SELECT type, COUNT(*) as count FROM public.user_notifications GROUP BY type;

-- Check recent notifications
SELECT id, user_id, type, title, created_at 
FROM public.user_notifications 
ORDER BY created_at DESC 
LIMIT 10;

-- Count unread notifications per user
SELECT user_id, COUNT(*) as unread_count 
FROM public.user_notifications 
WHERE is_read = false 
GROUP BY user_id;
*/
