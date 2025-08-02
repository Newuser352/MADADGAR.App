-- Quick Fix for Device Token Registration Issue
-- Run this in your Supabase SQL Editor to fix the empty user_device_tokens table

-- ========================================
-- STEP 1: Disable RLS temporarily to diagnose
-- ========================================
ALTER TABLE user_device_tokens DISABLE ROW LEVEL SECURITY;

-- ========================================
-- STEP 2: Create proper RLS policies
-- ========================================
-- Drop all existing policies first
DROP POLICY IF EXISTS "Users can manage their own device tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Service role can manage all tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Users can insert their own tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Users can update their own tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Users can view their own tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Service role has full access" ON user_device_tokens;
DROP POLICY IF EXISTS "Authenticated users can manage tokens" ON user_device_tokens;

-- Re-enable RLS
ALTER TABLE user_device_tokens ENABLE ROW LEVEL SECURITY;

-- Create a simple policy that allows authenticated users to manage their own tokens
CREATE POLICY "Enable all operations for users on their own tokens"
    ON user_device_tokens
    FOR ALL
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

-- ========================================
-- STEP 3: Create or update the upsert function
-- ========================================
CREATE OR REPLACE FUNCTION upsert_device_token(
    p_user_id UUID,
    p_device_token TEXT,
    p_platform TEXT DEFAULT 'android'
)
RETURNS BOOLEAN AS $$
BEGIN
    -- Log the attempt
    RAISE NOTICE 'Upserting device token for user: %', p_user_id;
    
    -- First, deactivate any existing tokens for this user
    UPDATE user_device_tokens 
    SET is_active = false, updated_at = NOW()
    WHERE user_id = p_user_id AND is_active = true;
    
    -- Insert or update the token
    INSERT INTO user_device_tokens (user_id, device_token, platform, is_active, created_at, updated_at)
    VALUES (p_user_id, p_device_token, p_platform, true, NOW(), NOW())
    ON CONFLICT (user_id, device_token) 
    DO UPDATE SET 
        is_active = true,
        updated_at = NOW();
    
    RETURN true;
EXCEPTION WHEN OTHERS THEN
    RAISE WARNING 'Error upserting device token: %', SQLERRM;
    RETURN false;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Grant execute permission
GRANT EXECUTE ON FUNCTION upsert_device_token TO authenticated;
GRANT EXECUTE ON FUNCTION upsert_device_token TO anon;

-- ========================================
-- STEP 4: Create a direct insert function for testing
-- ========================================
CREATE OR REPLACE FUNCTION test_direct_token_insert()
RETURNS TEXT AS $$
DECLARE
    test_user_id UUID;
    result TEXT := '';
BEGIN
    -- Get the first user
    SELECT id INTO test_user_id FROM auth.users LIMIT 1;
    
    IF test_user_id IS NULL THEN
        RETURN 'No users found. Please login to the app first.';
    END IF;
    
    -- Try direct insert without RLS
    BEGIN
        -- Temporarily disable RLS for this transaction
        SET LOCAL row_security TO OFF;
        
        INSERT INTO user_device_tokens (
            user_id, 
            device_token, 
            platform, 
            is_active,
            created_at,
            updated_at
        ) VALUES (
            test_user_id,
            'test_token_' || extract(epoch from now())::text,
            'android',
            true,
            NOW(),
            NOW()
        );
        
        result := '✅ Test token inserted successfully for user: ' || test_user_id::text;
    EXCEPTION WHEN OTHERS THEN
        result := '❌ Error inserting test token: ' || SQLERRM;
    END;
    
    RETURN result;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- ========================================
-- STEP 5: Check Android app logs
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '📱 TROUBLESHOOTING STEPS:';
    RAISE NOTICE '';
    RAISE NOTICE '1. Run this script in Supabase SQL Editor';
    RAISE NOTICE '2. Open Android Studio and clear app data';
    RAISE NOTICE '3. Run the app and login';
    RAISE NOTICE '4. Check Logcat for these messages:';
    RAISE NOTICE '   - "FCM token retrieved: [token]"';
    RAISE NOTICE '   - "FCM token registered successfully"';
    RAISE NOTICE '   OR';
    RAISE NOTICE '   - "Failed to register FCM token: [error]"';
    RAISE NOTICE '';
    RAISE NOTICE '5. If you see "Failed to register", the error message will tell you what''s wrong';
    RAISE NOTICE '';
    RAISE NOTICE '6. Check tokens with: SELECT * FROM user_device_tokens;';
END $$;

-- ========================================
-- STEP 6: Test the setup
-- ========================================
-- Test direct insert
SELECT test_direct_token_insert() as direct_insert_test;

-- Check current tokens
SELECT 
    user_id,
    LEFT(device_token, 30) || '...' as token_preview,
    platform,
    is_active,
    created_at
FROM user_device_tokens 
ORDER BY created_at DESC;

-- ========================================
-- FINAL: Create a simpler insert approach
-- ========================================
-- This function bypasses complex logic and just inserts
CREATE OR REPLACE FUNCTION simple_upsert_token(
    p_user_id UUID,
    p_token TEXT
)
RETURNS VOID AS $$
BEGIN
    -- Delete old tokens
    DELETE FROM user_device_tokens WHERE user_id = p_user_id;
    
    -- Insert new token
    INSERT INTO user_device_tokens (user_id, device_token, platform, is_active)
    VALUES (p_user_id, p_token, 'android', true);
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

GRANT EXECUTE ON FUNCTION simple_upsert_token TO authenticated;
GRANT EXECUTE ON FUNCTION simple_upsert_token TO anon;
