-- Fix duplicate device token constraint issues
-- Run this in your Supabase SQL editor if you continue to have duplicate key issues

-- 1. First, let's see if there are any duplicate entries
SELECT user_id, device_token, COUNT(*) as count
FROM user_device_tokens 
GROUP BY user_id, device_token 
HAVING COUNT(*) > 1;

-- 2. Remove duplicate entries (keep the most recent one)
WITH ranked_tokens AS (
  SELECT id, user_id, device_token, created_at,
         ROW_NUMBER() OVER (PARTITION BY user_id, device_token ORDER BY created_at DESC) as rn
  FROM user_device_tokens
)
DELETE FROM user_device_tokens 
WHERE id IN (
  SELECT id FROM ranked_tokens WHERE rn > 1
);

-- 3. Create or replace the upsert function to handle conflicts
CREATE OR REPLACE FUNCTION upsert_device_token(
  p_user_id UUID,
  p_device_token TEXT,
  p_platform TEXT DEFAULT 'android'
)
RETURNS VOID AS $$
BEGIN
  -- First try to update existing token
  UPDATE user_device_tokens 
  SET 
    is_active = true,
    updated_at = NOW(),
    platform = p_platform
  WHERE user_id = p_user_id AND device_token = p_device_token;
  
  -- If no rows were updated, insert new token
  IF NOT FOUND THEN
    -- Deactivate all other tokens for this user first
    UPDATE user_device_tokens 
    SET is_active = false, updated_at = NOW()
    WHERE user_id = p_user_id AND is_active = true;
    
    -- Insert the new token
    INSERT INTO user_device_tokens (user_id, device_token, platform, is_active)
    VALUES (p_user_id, p_device_token, p_platform, true)
    ON CONFLICT (user_id, device_token) DO UPDATE SET
      is_active = true,
      updated_at = NOW(),
      platform = p_platform;
  END IF;
END;
$$ LANGUAGE plpgsql;

-- 4. Grant execute permission to authenticated users
GRANT EXECUTE ON FUNCTION upsert_device_token(UUID, TEXT, TEXT) TO authenticated;

-- 5. Optional: Add RLS policy for the function if needed
-- ALTER FUNCTION upsert_device_token(UUID, TEXT, TEXT) SECURITY DEFINER;

-- Usage example (you don't need to run this):
-- SELECT upsert_device_token('user-uuid-here'::UUID, 'device-token-here', 'android');
