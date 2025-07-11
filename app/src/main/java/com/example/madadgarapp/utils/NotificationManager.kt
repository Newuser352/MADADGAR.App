package com.example.madadgarapp.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.madadgarapp.MainActivity
import com.example.madadgarapp.R
import java.util.concurrent.TimeUnit

/**
 * Notification Manager for handling all notification-related operations
 */
object NotificationManager {
    
    private const val TAG = "NotificationManager"
    
    // Notification channels
    private const val CHANNEL_NEW_ITEMS = "new_items"
    private const val CHANNEL_EXPIRY_ALERTS = "expiry_alerts"
    private const val CHANNEL_GENERAL = "general"
    
    // Notification IDs
    private const val NOTIFICATION_ID_NEW_ITEM = 1001
    private const val NOTIFICATION_ID_EXPIRY_ALERT = 1002
    private const val NOTIFICATION_ID_ITEM_AVAILABLE = 1003
    
    /**
     * Initialize notification channels
     */
    fun initializeNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            // New Items Channel
            val newItemsChannel = NotificationChannel(
                CHANNEL_NEW_ITEMS,
                "New Items",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new items in your area"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Expiry Alerts Channel
            val expiryAlertsChannel = NotificationChannel(
                CHANNEL_EXPIRY_ALERTS,
                "Expiry Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for items about to expire"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // General Channel
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
                enableVibration(true)
                setShowBadge(true)
            }
            
            // Register channels
            notificationManager.createNotificationChannel(newItemsChannel)
            notificationManager.createNotificationChannel(expiryAlertsChannel)
            notificationManager.createNotificationChannel(generalChannel)
            
            Log.d(TAG, "Notification channels initialized")
        }
    }
    
    /**
     * Show notification for new item in user's area
     */
    fun showNewItemNotification(
        context: Context,
        itemTitle: String,
        itemCategory: String,
        distance: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_NEW_ITEMS)
            .setSmallIcon(R.drawable.ic_add)
            .setContentTitle("New Item Nearby")
            .setContentText("$itemTitle ($itemCategory) - $distance away")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_NEW_ITEM, notification)
        Log.d(TAG, "New item notification shown: $itemTitle")
    }
    
    /**
     * Show expiry alert notification
     */
    fun showExpiryAlertNotification(
        context: Context,
        itemTitle: String,
        expiryTime: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "my_posts")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_EXPIRY_ALERTS)
            .setSmallIcon(R.drawable.ic_calendar)
            .setContentTitle("Item Expiring Soon")
            .setContentText("$itemTitle will expire $expiryTime")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_EXPIRY_ALERT, notification)
        Log.d(TAG, "Expiry alert notification shown: $itemTitle")
    }
    
    /**
     * Show item availability alert
     */
    fun showItemAvailabilityAlert(
        context: Context,
        itemTitle: String,
        itemCategory: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_info)
            .setContentTitle("Item Available")
            .setContentText("$itemTitle ($itemCategory) is now available")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_ITEM_AVAILABLE, notification)
        Log.d(TAG, "Item availability notification shown: $itemTitle")
    }
    
    /**
     * Schedule expiry reminder for an item
     */
    fun scheduleExpiryReminder(
        context: Context,
        itemId: String,
        itemTitle: String,
        expiryTimeMillis: Long
    ) {
        val currentTime = System.currentTimeMillis()
        val timeUntilExpiry = expiryTimeMillis - currentTime
        
        // Schedule reminder 1 hour before expiry
        val reminderTime = timeUntilExpiry - TimeUnit.HOURS.toMillis(1)
        
        if (reminderTime > 0) {
            val workRequest = OneTimeWorkRequestBuilder<ExpiryReminderWorker>()
                .setInitialDelay(reminderTime, TimeUnit.MILLISECONDS)
                .setInputData(
                    Data.Builder()
                        .putString("item_id", itemId)
                        .putString("item_title", itemTitle)
                        .putLong("expiry_time", expiryTimeMillis)
                        .build()
                )
                .addTag("expiry_reminder_$itemId")
                .build()
            
            WorkManager.getInstance(context).enqueue(workRequest)
            Log.d(TAG, "Expiry reminder scheduled for $itemTitle")
        }
    }
    
    /**
     * Cancel expiry reminder for an item
     */
    fun cancelExpiryReminder(context: Context, itemId: String) {
        WorkManager.getInstance(context).cancelAllWorkByTag("expiry_reminder_$itemId")
        Log.d(TAG, "Expiry reminder cancelled for item: $itemId")
    }
    
    /**
     * Check if notifications are enabled
     */
    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    
    /**
     * Worker class for handling expiry reminders
     */
    class ExpiryReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
        
        override fun doWork(): Result {
            return try {
                val itemId = inputData.getString("item_id") ?: return Result.failure()
                val itemTitle = inputData.getString("item_title") ?: return Result.failure()
                val expiryTime = inputData.getLong("expiry_time", 0)
                
                val currentTime = System.currentTimeMillis()
                val timeUntilExpiry = expiryTime - currentTime
                
                if (timeUntilExpiry > 0) {
                    val expiryTimeString = when {
                        timeUntilExpiry < TimeUnit.HOURS.toMillis(1) -> "in less than 1 hour"
                        timeUntilExpiry < TimeUnit.HOURS.toMillis(24) -> "in ${timeUntilExpiry / TimeUnit.HOURS.toMillis(1)} hours"
                        else -> "soon"
                    }
                    
                    showExpiryAlertNotification(applicationContext, itemTitle, expiryTimeString)
                }
                
                Result.success()
            } catch (e: Exception) {
                Log.e(TAG, "Error in ExpiryReminderWorker", e)
                Result.failure()
            }
        }
    }
}
