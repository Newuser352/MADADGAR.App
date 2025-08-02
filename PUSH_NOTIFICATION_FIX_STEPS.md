# 🔧 Push Notification Fix - Action Plan

## Issue Summary
Your push notifications are not working when users share new posts because:
1. The database configuration is incomplete
2. The Supabase Edge Function may not be deployed
3. FCM server key might not be configured
4. Device tokens might not be registering properly

## Step-by-Step Fix Instructions

### 📋 Step 1: Get Your Supabase Credentials

1. **Go to your Supabase Dashboard**
2. Navigate to **Settings → API**
3. Copy these values:
   - **Project URL**: `https://YOUR-PROJECT-ID.supabase.co`
   - **Service Role Key**: (starts with `eyJ...`)

### 🗄️ Step 2: Fix Database Configuration

1. **Open Supabase SQL Editor**
2. **Run the fix script** (`fix_push_notifications.sql`)
3. **Update the database settings** - Find these lines in the script and uncomment them with your actual values:
   ```sql
   ALTER DATABASE postgres SET app.supabase_url = 'https://YOUR-PROJECT-ID.supabase.co';
   ALTER DATABASE postgres SET app.service_role_key = 'YOUR-SERVICE-ROLE-KEY';
   ```

### 🔑 Step 3: Configure Firebase Cloud Messaging

1. **Go to Firebase Console** → Your Project → Project Settings → Cloud Messaging
2. **Copy the Server Key** (Legacy server key)
3. **Set it in Supabase**:
   - Go to Supabase Dashboard → Edge Functions
   - Add environment variable:
     - Key: `FCM_SERVER_KEY`
     - Value: Your Firebase server key

### 📱 Step 4: Deploy the Edge Function

1. **Open terminal in your project directory**
2. **Install Supabase CLI** (if not already):
   ```bash
   npm install -g supabase
   ```
3. **Login to Supabase**:
   ```bash
   supabase login
   ```
4. **Create function directory**:
   ```bash
   mkdir -p supabase/functions/send-push-notifications
   ```
5. **Copy the edge function**:
   ```bash
   copy supabase_edge_function_send_push_notifications.ts supabase\functions\send-push-notifications\index.ts
   ```
6. **Deploy the function**:
   ```bash
   supabase functions deploy send-push-notifications --project-ref YOUR-PROJECT-ID
   ```

### 🧪 Step 5: Test the Setup

1. **Check if device tokens are registering**:
   ```sql
   SELECT * FROM user_device_tokens WHERE is_active = true;
   ```
   
2. **Run the test function**:
   ```sql
   SELECT test_notifications_now();
   ```

3. **Test with a real post**:
   - Login to app on two different devices/users
   - Create a new post from one device
   - The other device should receive a notification

### 🔍 Step 6: Debug Common Issues

#### Issue: No device tokens in database
**Solution**: 
- Make sure users are logged in
- Check Android Studio Logcat for "FCM token registered successfully"
- Verify `google-services.json` is in the `app/` directory

#### Issue: Edge function returns error
**Solution**:
- Check Supabase function logs:
  ```bash
  supabase functions logs send-push-notifications --project-ref YOUR-PROJECT-ID
  ```
- Verify FCM_SERVER_KEY is set correctly
- Ensure HTTP extension is enabled in Supabase

#### Issue: Database trigger not firing
**Solution**:
- Check if the trigger exists:
  ```sql
  SELECT trigger_name FROM information_schema.triggers 
  WHERE trigger_name = 'trigger_send_new_item_push_notifications';
  ```
- Verify items are inserted with `is_active = true`

### 📊 Step 7: Monitor Notifications

Check notification logs regularly:
```sql
-- View recent notification attempts
SELECT * FROM notification_send_log 
ORDER BY sent_at DESC 
LIMIT 10;

-- Check success rate
SELECT 
    date_trunc('hour', sent_at) as hour,
    COUNT(*) as attempts,
    SUM(success_count) as successes,
    SUM(failure_count) as failures
FROM notification_send_log 
WHERE sent_at > NOW() - INTERVAL '24 hours'
GROUP BY hour
ORDER BY hour DESC;
```

## ✅ Success Checklist

- [ ] Database settings configured with actual Supabase URL and service key
- [ ] HTTP extension enabled in Supabase
- [ ] Edge function deployed successfully
- [ ] FCM_SERVER_KEY environment variable set
- [ ] Device tokens appearing in `user_device_tokens` table
- [ ] Test notification sent successfully
- [ ] New posts trigger notifications to other users
- [ ] Notifications appear on physical devices

## 🚨 Important Notes

1. **Test on physical devices** - Push notifications may not work on emulators
2. **Ensure notification permissions** are granted on devices
3. **The uploader will NOT receive notifications** for their own posts (by design)
4. **Check your Firebase project** matches the `google-services.json` in your app

## 📞 Quick Commands Reference

```sql
-- Check device tokens
SELECT user_id, LEFT(device_token, 20) || '...' as token, is_active, created_at 
FROM user_device_tokens ORDER BY created_at DESC;

-- Send test notification to all users
SELECT manual_send_push_notification(
    ARRAY(SELECT user_id FROM user_device_tokens WHERE is_active = true)::UUID[],
    'Test Notification 📱',
    'Testing push notifications!',
    'test'
);

-- Check recent logs
SELECT * FROM notification_send_log ORDER BY sent_at DESC LIMIT 5;

-- Test the trigger with a fake post
INSERT INTO items (title, description, main_category, owner_id, location, is_active) 
VALUES ('Test Item', 'Test', 'Electronics', 
        (SELECT id FROM auth.users LIMIT 1), 'Test Location', true);
```

Once you complete these steps, your push notifications should start working! 🎉
