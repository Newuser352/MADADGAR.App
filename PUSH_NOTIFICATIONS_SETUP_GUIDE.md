# 📱 Push Notifications Setup Guide

## Overview
This guide will help you set up **real push notifications** that appear on users' phones even when the app is closed. When a user uploads a new post, all other users will receive a push notification on their devices.

## 🏗️ Architecture Overview

**Flow:**
1. User uploads new post → Item inserted into database
2. Database trigger automatically calls Supabase Edge Function  
3. Edge Function fetches device tokens and sends FCM notifications
4. Users receive push notifications on their phones
5. Users tap notification → App opens to the new post

## 📋 Step-by-Step Setup

### **Step 1: Get Firebase Server Key**

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your MADADGAR project
3. Click **Project Settings** (gear icon)
4. Go to **Cloud Messaging** tab
5. Copy the **Server key** (starts with `AAAA...`)

### **Step 2: Deploy Supabase Edge Function**

1. **Install Supabase CLI** (if not already installed):
   ```bash
   npm install -g supabase
   ```

2. **Initialize Supabase in your project**:
   ```bash
   cd /path/to/your/project
   supabase init
   ```

3. **Create the Edge Function**:
   ```bash
   supabase functions new send-push-notifications
   ```

4. **Copy the function code**:
   - Copy the contents of `supabase_edge_function_send_push_notifications.ts`
   - Paste into `supabase/functions/send-push-notifications/index.ts`

5. **Deploy the function**:
   ```bash
   supabase functions deploy send-push-notifications
   ```

### **Step 3: Configure Environment Variables**

1. Go to **Supabase Dashboard** → **Settings** → **Edge Functions**
2. Add environment variable:
   - **Key**: `FCM_SERVER_KEY`  
   - **Value**: Your Firebase server key from Step 1

### **Step 4: Setup Database**

1. **Enable HTTP Extension**:
   - Go to **Supabase Dashboard** → **Database** → **Extensions**
   - Enable `http` extension

2. **Run the database scripts**:
   
   **First, run the user_notifications table setup:**
   ```sql
   -- Copy and paste the complete SQL from earlier (user_notifications table creation)
   ```

   **Then, run the push notifications trigger:**
   - Copy the contents of `auto_push_notifications_trigger.sql`
   - Paste into Supabase SQL Editor and run

3. **Update the function URL**:
   - In the SQL script, replace `your-project-id` with your actual Supabase project ID
   - Your URL should look like: `https://abcdefghijklmnop.supabase.co`

### **Step 5: Verify App Configuration**

Your app should already have FCM configured, but let's verify:

1. **Check AndroidManifest.xml**:
   ```xml
   <service
       android:name=".services.FCMService"
       android:exported="false">
       <intent-filter>
           <action android:name="com.google.firebase.MESSAGING_EVENT" />
       </intent-filter>
   </service>
   ```

2. **Verify FCM is initialized in MainActivity**:
   - FCM tokens should be automatically registered when users log in

### **Step 6: Test the Setup**

1. **Test with SQL function**:
   ```sql
   -- Replace with actual user UUIDs from your database
   SELECT manual_send_push_notification(
       ARRAY['user-uuid-1', 'user-uuid-2']::UUID[],
       'Test Notification',
       'This is a test push notification!',
       'test'
   );
   ```

2. **Test with new post**:
   - Have one user upload a new post
   - Check that other users receive push notifications

## 🔧 Configuration Files

### **Required Files:**

1. **`supabase/functions/send-push-notifications/index.ts`** - Edge function
2. **Database tables and triggers** - Applied via SQL scripts
3. **App FCM configuration** - Already configured in your app

### **Project Structure:**
```
your-project/
├── supabase/
│   └── functions/
│       └── send-push-notifications/
│           └── index.ts
├── app/src/main/java/com/example/madadgarapp/
│   └── services/
│       └── FCMService.kt (already exists)
└── SQL scripts (run in Supabase Dashboard)
```

## 📱 How Push Notifications Work

### **When App is Open:**
- FCMService receives the notification
- Shows notification in notification bar
- Also saves to local database

### **When App is Closed:**
- Device receives FCM push notification
- Android system shows notification
- User taps → App opens with notification data

### **Notification Content:**
- **Title**: "New Electronics Available!" (dynamic based on category)
- **Body**: "iPhone 13 has been shared in New York" (dynamic)
- **Data**: Contains item_id, uploader_id, category, etc.

## 🎯 Testing Scenarios

### **Test 1: Basic Push Notification**
1. User A uploads a post
2. User B should receive push notification (even if app is closed)
3. User A should NOT receive notification (self-notification prevention)

### **Test 2: Notification Data**
1. User B taps the notification
2. App should open with item details
3. Check logs for notification data

### **Test 3: Multiple Users**
1. User A uploads post
2. Users B, C, D should all receive notifications
3. Check database logs for success/failure counts

## 🚨 Troubleshooting

### **No notifications received:**
1. Check FCM server key is correct
2. Verify Edge Function is deployed
3. Check device tokens are saved in database
4. Test with manual SQL function

### **Edge Function fails:**
1. Check Supabase logs: **Functions** → **send-push-notifications** → **Logs**
2. Verify FCM_SERVER_KEY environment variable
3. Check HTTP extension is enabled

### **Database trigger not firing:**
1. Check trigger exists: `\dt public.items`
2. Verify function exists: `\df send_new_item_push_notifications`
3. Check logs in notification_send_log table

### **App doesn't handle notifications:**
1. Verify FCMService is registered in AndroidManifest.xml
2. Check notification permissions are granted
3. Test with app in background vs foreground

## 📊 Monitoring

### **Check Push Notification Logs:**
```sql
-- View recent push notification attempts
SELECT * FROM notification_send_log 
ORDER BY sent_at DESC 
LIMIT 10;

-- Check success rates
SELECT 
    type,
    COUNT(*) as total_attempts,
    SUM(success_count) as total_sent,
    SUM(failure_count) as total_failed
FROM notification_send_log 
GROUP BY type;
```

### **Check Device Tokens:**
```sql
-- View active device tokens
SELECT user_id, platform, is_active, created_at 
FROM user_device_tokens 
WHERE is_active = true 
ORDER BY created_at DESC;
```

### **Check Recent Notifications:**
```sql
-- View recent in-app notifications
SELECT user_id, type, title, is_read, created_at 
FROM user_notifications 
ORDER BY created_at DESC 
LIMIT 20;
```

## ✅ Success Checklist

- [ ] Firebase server key obtained and configured
- [ ] Supabase Edge Function deployed successfully  
- [ ] FCM_SERVER_KEY environment variable set
- [ ] HTTP extension enabled in Supabase
- [ ] Database trigger created and active
- [ ] Project URLs updated with correct Supabase project ID
- [ ] Test push notification sent and received
- [ ] Self-notification prevention working
- [ ] App handles notification taps correctly

## 🔒 Security Notes

- FCM server key is stored securely as environment variable
- Database trigger has SECURITY DEFINER for proper permissions
- RLS policies protect notification logs
- Self-notification prevention active at multiple levels

## 🚀 Production Deployment

Once testing is complete:

1. **Monitor logs** for the first few days
2. **Scale considerations**: Current setup handles 1000s of users efficiently  
3. **Optimization**: Consider batch processing for very large user bases
4. **Analytics**: Track notification open rates and effectiveness

---

**Status**: 📱 **Ready for Push Notifications!**  
**Next Step**: Complete the setup steps above and test with real devices!
