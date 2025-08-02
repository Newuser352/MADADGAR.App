# Live Push Notifications Setup Guide

## 🎯 Goal
Enable users to receive push notifications on their phones when new posts are created, even when the app is closed.

## ✅ Current Status
Your app already has all the code components needed:
- FCM Service (receives notifications)
- FCM Token Manager (manages device registration)
- Supabase Edge Function (sends notifications via FCM)
- Database trigger (auto-triggers on new posts)

## 🔧 Setup Steps Required

### Step 1: Complete Supabase Configuration

1. **Get your Supabase project details:**
   - Go to your Supabase dashboard
   - Note your Project URL (e.g., `https://abc123.supabase.co`)
   - Get your Service Role Key from Settings → API

2. **Update SupabaseClient.kt with real credentials:**
   ```kotlin
   // Update these values in SupabaseClient.kt
   private const val SUPABASE_URL = "https://your-actual-project.supabase.co"
   private const val SUPABASE_ANON_KEY = "your-actual-anon-key"
   ```

### Step 2: Enable HTTP Extension in Supabase

1. Go to Supabase Dashboard → Database → Extensions
2. Search for "http" and enable it
3. This allows your database triggers to make HTTP requests

### Step 3: Deploy the Supabase Edge Function

1. **Install Supabase CLI:**
   ```bash
   npm install -g supabase
   ```

2. **Login to Supabase:**
   ```bash
   supabase login
   ```

3. **Create the function structure:**
   ```bash
   mkdir -p supabase/functions/send-push-notifications
   ```

4. **Copy the function code:**
   - Copy `supabase_edge_function_send_push_notifications.ts` to `supabase/functions/send-push-notifications/index.ts`

5. **Deploy the function:**
   ```bash
   supabase functions deploy send-push-notifications --project-ref your-project-id
   ```

### Step 4: Configure Firebase Cloud Messaging

1. **Get FCM Server Key:**
   - Go to Firebase Console → Project Settings → Cloud Messaging
   - Copy the "Server key" (legacy)

2. **Set environment variable in Supabase:**
   ```bash
   supabase secrets set FCM_SERVER_KEY=your-fcm-server-key --project-ref your-project-id
   ```

3. **Verify your google-services.json is correct:**
   - Make sure it matches your Firebase project
   - Ensure it's in `app/` directory

### Step 5: Update Database Configuration

Run this SQL in your Supabase SQL editor:

```sql
-- Set Supabase URL setting for triggers
ALTER DATABASE postgres SET app.supabase_url = 'https://your-project-id.supabase.co';

-- Set service role key setting for triggers  
ALTER DATABASE postgres SET app.service_role_key = 'your-service-role-key';
```

### Step 6: Apply Database Triggers

Execute these SQL files in order:
1. `user_notifications_schema.sql`
2. `auto_push_notifications_trigger.sql`

### Step 7: Test the System

1. **Test FCM token registration:**
   - Login to app
   - Check logs for "FCM token registered successfully"

2. **Test manual notification:**
   ```sql
   SELECT manual_send_push_notification(
       ARRAY['user-uuid']::UUID[],
       'Test Notification',
       'This is a test push notification',
       'test'
   );
   ```

3. **Test automatic notifications:**
   - Create a new post from one device
   - Check if other users receive push notifications

## 🔍 Troubleshooting

### Common Issues:

**1. No notifications received:**
- Check if FCM token is registered in `user_device_tokens` table
- Verify FCM server key is correct
- Check device notification permissions

**2. Edge function errors:**
- Check Supabase function logs
- Verify FCM_SERVER_KEY environment variable is set
- Ensure HTTP extension is enabled

**3. Trigger not firing:**
- Check if items are being inserted with `is_active = true`
- Verify trigger exists: `\d+ items` in SQL editor
- Check function logs for errors

### Debug Commands:

```sql
-- Check registered device tokens
SELECT * FROM user_device_tokens WHERE is_active = true;

-- Check notification send logs
SELECT * FROM notification_send_log ORDER BY sent_at DESC LIMIT 5;

-- Test trigger manually
INSERT INTO items (title, description, main_category, owner_id, location, is_active) 
VALUES ('Test Item', 'Test Description', 'Electronics', 'user-uuid', 'Test Location', true);
```

## 📱 How It Works

1. **User creates new post** → Database `INSERT` trigger fires
2. **Trigger gets all device tokens** → Excludes uploader
3. **Trigger calls Edge Function** → Sends FCM payload
4. **Edge Function sends to FCM** → Firebase routes to devices
5. **FCMService receives message** → Shows notification on device
6. **User taps notification** → Opens app

## 🎯 Next Steps

1. Complete the Supabase and Firebase configuration above
2. Test with a simple manual notification
3. Create a test post to verify automatic notifications
4. Monitor logs for any errors

Once setup is complete, users will automatically receive push notifications on their phones whenever new posts are created!
