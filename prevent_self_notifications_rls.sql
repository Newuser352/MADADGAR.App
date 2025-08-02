-- RLS policy to prevent users from receiving notifications about their own posts
-- This policy blocks INSERT operations where the user_id matches the uploader_id in the payload

-- First, drop existing INSERT policies to avoid conflicts
DROP POLICY IF EXISTS "Authenticated users can create notifications" ON public.user_notifications;
DROP POLICY IF EXISTS "Users can create notifications for themselves" ON public.user_notifications;

-- Create new INSERT policy that prevents self-notifications
CREATE POLICY "Authenticated users can create notifications excluding self"
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

-- Keep existing policies for other operations
CREATE POLICY "Users can view their own notifications"
    ON public.user_notifications
    FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own notifications"
    ON public.user_notifications
    FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own notifications"
    ON public.user_notifications
    FOR DELETE
    USING (auth.uid() = user_id);

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
