-- Complete RLS policies for user_notifications table
-- Run this in Supabase SQL Editor

-- 1. Enable RLS on the table
ALTER TABLE public.user_notifications ENABLE ROW LEVEL SECURITY;

-- 2. Drop any existing policies to start fresh
DROP POLICY IF EXISTS "Users can view their own notifications" ON public.user_notifications;
DROP POLICY IF EXISTS "Users can create notifications for themselves" ON public.user_notifications;
DROP POLICY IF EXISTS "Everyone but uploader can read" ON public.user_notifications;
DROP POLICY IF EXISTS "Users can update their own notifications" ON public.user_notifications;
DROP POLICY IF EXISTS "Users can delete their own notifications" ON public.user_notifications;

-- 3. SELECT Policy - Users can only read their own notifications
CREATE POLICY "Users can view their own notifications"
    ON public.user_notifications
    FOR SELECT
    USING (auth.uid() = user_id);

-- 4. INSERT Policy - Allow authenticated users to create notifications for any user
-- This is needed for the app to create notifications for other users when posts are uploaded
CREATE POLICY "Authenticated users can create notifications"
    ON public.user_notifications
    FOR INSERT
    WITH CHECK (auth.role() = 'authenticated');

-- 5. UPDATE Policy - Users can only update their own notifications (for marking as read)
CREATE POLICY "Users can update their own notifications"
    ON public.user_notifications
    FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- 6. DELETE Policy - Users can only delete their own notifications
CREATE POLICY "Users can delete their own notifications"
    ON public.user_notifications
    FOR DELETE
    USING (auth.uid() = user_id);

-- 7. Verify all policies are correctly applied
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

-- 8. Check if RLS is enabled
SELECT 
    schemaname,
    tablename,
    rowsecurity,
    forcerowsecurity
FROM pg_tables 
WHERE tablename = 'user_notifications';
