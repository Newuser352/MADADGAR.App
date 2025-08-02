-- Quick Setup Script for Push Notifications
-- Run this in your Supabase SQL Editor after completing the basic setup

-- Step 1: Verify required tables exist
DO $$
BEGIN
    -- Check if user_device_tokens table exists
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'user_device_tokens') THEN
        RAISE NOTICE '❌ ERROR: user_device_tokens table not found. Please run user_notifications_schema.sql first.';
    ELSE
        RAISE NOTICE '✅ user_device_tokens table exists';
    END IF;
    
    -- Check if notification_send_log table exists
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'notification_send_log') THEN
        RAISE NOTICE '❌ ERROR: notification_send_log table not found. Please run auto_push_notifications_trigger.sql first.';
    ELSE
        RAISE NOTICE '✅ notification_send_log table exists';
    END IF;
    
    -- Check if items table exists
    IF NOT EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'items') THEN
        RAISE NOTICE '❌ ERROR: items table not found. Please create your items table first.';
    ELSE
        RAISE NOTICE '✅ items table exists';
    END IF;
END $$;

-- Step 2: Check if HTTP extension is enabled
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_extension WHERE extname = 'http') THEN
        RAISE NOTICE '✅ HTTP extension is enabled';
    ELSE
        RAISE NOTICE '❌ ERROR: HTTP extension not found. Please enable it in Supabase Dashboard → Database → Extensions';
        -- Try to enable it (may need to be done from dashboard)
        -- CREATE EXTENSION IF NOT EXISTS http;
    END IF;
END $$;

-- Step 3: Check if trigger function exists
DO $$
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.routines 
        WHERE routine_name = 'send_new_item_push_notifications'
    ) THEN
        RAISE NOTICE '✅ Push notification trigger function exists';
    ELSE
        RAISE NOTICE '❌ ERROR: Push notification function not found. Please run auto_push_notifications_trigger.sql';
    END IF;
END $$;

-- Step 4: Check if trigger exists on items table
DO $$
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.triggers 
        WHERE trigger_name = 'trigger_send_new_item_push_notifications'
    ) THEN
        RAISE NOTICE '✅ Push notification trigger exists on items table';
    ELSE
        RAISE NOTICE '❌ ERROR: Push notification trigger not found. Please run auto_push_notifications_trigger.sql';
    END IF;
END $$;

-- Step 5: Test function for manual notification sending
-- Replace 'your-project-id' with your actual Supabase project ID
CREATE OR REPLACE FUNCTION test_push_notification_setup()
RETURNS TEXT AS $$
DECLARE
    active_tokens INTEGER;
    result_text TEXT := '';
BEGIN
    -- Count active device tokens
    SELECT COUNT(*) INTO active_tokens 
    FROM user_device_tokens 
    WHERE is_active = true;
    
    result_text := result_text || '📱 Active device tokens: ' || active_tokens || E'\n';
    
    IF active_tokens = 0 THEN
        result_text := result_text || '⚠️ No active device tokens found. Users need to login to register tokens.' || E'\n';
    ELSE
        result_text := result_text || '✅ Ready to send notifications!' || E'\n';
    END IF;
    
    -- Check recent notifications
    result_text := result_text || '📋 Recent notification logs: ';
    SELECT COUNT(*) INTO active_tokens 
    FROM notification_send_log 
    WHERE sent_at > NOW() - INTERVAL '24 hours';
    
    result_text := result_text || active_tokens || ' in last 24 hours' || E'\n';
    
    RETURN result_text;
END;
$$ LANGUAGE plpgsql;

-- Step 6: Send test notification to all active users
CREATE OR REPLACE FUNCTION send_test_notification()
RETURNS TEXT AS $$
DECLARE
    user_ids_array UUID[];
    result_text TEXT;
BEGIN
    -- Get all active user IDs
    SELECT ARRAY_AGG(DISTINCT user_id) INTO user_ids_array
    FROM user_device_tokens 
    WHERE is_active = true;
    
    IF user_ids_array IS NULL OR array_length(user_ids_array, 1) = 0 THEN
        RETURN '❌ No active users found to send test notification';
    END IF;
    
    -- Send test notification
    SELECT manual_send_push_notification(
        user_ids_array,
        'MADADGAR Test Notification 🧪',
        'Your push notification system is working! You can now receive alerts for new posts.',
        'test_setup',
        '{"source": "setup_test", "timestamp": "' || NOW()::TEXT || '"}'::jsonb
    )::TEXT INTO result_text;
    
    RETURN '✅ Test notification sent to ' || array_length(user_ids_array, 1) || ' users. Result: ' || result_text;
END;
$$ LANGUAGE plpgsql;

-- Step 7: Update database settings (replace with your actual URLs)
-- IMPORTANT: Update these with your real Supabase project details
DO $$
BEGIN
    -- Check if settings are configured
    BEGIN
        PERFORM current_setting('app.supabase_url', true);
        RAISE NOTICE '✅ Supabase URL setting exists';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE '⚠️ Supabase URL not configured. Run: ALTER DATABASE postgres SET app.supabase_url = ''https://your-project.supabase.co'';';
    END;
    
    BEGIN
        PERFORM current_setting('app.service_role_key', true);
        RAISE NOTICE '✅ Service role key setting exists';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE '⚠️ Service role key not configured. Run: ALTER DATABASE postgres SET app.service_role_key = ''your-service-role-key'';';
    END;
END $$;

-- Final setup completion message
DO $$
BEGIN
    RAISE NOTICE '🎉 PUSH NOTIFICATION SETUP CHECK COMPLETE!';
    RAISE NOTICE '';
    RAISE NOTICE '📋 NEXT STEPS:';
    RAISE NOTICE '1. Fix any ❌ errors shown above';
    RAISE NOTICE '2. Deploy your Supabase Edge Function';
    RAISE NOTICE '3. Set FCM_SERVER_KEY in Supabase environment variables';
    RAISE NOTICE '4. Test with: SELECT test_push_notification_setup();';
    RAISE NOTICE '5. Send test notification: SELECT send_test_notification();';
    RAISE NOTICE '';
    RAISE NOTICE '🔧 MANUAL TESTING:';
    RAISE NOTICE '- Login to your app to register FCM token';
    RAISE NOTICE '- Create a new post to trigger automatic notification';
    RAISE NOTICE '- Check notification_send_log table for results';
    RAISE NOTICE '';
    RAISE NOTICE '📱 REMEMBER: Test on a physical device, not emulator!';
END $$;

-- Test the setup immediately
SELECT test_push_notification_setup() as setup_status;
