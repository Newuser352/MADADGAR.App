-- Comprehensive Push Notification Diagnostic Script
-- Run this in your Supabase SQL Editor to identify issues

-- ========================================
-- STEP 1: Check if required tables exist
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '🔍 CHECKING DATABASE SETUP...';
    RAISE NOTICE '';
END $$;

-- Check user_device_tokens table
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'user_device_tokens') THEN
        RAISE NOTICE '✅ user_device_tokens table exists';
        
        -- Show structure
        RAISE NOTICE '📋 Table structure:';
        FOR rec IN 
            SELECT column_name, data_type, is_nullable 
            FROM information_schema.columns 
            WHERE table_name = 'user_device_tokens'
            ORDER BY ordinal_position
        LOOP
            RAISE NOTICE '  - %: % (nullable: %)', rec.column_name, rec.data_type, rec.is_nullable;
        END LOOP;
    ELSE
        RAISE NOTICE '❌ user_device_tokens table MISSING! Run user_notifications_schema.sql';
    END IF;
END $$;

-- Check notification_send_log table
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'notification_send_log') THEN
        RAISE NOTICE '✅ notification_send_log table exists';
    ELSE
        RAISE NOTICE '❌ notification_send_log table MISSING! Run auto_push_notifications_trigger.sql';
    END IF;
END $$;

-- Check items table
DO $$
BEGIN
    IF EXISTS (SELECT FROM information_schema.tables WHERE table_name = 'items') THEN
        RAISE NOTICE '✅ items table exists';
    ELSE
        RAISE NOTICE '❌ items table MISSING! Create your items table first';
    END IF;
END $$;

-- ========================================
-- STEP 2: Check active device tokens
-- ========================================
DO $$
DECLARE
    token_count INTEGER;
    user_count INTEGER;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '📱 CHECKING DEVICE TOKENS...';
    
    -- Count active tokens
    SELECT COUNT(*) INTO token_count FROM user_device_tokens WHERE is_active = true;
    SELECT COUNT(DISTINCT user_id) INTO user_count FROM user_device_tokens WHERE is_active = true;
    
    RAISE NOTICE '📊 Active device tokens: %', token_count;
    RAISE NOTICE '👥 Unique users with tokens: %', user_count;
    
    IF token_count = 0 THEN
        RAISE NOTICE '⚠️ NO ACTIVE TOKENS! Users need to login to app to register FCM tokens';
    END IF;
END $$;

-- Show recent device token registrations
SELECT 
    user_id,
    LEFT(device_token, 20) || '...' as token_preview,
    platform,
    is_active,
    created_at,
    updated_at
FROM user_device_tokens 
ORDER BY updated_at DESC 
LIMIT 5;

-- ========================================
-- STEP 3: Check database trigger setup
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '⚙️ CHECKING TRIGGER SETUP...';
    
    -- Check if trigger function exists
    IF EXISTS (
        SELECT FROM information_schema.routines 
        WHERE routine_name = 'send_new_item_push_notifications'
    ) THEN
        RAISE NOTICE '✅ Push notification function exists';
    ELSE
        RAISE NOTICE '❌ Push notification function MISSING! Run auto_push_notifications_trigger.sql';
    END IF;
    
    -- Check if trigger exists
    IF EXISTS (
        SELECT FROM information_schema.triggers 
        WHERE trigger_name = 'trigger_send_new_item_push_notifications'
    ) THEN
        RAISE NOTICE '✅ Push notification trigger exists on items table';
    ELSE
        RAISE NOTICE '❌ Push notification trigger MISSING! Run auto_push_notifications_trigger.sql';
    END IF;
END $$;

-- ========================================
-- STEP 4: Check HTTP extension
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🌐 CHECKING HTTP EXTENSION...';
    
    IF EXISTS (SELECT FROM pg_extension WHERE extname = 'http') THEN
        RAISE NOTICE '✅ HTTP extension is enabled';
    ELSE
        RAISE NOTICE '❌ HTTP extension MISSING! Enable in Supabase Dashboard → Database → Extensions';
    END IF;
END $$;

-- ========================================
-- STEP 5: Check database configuration
-- ========================================
DO $$
DECLARE
    supabase_url TEXT;
    service_key TEXT;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🔧 CHECKING DATABASE CONFIGURATION...';
    
    -- Check Supabase URL setting
    BEGIN
        supabase_url := current_setting('app.supabase_url', true);
        IF supabase_url IS NULL OR supabase_url = '' THEN
            RAISE NOTICE '❌ Supabase URL not configured';
            RAISE NOTICE '   Run: ALTER DATABASE postgres SET app.supabase_url = ''https://your-project.supabase.co'';';
        ELSE
            RAISE NOTICE '✅ Supabase URL configured: %', supabase_url;
        END IF;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE '❌ Supabase URL setting missing';
        RAISE NOTICE '   Run: ALTER DATABASE postgres SET app.supabase_url = ''https://your-project.supabase.co'';';
    END;
    
    -- Check service role key setting
    BEGIN
        service_key := current_setting('app.service_role_key', true);
        IF service_key IS NULL OR service_key = '' THEN
            RAISE NOTICE '❌ Service role key not configured';
            RAISE NOTICE '   Run: ALTER DATABASE postgres SET app.service_role_key = ''your-service-role-key'';';
        ELSE
            RAISE NOTICE '✅ Service role key configured (length: %)', LENGTH(service_key);
        END IF;
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE '❌ Service role key setting missing';
        RAISE NOTICE '   Run: ALTER DATABASE postgres SET app.service_role_key = ''your-service-role-key'';';
    END;
END $$;

-- ========================================
-- STEP 6: Check recent notification attempts
-- ========================================
DO $$
DECLARE
    recent_count INTEGER;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '📋 CHECKING RECENT NOTIFICATIONS...';
    
    SELECT COUNT(*) INTO recent_count 
    FROM notification_send_log 
    WHERE sent_at > NOW() - INTERVAL '24 hours';
    
    RAISE NOTICE '📊 Notifications attempted in last 24 hours: %', recent_count;
END $$;

-- Show recent notification attempts
SELECT 
    id,
    title,
    body,
    type,
    success_count,
    failure_count,
    sent_at,
    array_length(user_ids, 1) as target_users,
    item_id,
    uploader_id
FROM notification_send_log 
ORDER BY sent_at DESC 
LIMIT 5;

-- ========================================
-- STEP 7: Check recent items that should trigger notifications
-- ========================================
SELECT 
    id,
    title,
    main_category,
    owner_id,
    location,
    is_active,
    created_at
FROM items 
WHERE created_at > NOW() - INTERVAL '24 hours'
AND is_active = true
ORDER BY created_at DESC 
LIMIT 5;

-- ========================================
-- STEP 8: Create test functions for debugging
-- ========================================

-- Function to test trigger manually
CREATE OR REPLACE FUNCTION test_notification_trigger()
RETURNS TEXT AS $$
DECLARE
    test_item_id UUID := gen_random_uuid();
    user_id UUID;
    result TEXT := '';
BEGIN
    -- Get a random user ID (or create test user)
    SELECT id INTO user_id FROM auth.users LIMIT 1;
    
    IF user_id IS NULL THEN
        RETURN '❌ No users found in auth.users table. Create a user first.';
    END IF;
    
    -- Insert a test item to trigger the notification
    BEGIN
        INSERT INTO items (
            id,
            title, 
            description, 
            main_category, 
            sub_category,
            owner_id, 
            location, 
            is_active,
            created_at
        ) VALUES (
            test_item_id,
            'TEST: Push Notification Test Item', 
            'This is a test item to trigger push notifications', 
            'Electronics', 
            'Testing',
            user_id,
            'Test Location', 
            true,
            NOW()
        );
        
        result := '✅ Test item created with ID: ' || test_item_id::TEXT || E'\n';
        result := result || '📱 Check notification_send_log table for results' || E'\n';
        result := result || '👤 Uploader ID: ' || user_id::TEXT;
        
        RETURN result;
        
    EXCEPTION WHEN OTHERS THEN
        RETURN '❌ Error creating test item: ' || SQLERRM;
    END;
END;
$$ LANGUAGE plpgsql;

-- Function to manually send test notification
CREATE OR REPLACE FUNCTION send_manual_test_notification()
RETURNS TEXT AS $$
DECLARE
    user_ids_array UUID[];
    result TEXT;
BEGIN
    -- Get all users with active device tokens
    SELECT ARRAY_AGG(DISTINCT user_id) INTO user_ids_array
    FROM user_device_tokens 
    WHERE is_active = true;
    
    IF user_ids_array IS NULL OR array_length(user_ids_array, 1) = 0 THEN
        RETURN '❌ No active device tokens found. Users need to login to app.';
    END IF;
    
    -- Call manual notification function
    SELECT manual_send_push_notification(
        user_ids_array,
        'TEST: Manual Push Notification 🧪',
        'If you see this, your push notification system is working!',
        'manual_test',
        '{"test": true, "timestamp": "' || NOW()::TEXT || '"}'::jsonb
    )::TEXT INTO result;
    
    RETURN '✅ Manual test sent to ' || array_length(user_ids_array, 1) || ' users. Result: ' || result;
END;
$$ LANGUAGE plpgsql;

-- ========================================
-- FINAL SUMMARY AND NEXT STEPS
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🎯 DIAGNOSTIC COMPLETE!';
    RAISE NOTICE '';
    RAISE NOTICE '📋 NEXT STEPS TO TEST:';
    RAISE NOTICE '1. Fix any ❌ errors shown above';
    RAISE NOTICE '2. Login to your app to register FCM token';
    RAISE NOTICE '3. Test manual notification: SELECT send_manual_test_notification();';
    RAISE NOTICE '4. Test trigger: SELECT test_notification_trigger();';
    RAISE NOTICE '5. Check results: SELECT * FROM notification_send_log ORDER BY sent_at DESC LIMIT 3;';
    RAISE NOTICE '';
    RAISE NOTICE '🚨 COMMON ISSUES:';
    RAISE NOTICE '- FCM_SERVER_KEY not set in Supabase environment variables';
    RAISE NOTICE '- Edge function not deployed';
    RAISE NOTICE '- HTTP extension not enabled';
    RAISE NOTICE '- Database settings not configured';
    RAISE NOTICE '- No device tokens (users need to login to app)';
    RAISE NOTICE '';
    RAISE NOTICE '📱 REMEMBER: Test on physical device, not emulator!';
END $$;
