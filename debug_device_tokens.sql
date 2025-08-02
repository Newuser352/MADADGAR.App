-- Debug script for device token registration issues
-- Run this in your Supabase SQL Editor

-- ========================================
-- STEP 1: Check table structure
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '🔍 CHECKING USER_DEVICE_TOKENS TABLE...';
    RAISE NOTICE '';
END $$;

-- Check if table exists and show structure
SELECT 
    column_name,
    data_type,
    is_nullable,
    column_default
FROM information_schema.columns
WHERE table_name = 'user_device_tokens'
ORDER BY ordinal_position;

-- ========================================
-- STEP 2: Check RLS policies
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🔐 CHECKING ROW LEVEL SECURITY...';
END $$;

-- Check if RLS is enabled
SELECT 
    relname as table_name,
    relrowsecurity as rls_enabled,
    relforcerowsecurity as rls_forced
FROM pg_class
WHERE relname = 'user_device_tokens';

-- List all policies
SELECT 
    policyname as policy_name,
    permissive,
    roles,
    cmd as operation,
    qual as using_expression,
    with_check
FROM pg_policies
WHERE tablename = 'user_device_tokens';

-- ========================================
-- STEP 3: Check current data
-- ========================================
DO $$
DECLARE
    token_count INTEGER;
    user_count INTEGER;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '📊 CHECKING EXISTING DATA...';
    
    SELECT COUNT(*) INTO token_count FROM user_device_tokens;
    SELECT COUNT(DISTINCT user_id) INTO user_count FROM user_device_tokens;
    
    RAISE NOTICE 'Total tokens: %', token_count;
    RAISE NOTICE 'Unique users: %', user_count;
END $$;

-- Show all tokens (if any)
SELECT * FROM user_device_tokens ORDER BY created_at DESC;

-- ========================================
-- STEP 4: Test direct insert (as service role)
-- ========================================
DO $$
DECLARE
    test_user_id UUID;
    insert_success BOOLEAN := false;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🧪 TESTING DIRECT INSERT...';
    
    -- Get a test user ID
    SELECT id INTO test_user_id FROM auth.users LIMIT 1;
    
    IF test_user_id IS NULL THEN
        RAISE NOTICE '❌ No users found in auth.users table';
        RETURN;
    END IF;
    
    RAISE NOTICE 'Test user ID: %', test_user_id;
    
    -- Try to insert a test token
    BEGIN
        INSERT INTO user_device_tokens (
            user_id, 
            device_token, 
            platform, 
            is_active
        ) VALUES (
            test_user_id,
            'test_token_' || NOW()::TEXT,
            'android',
            true
        );
        insert_success := true;
        RAISE NOTICE '✅ Direct insert successful!';
    EXCEPTION WHEN OTHERS THEN
        RAISE NOTICE '❌ Direct insert failed: %', SQLERRM;
    END;
    
    -- If successful, check if it was saved
    IF insert_success THEN
        PERFORM * FROM user_device_tokens WHERE user_id = test_user_id AND device_token LIKE 'test_token_%';
        IF FOUND THEN
            RAISE NOTICE '✅ Token was saved and can be retrieved';
        ELSE
            RAISE NOTICE '❌ Token was inserted but cannot be retrieved (RLS issue?)';
        END IF;
    END IF;
END $$;

-- ========================================
-- STEP 5: Fix RLS policies
-- ========================================
DO $$
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🔧 FIXING RLS POLICIES...';
END $$;

-- Drop existing policies
DROP POLICY IF EXISTS "Users can manage their own device tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Service role can manage all tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Users can insert their own tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Users can update their own tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Users can view their own tokens" ON user_device_tokens;
DROP POLICY IF EXISTS "Service role has full access" ON user_device_tokens;

-- Create comprehensive policies
CREATE POLICY "Users can insert their own tokens"
    ON user_device_tokens
    FOR INSERT
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can view their own tokens"
    ON user_device_tokens
    FOR SELECT
    USING (auth.uid() = user_id);

CREATE POLICY "Users can update their own tokens"
    ON user_device_tokens
    FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can delete their own tokens"
    ON user_device_tokens
    FOR DELETE
    USING (auth.uid() = user_id);

CREATE POLICY "Service role has full access"
    ON user_device_tokens
    FOR ALL
    TO service_role
    USING (true)
    WITH CHECK (true);

-- Also create a policy for authenticated users to insert
CREATE POLICY "Authenticated users can manage tokens"
    ON user_device_tokens
    FOR ALL
    TO authenticated
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

RAISE NOTICE '✅ RLS policies updated!';

-- ========================================
-- STEP 6: Create helper function for easier token management
-- ========================================
CREATE OR REPLACE FUNCTION upsert_device_token(
    p_user_id UUID,
    p_device_token TEXT,
    p_platform TEXT DEFAULT 'android'
)
RETURNS BOOLEAN AS $$
BEGIN
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

-- Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION upsert_device_token TO authenticated;

-- ========================================
-- STEP 7: Test with the new function
-- ========================================
DO $$
DECLARE
    test_user_id UUID;
    result BOOLEAN;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '🧪 TESTING NEW UPSERT FUNCTION...';
    
    -- Get a test user ID
    SELECT id INTO test_user_id FROM auth.users LIMIT 1;
    
    IF test_user_id IS NOT NULL THEN
        SELECT upsert_device_token(test_user_id, 'test_upsert_token_' || NOW()::TEXT) INTO result;
        
        IF result THEN
            RAISE NOTICE '✅ Upsert function worked successfully!';
        ELSE
            RAISE NOTICE '❌ Upsert function failed';
        END IF;
    END IF;
END $$;

-- ========================================
-- FINAL CHECK
-- ========================================
DO $$
DECLARE
    final_count INTEGER;
BEGIN
    RAISE NOTICE '';
    RAISE NOTICE '📋 FINAL STATUS:';
    
    SELECT COUNT(*) INTO final_count FROM user_device_tokens WHERE is_active = true;
    RAISE NOTICE '✅ Active device tokens: %', final_count;
    
    RAISE NOTICE '';
    RAISE NOTICE '🎯 NEXT STEPS:';
    RAISE NOTICE '1. Check Android Studio logs when users login';
    RAISE NOTICE '2. Look for "FCM token retrieved" and "FCM token registered successfully"';
    RAISE NOTICE '3. If tokens are retrieved but not saved, check Supabase client configuration';
    RAISE NOTICE '4. Make sure google-services.json is correctly configured';
END $$;

-- Show final state of tokens
SELECT 
    user_id,
    LEFT(device_token, 30) || '...' as token_preview,
    platform,
    is_active,
    created_at,
    updated_at
FROM user_device_tokens 
ORDER BY created_at DESC;
