// Supabase Edge Function to send FCM push notifications
// Save this as: supabase/functions/send-push-notifications/index.ts

import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

interface NotificationPayload {
  userIds: string[]
  title: string
  body: string
  data: Record<string, string>
  type: string
}

interface DeviceToken {
  user_id: string
  device_token: string
  platform: string
  is_active: boolean
}

serve(async (req) => {
  // Handle CORS preflight requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
    console.log('🚀 Push notification function called')
    
    // Initialize Supabase client
    const supabaseUrl = Deno.env.get('SUPABASE_URL')!
    const supabaseServiceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
    const supabase = createClient(supabaseUrl, supabaseServiceKey)

    // Get FCM server key from environment variables
    const fcmServerKey = Deno.env.get('FCM_SERVER_KEY')
    if (!fcmServerKey) {
      throw new Error('FCM_SERVER_KEY environment variable not set')
    }

    // Parse request body
    const payload: NotificationPayload = await req.json()
    console.log('📝 Payload received:', JSON.stringify(payload, null, 2))

    // Validate required fields
    if (!payload.userIds || !payload.title || !payload.body) {
      return new Response(
        JSON.stringify({ error: 'Missing required fields: userIds, title, body' }),
        { 
          status: 400,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    // Get device tokens for the specified users
    console.log(`🔍 Fetching device tokens for ${payload.userIds.length} users`)
    const { data: deviceTokens, error: tokenError } = await supabase
      .from('user_device_tokens')
      .select('user_id, device_token, platform, is_active')
      .in('user_id', payload.userIds)
      .eq('is_active', true)

    if (tokenError) {
      console.error('❌ Error fetching device tokens:', tokenError)
      throw tokenError
    }

    console.log(`📱 Found ${deviceTokens?.length || 0} active device tokens`)

    if (!deviceTokens || deviceTokens.length === 0) {
      console.log('⚠️ No active device tokens found for specified users')
      return new Response(
        JSON.stringify({ 
          success: true, 
          message: 'No active device tokens found',
          sentCount: 0 
        }),
        { 
          status: 200,
          headers: { ...corsHeaders, 'Content-Type': 'application/json' }
        }
      )
    }

    // Prepare FCM messages
    const fcmMessages = deviceTokens.map((token: DeviceToken) => ({
      to: token.device_token,
      notification: {
        title: payload.title,
        body: payload.body,
        icon: 'ic_notification', // Your app's notification icon
        sound: 'default',
        click_action: 'FLUTTER_NOTIFICATION_CLICK', // For deep linking
      },
      data: {
        ...payload.data,
        type: payload.type,
        user_id: token.user_id,
        click_action: 'OPEN_APP'
      },
      android: {
        notification: {
          channel_id: 'madadgar_notifications',
          priority: 'high',
          sound: 'default',
        }
      },
      apns: {
        payload: {
          aps: {
            alert: {
              title: payload.title,
              body: payload.body
            },
            sound: 'default',
            badge: 1
          }
        }
      }
    }))

    console.log(`📤 Preparing to send ${fcmMessages.length} FCM messages`)

    // Send FCM messages
    let successCount = 0
    let failureCount = 0
    const results: Array<{ token: string, success: boolean, error?: string }> = []

    for (const message of fcmMessages) {
      try {
        console.log(`📨 Sending notification to token: ${message.to.substring(0, 20)}...`)
        
        const response = await fetch('https://fcm.googleapis.com/fcm/send', {
          method: 'POST',
          headers: {
            'Authorization': `key=${fcmServerKey}`,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(message),
        })

        const result = await response.json()
        console.log('FCM Response:', result)

        if (response.ok && result.success === 1) {
          successCount++
          results.push({ token: message.to, success: true })
          console.log('✅ Notification sent successfully')
        } else {
          failureCount++
          const errorMsg = result.results?.[0]?.error || 'Unknown FCM error'
          results.push({ token: message.to, success: false, error: errorMsg })
          console.error('❌ Failed to send notification:', errorMsg)
        }
      } catch (error) {
        failureCount++
        results.push({ 
          token: message.to, 
          success: false, 
          error: error.message 
        })
        console.error('❌ Exception sending notification:', error)
      }
    }

    console.log(`✅ Push notification sending complete: ${successCount} sent, ${failureCount} failed`)

    // Store notification results in database for tracking
    try {
      const { error: insertError } = await supabase
        .from('notification_send_log')
        .insert({
          user_ids: payload.userIds,
          title: payload.title,
          body: payload.body,
          type: payload.type,
          success_count: successCount,
          failure_count: failureCount,
          results: results,
          sent_at: new Date().toISOString()
        })

      if (insertError) {
        console.warn('⚠️ Failed to log notification results:', insertError)
      }
    } catch (logError) {
      console.warn('⚠️ Exception logging notification results:', logError)
    }

    return new Response(
      JSON.stringify({
        success: true,
        message: `Push notifications processed: ${successCount} sent, ${failureCount} failed`,
        sentCount: successCount,
        failedCount: failureCount,
        details: results
      }),
      { 
        status: 200,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )

  } catch (error) {
    console.error('❌ Error in push notification function:', error)
    return new Response(
      JSON.stringify({ 
        error: 'Internal server error', 
        message: error.message 
      }),
      { 
        status: 500,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      }
    )
  }
})

/* 
SETUP INSTRUCTIONS:

1. Create this as a Supabase Edge Function:
   npx supabase functions new send-push-notifications
   
2. Copy this code to: supabase/functions/send-push-notifications/index.ts

3. Set environment variables in Supabase:
   - FCM_SERVER_KEY: Your Firebase server key from Firebase Console
   
4. Deploy the function:
   npx supabase functions deploy send-push-notifications

5. Create the notification_send_log table (optional, for tracking):
   
   CREATE TABLE IF NOT EXISTS public.notification_send_log (
     id BIGSERIAL PRIMARY KEY,
     user_ids UUID[],
     title VARCHAR(255),
     body TEXT,
     type VARCHAR(50),
     success_count INTEGER DEFAULT 0,
     failure_count INTEGER DEFAULT 0,
     results JSONB,
     sent_at TIMESTAMPTZ DEFAULT NOW()
   );

USAGE:

POST https://your-project.supabase.co/functions/v1/send-push-notifications

Body:
{
  "userIds": ["user-uuid-1", "user-uuid-2"],
  "title": "New Post Available!",
  "body": "iPhone 13 has been shared in New York",
  "type": "new_listing",
  "data": {
    "item_id": "123",
    "uploader_id": "uploader-uuid",
    "category": "Electronics"
  }
}
*/
