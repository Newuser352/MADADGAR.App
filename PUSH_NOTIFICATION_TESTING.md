# Push Notification Testing Guide

## 🧪 Testing Your Push Notification Setup

After completing the setup steps, use this guide to test if your push notifications are working correctly.

## ✅ Pre-Test Checklist

Before testing, ensure:
- [ ] Supabase project is configured with real URLs and keys
- [ ] FCM server key is set in Supabase environment variables
- [ ] HTTP extension is enabled in Supabase
- [ ] Database triggers are applied
- [ ] Edge function is deployed
- [ ] App has notification permissions

## 🔧 Step-by-Step Testing

### Step 1: Check FCM Token Registration

1. **Open your app and login**
2. **Check Android Studio Logcat for these messages:**
   ```
   FCMTokenManager: FCM token retrieved: [token]
   FCMTokenManager: FCM token registered successfully
   ```

3. **Verify in Supabase database:**
   ```sql
   SELECT user_id, device_token, is_active, created_at 
   FROM user_device_tokens 
   WHERE is_active = true;
   ```

### Step 2: Test Manual Push Notification

Run this SQL in Supabase SQL Editor:

```sql
-- Test push notification to all users
SELECT manual_send_push_notification(
    ARRAY(SELECT user_id FROM user_device_tokens WHERE is_active = true)::UUID[],
    'Test Push Notification 📱',
    'This is a test notification from your MADADGAR app!',
    'test',
    '{"test": "true", "source": "manual"}'::jsonb
);
```

**Expected Result:** All logged-in users should receive a push notification.

### Step 3: Test Automatic Notifications (New Post)

1. **Login with User A** on Device 1
2. **Login with User B** on Device 2  
3. **User A creates a new post**
4. **User B should receive push notification** about the new post

You can also test by directly inserting into the database:

```sql
-- Insert test item (replace user-uuid with actual user ID)
INSERT INTO items (
    title, 
    description, 
    main_category, 
    sub_category,
    owner_id, 
    location, 
    is_active,
    created_at
) VALUES (
    'Test iPhone 13', 
    'Testing push notifications', 
    'Electronics', 
    'Phones',
    'your-user-uuid-here',  -- Replace with actual user ID
    'Test City, Test State', 
    true,
    NOW()
);
```

### Step 4: Monitor Notification Logs

Check if notifications are being sent:

```sql
-- View notification send logs
SELECT 
    id,
    title,
    body,
    type,
    success_count,
    failure_count,
    sent_at,
    user_ids
FROM notification_send_log 
ORDER BY sent_at DESC 
LIMIT 10;
```

## 🔍 Debugging Common Issues

### Issue 1: No FCM Token in Database
**Symptoms:** No entries in `user_device_tokens` table
**Solutions:**
- Check if user is properly authenticated
- Verify FCM dependencies in build.gradle
- Check `google-services.json` is in correct location
- Look for token registration errors in logs

### Issue 2: Edge Function Errors
**Symptoms:** Notifications not being sent, function errors in logs
**Solutions:**
```bash
# Check Supabase function logs
supabase functions logs send-push-notifications --project-ref your-project-id

# Verify environment variables
supabase secrets list --project-ref your-project-id
```

### Issue 3: FCM Server Key Issues
**Symptoms:** "Unauthorized" or "Invalid key" errors
**Solutions:**
- Get new server key from Firebase Console
- Update in Supabase secrets: `supabase secrets set FCM_SERVER_KEY=new-key`
- Verify Firebase project matches `google-services.json`

### Issue 4: Database Trigger Not Firing
**Symptoms:** New posts don't trigger notifications
**Solutions:**
```sql
-- Check if trigger exists
SELECT trigger_name, event_manipulation, event_object_table 
FROM information_schema.triggers 
WHERE trigger_name = 'trigger_send_new_item_push_notifications';

-- Check if HTTP extension is enabled
SELECT * FROM pg_extension WHERE extname = 'http';

-- Manual trigger test
SELECT send_new_item_push_notifications();
```

## 📱 Testing on Physical Device

1. **Install app on physical device** (emulator may not receive push notifications)
2. **Allow notification permissions** when prompted
3. **Test with app in background/closed** 
4. **Try different notification scenarios:**
   - App open in foreground
   - App in background
   - App completely closed

## 🎯 Expected Behavior

### When Working Correctly:
1. **User logs in** → FCM token registered
2. **New post created** → Database trigger fires
3. **Edge function called** → Sends FCM messages
4. **All users receive notification** (except post creator)
5. **Notification shows in system tray**
6. **Tapping notification opens app**
7. **Notification logged in database**

### Notification Content:
- **Title:** "New [Category] Available!"
- **Body:** "[Item Title] has been shared in [Location]"
- **Action:** Tapping opens app to main screen

## 🚀 Production Checklist

Before going live:
- [ ] Test with multiple devices
- [ ] Test with app in different states (foreground/background/closed)
- [ ] Verify notification permissions are properly requested
- [ ] Test notification throttling (avoid spam)
- [ ] Verify notifications don't go to post uploader
- [ ] Test with poor internet connectivity
- [ ] Monitor database performance with notification load

## 📞 Quick Debug Commands

```sql
-- Count active device tokens
SELECT COUNT(*) as active_tokens FROM user_device_tokens WHERE is_active = true;

-- Check recent notification attempts
SELECT * FROM notification_send_log WHERE sent_at > NOW() - INTERVAL '1 hour';

-- Test edge function directly (replace with your project URL)
SELECT http((
    'POST',
    'https://your-project.supabase.co/functions/v1/send-push-notifications',
    ARRAY[
        http_header('Authorization', 'Bearer your-service-key'),
        http_header('Content-Type', 'application/json')
    ],
    '{"userIds":["test-user-id"],"title":"Test","body":"Test notification","type":"test","data":{}}'
));
```

Once you confirm notifications are working in testing, your users will automatically receive push notifications whenever new posts are created! 🎉
