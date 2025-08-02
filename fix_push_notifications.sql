-- Complete Push Notification Fix Script
-- Run this in your Supabase SQL Editor to fix notification issues

-- ========================================
-- STEP 1: Create user_device_tokens table
-- ========================================
CREATE TABLE IF NOT EXISTS public.user_device_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    device_token TEXT NOT NULL,
    platform VARCHAR(20) DEFAULT 'android',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, device_token)
);

-- Add indexes
CREATE INDEX IF NOT EXISTS idx_user_device_tokens_user_id ON public.user_device_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_user_device_tokens_active ON public.user_device_tokens(is_active);

-- Enable RLS
ALTER TABLE public.user_device_tokens ENABLE ROW LEVEL SECURITY;

-- Drop existing policies
DROP POLICY IF EXISTS "Users can manage their own device tokens" ON public.user_device_tokens;
DROP POLICY IF EXISTS "Service role can manage all tokens" ON public.user_device_tokens;

-- Create policies
CREATE POLICY "Users can manage their own device tokens"
    ON public.user_device_tokens
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Service role can manage all tokens"
    ON public.user_device_tokens
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- ========================================
-- STEP 2: Check and fix database settings
-- ========================================
DO $$
DECLARE
    project_url TEXT;
    service_key TEXT;
BEGIN
    RAISE NOTICE '🔧 CONFIGURING DATABASE SETTINGS...';
    
    -- Check current settings
    BEGIN
        project_url := current_setting('app.supabase_url', true);
        service_key := current_setting('app.service_role_key', true);
        
        IF project_url IS NULL OR project_url = '' OR project_url LIKE '%your-project%' THEN
            RAISE NOTICE '❌ Supabase URL not properly configured';
            RAISE NOTICE '⚠️ UPDATE REQUIRED: Replace your-project-id with your actual project ID in the next commands';
        ELSE
            RAISE NOTICE '✅ Supabase URL configured: %', project_url;
        END IF;
        
        IF service_key IS NULL OR service_key = '' OR service_key LIKE '%your-service%' THEN
            RAISE NOTICE '❌ Service role key not properly configured';
            RAISE NOTICE '⚠️ UPDATE REQUIRED: Replace your-service-role-key with your actual key in the next commands';
        ELSE
            RAISE NOTICE '✅ Service role key configured';
        END IF;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE '❌ Database settings not configured at all';
    END;
END $$;

-- ========================================
-- IMPORTANT: Update these with your actual values
-- ========================================
-- Get these from your Supabase Dashboard:
-- 1. Project URL: Settings > API > Project URL
-- 2. Service Role Key: Settings > API > Service role key

-- Uncomment and update these lines with your actual values:
-- ALTER DATABASE postgres SET app.supabase_url = 'https://YOUR-PROJECT-ID.supabase.co';
-- ALTER DATABASE postgres SET app.service_role_key = 'YOUR-SERVICE-ROLE-KEY';

-- ========================================
-- STEP 3: Enable HTTP extension
-- ========================================
CREATE EXTENSION IF NOT EXISTS http;

-- ========================================
-- STEP 4: Fix the trigger function
-- ========================================
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
    auth_header TEXT;
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
        
        -- Check if we have users to notify
        IF user_ids_array IS NULL OR array_length(user_ids_array, 1) = 0 THEN
            RAISE NOTICE '⚠️ No users with device tokens found to notify';
            -- Try to get all users as fallback
            SELECT ARRAY_AGG(DISTINCT id)
            INTO user_ids_array
            FROM auth.users
            WHERE id != NEW.owner_id;
            
            IF user_ids_array IS NULL OR array_length(user_ids_array, 1) = 0 THEN
                RAISE NOTICE '❌ No users found at all';
                RETURN NEW;
            END IF;
        END IF;
        
        RAISE NOTICE '📋 Found % users to notify', array_length(user_ids_array, 1);
        
        -- Prepare notification content
        notification_title := 'New ' || COALESCE(NEW.main_category, 'Item') || ' Available!';
        notification_body := COALESCE(NEW.title, 'New item') || ' has been shared in ' || COALESCE(NEW.location, 'your area');
        
        -- Prepare payload data
        payload_data := jsonb_build_object(
            'item_id', NEW.id,
            'uploader_id', NEW.owner_id,
            'category', COALESCE(NEW.main_category, ''),
            'subcategory', COALESCE(NEW.sub_category, ''),
            'location', COALESCE(NEW.location, ''),
            'title', COALESCE(NEW.title, ''),
            'item_title', COALESCE(NEW.title, '')
        );
        
        -- Construct the Supabase Edge Function URL
        BEGIN
            function_url := current_setting('app.supabase_url', true) || '/functions/v1/send-push-notifications';
            auth_header := 'Bearer ' || current_setting('app.service_role_key', true);
        EXCEPTION WHEN OTHERS THEN
            RAISE WARNING '❌ Database settings not configured. Please set app.supabase_url and app.service_role_key';
            RETURN NEW;
        END;
        
        RAISE NOTICE '🌐 Calling push notification function at: %', function_url;
        
        -- Call the Supabase Edge Function to send push notifications
        BEGIN
            SELECT status, content::text
            INTO response_status, response_body
            FROM http((
                'POST',
                function_url,
                ARRAY[
                    http_header('Content-Type', 'application/json'),
                    http_header('Authorization', auth_header)
                ],
                'application/json',
                jsonb_build_object(
                    'userIds', user_ids_array,
                    'title', notification_title,
                    'body', notification_body,
                    'type', 'new_listing',
                    'data', payload_data
                )::text
            )::http_request);
            
            IF response_status = 200 THEN
                RAISE NOTICE '✅ Push notifications sent successfully';
            ELSE
                RAISE WARNING '❌ Push notification failed with status %: %', response_status, response_body;
            END IF;
            
        EXCEPTION WHEN OTHERS THEN
            -- Log the error but don't fail the item creation
            RAISE WARNING '❌ Failed to send push notifications: %', SQLERRM;
            
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

-- Drop and recreate the trigger
DROP TRIGGER IF EXISTS trigger_send_new_item_push_notifications ON public.items;

CREATE TRIGGER trigger_send_new_item_push_notifications
    AFTER INSERT ON public.items
    FOR EACH ROW
    EXECUTE FUNCTION send_new_item_push_notifications();

-- ========================================
-- STEP 5: Create manual test function
-- ========================================
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
    auth_header TEXT;
    response_status INTEGER;
    response_body TEXT;
    result JSONB;
BEGIN
    -- Get URL and auth
    BEGIN
        function_url := current_setting('app.supabase_url', true) || '/functions/v1/send-push-notifications';
        auth_header := 'Bearer ' || current_setting('app.service_role_key', true);
    EXCEPTION WHEN OTHERS THEN
        RETURN jsonb_build_object(
            'error', 'Database settings not configured. Please set app.supabase_url and app.service_role_key'
        );
    END;
    
    -- Call the Edge Function
    SELECT status, content::text
    INTO response_status, response_body
    FROM http((
        'POST',
        function_url,
        ARRAY[
            http_header('Content-Type', 'application/json'),
            http_header('Authorization', auth_header)
        ],
        'application/json',
        jsonb_build_object(
            'userIds', p_user_ids,
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
EXCEPTION WHEN OTHERS THEN
    RETURN jsonb_build_object(
        'error', SQLERRM,
        'status', 0
    );
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ========================================
-- STEP 6: Quick test function
-- ========================================
CREATE OR REPLACE FUNCTION test_notifications_now()
RETURNS TEXT AS $$
DECLARE
    test_user_id UUID;
    active_tokens INTEGER;
    result TEXT := '';
BEGIN
    -- Check device tokens
    SELECT COUNT(*) INTO active_tokens FROM user_device_tokens WHERE is_active = true;
    result := result || '📱 Active device tokens: ' || active_tokens || E'\n';
    
    -- Get a test user
    SELECT user_id INTO test_user_id FROM user_device_tokens WHERE is_active = true LIMIT 1;
    
    IF test_user_id IS NULL THEN
        SELECT id INTO test_user_id FROM auth.users LIMIT 1;
    END IF;
    
    IF test_user_id IS NULL THEN
        RETURN result || '❌ No users found. Please login to the app first.';
    END IF;
    
    -- Try to send a test notification
    result := result || E'\n🧪 Sending test notification...\n';
    
    DECLARE
        test_result JSONB;
    BEGIN
        SELECT manual_send_push_notification(
            ARRAY[test_user_id]::UUID[],
            'Test Push Notification 🔔',
            'If you see this, push notifications are working!',
            'test',
            '{"test": true}'::jsonb
        ) INTO test_result;
        
        result := result || '📬 Result: ' || test_result::text;
    EXCEPTION WHEN OTHERS THEN
        result := result || '❌ Error: ' || SQLERRM;
    END;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- FINAL STATUS CHECK
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🎯 PUSH NOTIFICATION FIX COMPLETE!';
    RAISE NOTICE '';
    RAISE NOTICE '📋 REQUIRED ACTIONS:';
    RAISE NOTICE '1. ⚠️ UPDATE the database settings with your actual Supabase project details (see comments above)';
    RAISE NOTICE '2. 📱 Deploy the Edge Function: supabase functions deploy send-push-notifications';
    RAISE NOTICE '3. 🔑 Set FCM_SERVER_KEY in Supabase environment variables';
    RAISE NOTICE '4. 📲 Login to your app to register device token';
    RAISE NOTICE '5. 🧪 Test with: SELECT test_notifications_now();';
    RAISE NOTICE '';
    RAISE NOTICE '🔍 TROUBLESHOOTING:';
    RAISE NOTICE '- Check device tokens: SELECT * FROM user_device_tokens;';
    RAISE NOTICE '- Check logs: SELECT * FROM notification_send_log ORDER BY sent_at DESC;';
    RAISE NOTICE '- Test trigger: INSERT a test item in the items table';
END $$;
