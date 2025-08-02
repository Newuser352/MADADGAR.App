-- Database trigger to automatically send push notifications when new items are created
-- This will call the Supabase Edge Function to send push notifications to users' phones

-- First, create the notification_send_log table for tracking
CREATE TABLE IF NOT EXISTS public.notification_send_log (
    id BIGSERIAL PRIMARY KEY,
    user_ids UUID[],
    title VARCHAR(255),
    body TEXT,
    type VARCHAR(50),
    success_count INTEGER DEFAULT 0,
    failure_count INTEGER DEFAULT 0,
    results JSONB,
    sent_at TIMESTAMPTZ DEFAULT NOW(),
    item_id UUID,
    uploader_id UUID
);

-- Add RLS policy for notification_send_log
ALTER TABLE public.notification_send_log ENABLE ROW LEVEL SECURITY;

-- Policy to allow service role to insert/select
DROP POLICY IF EXISTS "Service role can manage notification logs" ON public.notification_send_log;
CREATE POLICY "Service role can manage notification logs"
    ON public.notification_send_log
    FOR ALL
    USING (true)
    WITH CHECK (true);

-- Create function to send push notifications for new items
CREATE OR REPLACE FUNCTION send_new_item_push_notifications()
RETURNS TRIGGER AS $$
DECLARE
    user_ids_array UUID[];
    notification_title TEXT;
    notification_body TEXT;
    payload_data JSONB;
    response_status INTEGER;
    response_body TEXT;
    function_url TEXT;
BEGIN
    -- Only send notifications for new items (not updates)
    IF TG_OP = 'INSERT' AND NEW.is_active = true THEN
        
        RAISE NOTICE '📱 NEW ITEM CREATED: Preparing push notifications for item: %', NEW.title;
        
        -- Get all active user IDs except the uploader
        SELECT ARRAY_AGG(DISTINCT udt.user_id)
        INTO user_ids_array
        FROM public.user_device_tokens udt
        WHERE udt.is_active = true 
        AND udt.user_id != NEW.owner_id;
        
        -- Also include users from profiles table (fallback)
        IF user_ids_array IS NULL OR array_length(user_ids_array, 1) = 0 THEN
            SELECT ARRAY_AGG(DISTINCT p.id)
            INTO user_ids_array  
            FROM auth.users p
            WHERE p.id != NEW.owner_id;
        END IF;
        
        -- Check if we have users to notify
        IF user_ids_array IS NULL OR array_length(user_ids_array, 1) = 0 THEN
            RAISE NOTICE '⚠️ No users found to notify for new item: %', NEW.title;
            RETURN NEW;
        END IF;
        
        RAISE NOTICE '📋 Found % users to notify about new item: %', array_length(user_ids_array, 1), NEW.title;
        
        -- Prepare notification content
        notification_title := 'New ' || NEW.main_category || ' Available!';
        notification_body := NEW.title || ' has been shared in ' || NEW.location;
        
        -- Prepare payload data
        payload_data := jsonb_build_object(
            'item_id', NEW.id,
            'uploader_id', NEW.owner_id,
            'category', NEW.main_category,
            'subcategory', COALESCE(NEW.sub_category, ''),
            'location', NEW.location,
            'title', NEW.title,
            'item_title', NEW.title
        );
        
        -- Construct the Supabase Edge Function URL
        -- Replace 'your-project-id' with your actual Supabase project ID
        function_url := current_setting('app.supabase_url', true) || '/functions/v1/send-push-notifications';
        
        -- If the setting is not available, use a placeholder (you'll need to update this)
        IF function_url IS NULL OR function_url = '/functions/v1/send-push-notifications' THEN
            function_url := 'https://your-project-id.supabase.co/functions/v1/send-push-notifications';
            RAISE NOTICE '⚠️ Using placeholder URL. Please update with your actual Supabase project URL';
        END IF;
        
        RAISE NOTICE '🌐 Calling push notification function at: %', function_url;
        
        -- Call the Supabase Edge Function to send push notifications
        BEGIN
            SELECT status, content
            INTO response_status, response_body
            FROM http((
                'POST',
                function_url,
                ARRAY[
                    http_header('Content-Type', 'application/json'),
                    http_header('Authorization', 'Bearer ' || current_setting('app.service_role_key', true))
                ],
                jsonb_build_object(
                    'userIds', (
                        SELECT jsonb_agg(user_id)
                        FROM unnest(user_ids_array) AS user_id
                    ),
                    'title', notification_title,
                    'body', notification_body,
                    'type', 'new_listing',
                    'data', payload_data
                )::text
            )::http_request);
            
            RAISE NOTICE '✅ Push notification function called successfully. Status: %, Response: %', response_status, response_body;
            
        EXCEPTION WHEN OTHERS THEN
            -- Log the error but don't fail the item creation
            RAISE WARNING '❌ Failed to send push notifications for item %: %', NEW.id, SQLERRM;
            
            -- Insert error log
            INSERT INTO public.notification_send_log (
                user_ids, title, body, type, success_count, failure_count, 
                results, item_id, uploader_id
            ) VALUES (
                user_ids_array, notification_title, notification_body, 'new_listing', 
                0, array_length(user_ids_array, 1),
                jsonb_build_object('error', SQLERRM),
                NEW.id, NEW.owner_id
            );
        END;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Drop existing trigger if it exists
DROP TRIGGER IF EXISTS trigger_send_new_item_push_notifications ON public.items;

-- Create the trigger on the items table
CREATE TRIGGER trigger_send_new_item_push_notifications
    AFTER INSERT ON public.items
    FOR EACH ROW
    EXECUTE FUNCTION send_new_item_push_notifications();

-- Enable the http extension (required for making HTTP requests)
-- Note: You might need to enable this in Supabase dashboard under Database > Extensions
-- CREATE EXTENSION IF NOT EXISTS http;

-- Add documentation
COMMENT ON FUNCTION send_new_item_push_notifications() IS 'Automatically sends push notifications to users when new items are created. Calls Supabase Edge Function to handle FCM messaging.';

-- Create a manual function to send push notifications (for testing or manual triggering)
CREATE OR REPLACE FUNCTION manual_send_push_notification(
    p_user_ids UUID[],
    p_title TEXT,
    p_body TEXT,
    p_type TEXT DEFAULT 'manual',
    p_data JSONB DEFAULT '{}'::jsonb
)
RETURNS JSONB AS $$
DECLARE
    function_url TEXT;
    response_status INTEGER;
    response_body TEXT;
    result JSONB;
BEGIN
    -- Construct the function URL
    function_url := current_setting('app.supabase_url', true) || '/functions/v1/send-push-notifications';
    
    IF function_url IS NULL OR function_url = '/functions/v1/send-push-notifications' THEN
        function_url := 'https://your-project-id.supabase.co/functions/v1/send-push-notifications';
    END IF;
    
    -- Call the Edge Function
    SELECT status, content
    INTO response_status, response_body
    FROM http((
        'POST',
        function_url,
        ARRAY[
            http_header('Content-Type', 'application/json'),
            http_header('Authorization', 'Bearer ' || current_setting('app.service_role_key', true))
        ],
        jsonb_build_object(
            'userIds', (SELECT jsonb_agg(user_id) FROM unnest(p_user_ids) AS user_id),
            'title', p_title,
            'body', p_body,
            'type', p_type,
            'data', p_data
        )::text
    )::http_request);
    
    -- Return result
    result := jsonb_build_object(
        'status', response_status,
        'response', response_body::jsonb
    );
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

COMMENT ON FUNCTION manual_send_push_notification(UUID[], TEXT, TEXT, TEXT, JSONB) IS 'Manually send push notifications to specified users. Useful for testing or administrative purposes.';

-- Success message
DO $$
BEGIN
    RAISE NOTICE '✅ SUCCESS: Push notification trigger and functions created!';
    RAISE NOTICE '🔧 Trigger will automatically send push notifications when new items are created';
    RAISE NOTICE '📱 Users will receive notifications on their phones even when app is closed';
    RAISE NOTICE '🛠️ Manual function available: SELECT manual_send_push_notification(...);';
    RAISE NOTICE '⚠️ IMPORTANT: Update the function URL with your actual Supabase project ID!';
    RAISE NOTICE '📋 NEXT STEPS:';
    RAISE NOTICE '   1. Deploy the Supabase Edge Function';
    RAISE NOTICE '   2. Set FCM_SERVER_KEY environment variable in Supabase';
    RAISE NOTICE '   3. Enable http extension in Supabase';
    RAISE NOTICE '   4. Update function URLs with your project ID';
END $$;
