-- Apply the database trigger to prevent self-notifications
-- This script combines the trigger and RLS policy to ensure users don't receive 
-- notifications about their own posts

-- First, ensure the user_notifications table has proper structure
DO $$
BEGIN
    -- Check if the table exists and has the required columns
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'user_notifications') THEN
        RAISE EXCEPTION 'user_notifications table does not exist';
    END IF;
    
    -- Check if required columns exist
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'user_notifications' AND column_name = 'payload') THEN
        RAISE EXCEPTION 'payload column does not exist in user_notifications table';
    END IF;
END $$;

-- Create the function that will be called by the trigger
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
                RAISE NOTICE 'BLOCKED SELF-NOTIFICATION: user_id=%, uploader_id=%, title=%', 
                    NEW.user_id, (NEW.payload->>'uploader_id'), NEW.title;
                
                -- Return NULL to prevent the insertion
                RETURN NULL;
            ELSE
                -- Log allowed notifications for debugging
                RAISE NOTICE 'ALLOWED NOTIFICATION: user_id=%, uploader_id=%, title=%', 
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

-- Add documentation
COMMENT ON FUNCTION prevent_self_notifications() IS 'Prevents users from receiving notifications about their own posts by checking uploader_id in payload. Blocks new_listing notifications where user_id matches uploader_id.';

-- Also create an RLS policy as additional protection
-- First, enable RLS on the table if not already enabled
ALTER TABLE public.user_notifications ENABLE ROW LEVEL SECURITY;

-- Drop existing conflicting INSERT policies
DROP POLICY IF EXISTS "Authenticated users can create notifications" ON public.user_notifications;
DROP POLICY IF EXISTS "Users can create notifications for themselves" ON public.user_notifications;
DROP POLICY IF EXISTS "Authenticated users can create notifications excluding self" ON public.user_notifications;

-- Create new INSERT policy that prevents self-notifications at RLS level
CREATE POLICY "Block self notifications policy"
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

-- Test the setup with a simple query (this won't insert anything, just shows the structure)
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

-- Show the trigger
SELECT 
    trigger_name,
    event_manipulation,
    action_timing,
    action_statement
FROM information_schema.triggers 
WHERE event_object_table = 'user_notifications'
AND trigger_name = 'prevent_self_notifications_trigger';

-- Success message
DO $$
BEGIN
    RAISE NOTICE '✅ SUCCESS: Self-notification prevention trigger and RLS policies have been applied!';
    RAISE NOTICE '🔒 Users will no longer receive notifications about their own posts.';
    RAISE NOTICE '📝 Both database trigger and RLS policy are active for double protection.';
END $$;
