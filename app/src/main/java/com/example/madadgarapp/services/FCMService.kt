package com.example.madadgarapp.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.madadgarapp.MainActivity
import com.example.madadgarapp.R
import com.example.madadgarapp.repository.NotificationRepository
import com.example.madadgarapp.utils.SupabaseClient
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMService : FirebaseMessagingService() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "madadgar_notifications"
        private const val CHANNEL_NAME = "MADADGAR Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for new listings and updates"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")
        
        // Update the token in the database
        updateTokenInDatabase(token)
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d(TAG, "=== FCM MESSAGE RECEIVED ===")
        Log.d(TAG, "Message from: ${remoteMessage.from}")
        Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
        Log.d(TAG, "Data payload size: ${remoteMessage.data.size}")
        
        // Log all data fields
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Data payload contents:")
            for ((key, value) in remoteMessage.data) {
                Log.d(TAG, "  $key = $value")
            }
        }
        
        // Handle notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification title: ${notification.title}")
            Log.d(TAG, "Notification body: ${notification.body}")
            
            showNotification(
                title = notification.title ?: "MADADGAR",
                body = notification.body ?: "You have a new notification",
                data = remoteMessage.data
            )
        }
        
        // Handle data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Processing data payload...")
            handleDataPayload(remoteMessage.data)
        } else {
            Log.d(TAG, "No data payload found")
        }
    }

    /**
     * Update FCM token in the database
     */
    private fun updateTokenInDatabase(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
                if (currentUser != null) {
                    // Update token in user_device_tokens table
                    val result = notificationRepository.updateDeviceToken(currentUser.id, token)
                    if (result.isSuccess) {
                        Log.d(TAG, "FCM token updated successfully in database")
                    } else {
                        val error = result.exceptionOrNull()
                        // Only log as error if it's not a duplicate key issue
                        if (error?.message?.contains("duplicate key") == true || 
                            error?.message?.contains("unique constraint") == true) {
                            Log.d(TAG, "FCM token already exists in database (this is normal)")
                        } else {
                            Log.e(TAG, "Failed to update FCM token in database: $error")
                        }
                    }
                } else {
                    Log.w(TAG, "No authenticated user found, cannot update FCM token")
                }
            } catch (e: Exception) {
                // Only log as error if it's not a duplicate key issue
                if (e.message?.contains("duplicate key") == true || 
                    e.message?.contains("unique constraint") == true) {
                    Log.d(TAG, "FCM token already exists in database (this is normal)")
                } else {
                    Log.e(TAG, "Error updating FCM token in database", e)
                }
            }
        }
    }

    /**
     * Handle data payload from FCM message
     */
private fun handleDataPayload(data: Map<String, String>) {
        val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
        val uploaderId = data["uploader_id"]
        
        Log.d(TAG, "Processing notification - Current user: ${currentUser?.id}, Uploader ID: $uploaderId")
        
        // Enhanced uploader filtering with multiple checks
        if (currentUser != null && uploaderId != null) {
            val currentUserId = currentUser.id.trim()
            val uploaderIdTrimmed = uploaderId.trim()
            
            Log.d(TAG, "Comparing user IDs: currentUser='$currentUserId', uploader='$uploaderIdTrimmed'")
            
            // Multiple comparison methods for safety
            val isUploader = currentUserId.equals(uploaderIdTrimmed, ignoreCase = true) ||
                           currentUser.id == uploaderId ||
                           currentUser.id.equals(uploaderId, ignoreCase = false)
            
            if (isUploader) {
                Log.d(TAG, "✅ BLOCKED: Ignoring notification for uploader user $currentUserId")
                return
            } else {
                Log.d(TAG, "✅ ALLOWED: Showing notification to user $currentUserId (uploader: $uploaderIdTrimmed)")
            }
        } else {
            Log.d(TAG, "⚠️ Missing user info: currentUser=${currentUser?.id}, uploaderId=$uploaderId")
            // If we can't determine the uploader, don't show notification to be safe
            if (uploaderId == null) {
                Log.w(TAG, "No uploader_id in notification data, skipping notification")
                return
            }
        }

        val notificationType = data["type"]
        val itemId = data["item_id"]
        val title = data["title"]
        
        when (notificationType) {
            "new_listing" -> {
                showNotification(
                    title = "New Listing Available!",
                    body = "A new item has been posted: $title",
                    data = data
                )
            }
            "post_deleted" -> {
                showNotification(
                    title = "Post Deleted",
                    body = "Your post '$title' has been deleted",
                    data = data
                )
            }
            else -> {
                Log.w(TAG, "Unknown notification type: $notificationType")
            }
        }
    }

    /**
     * Show notification to user
     */
    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add extra data to handle notification click
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            // Add a flag to indicate this was opened from notification
            putExtra("opened_from_notification", true)
            putExtra("notification_type", data["type"] ?: "unknown")
            
            // For new listing notifications, add action to open specific item
            if (data["type"] == "new_listing" && data["item_id"] != null) {
                putExtra("action", "open_item")
                putExtra("target_item_id", data["item_id"])
                Log.d(TAG, "Setting up notification to open item: ${data["item_id"]}")
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // You'll need to add this icon
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Make notifications more prominent
            .setCategory(NotificationCompat.CATEGORY_SOCIAL) // Appropriate for social app
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sound, vibration, lights
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()
        
        Log.d(TAG, "Showing push notification with ID: $notificationId")
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        // Store notification in local database for tracking
        storeNotificationLocally(title, body, data)
    }
    
    /**
     * Store notification in local database for user's notification list
     */
    private fun storeNotificationLocally(title: String, body: String, data: Map<String, String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
                if (currentUser != null) {
                    Log.d(TAG, "Storing push notification locally for user: ${currentUser.id}")
                    
                    val result = notificationRepository.createNotification(
                        userId = currentUser.id,
                        type = data["type"] ?: "push_notification",
                        title = title,
                        body = body,
                        payload = kotlinx.serialization.json.buildJsonObject {
                            data.forEach { (key, value) ->
                                put(key, kotlinx.serialization.json.JsonPrimitive(value))
                            }
                        }
                    )
                    
                    if (result.isSuccess) {
                        Log.d(TAG, "Push notification stored locally successfully")
                    } else {
                        Log.e(TAG, "Failed to store push notification locally: ${result.exceptionOrNull()}")
                    }
                } else {
                    Log.w(TAG, "No authenticated user found, cannot store notification locally")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception storing push notification locally", e)
            }
        }
    }

    /**
     * Create notification channel (required for Android 8.0+)
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // Changed from DEFAULT to HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created with HIGH importance")
        }
    }
}
