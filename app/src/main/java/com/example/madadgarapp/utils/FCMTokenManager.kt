package com.example.madadgarapp.utils

import android.content.Context
import android.util.Log
import com.example.madadgarapp.repository.NotificationRepository
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FCMTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    
    companion object {
        private const val TAG = "FCMTokenManager"
    }

    /**
     * Initialize FCM token for the current user
     */
    fun initializeFCMToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
                if (currentUser != null) {
                    // Get current FCM token
                    val token = FirebaseMessaging.getInstance().token.await()
                    Log.d(TAG, "FCM token retrieved: $token")
                    
                    // Update token in database
                    val result = notificationRepository.updateDeviceToken(currentUser.id, token)
                    if (result.isSuccess) {
                        Log.d(TAG, "FCM token registered successfully")
                    } else {
                        Log.e(TAG, "Failed to register FCM token: ${result.exceptionOrNull()}")
                    }
                } else {
                    Log.w(TAG, "No authenticated user found, cannot register FCM token")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing FCM token", e)
            }
        }
    }

    /**
     * Refresh FCM token and update in database
     */
    fun refreshFCMToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                FirebaseMessaging.getInstance().deleteToken().await()
                val newToken = FirebaseMessaging.getInstance().token.await()
                
                val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
                if (currentUser != null) {
                    val result = notificationRepository.updateDeviceToken(currentUser.id, newToken)
                    if (result.isSuccess) {
                        Log.d(TAG, "FCM token refreshed successfully")
                    } else {
                        Log.e(TAG, "Failed to refresh FCM token: ${result.exceptionOrNull()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing FCM token", e)
            }
        }
    }

    /**
     * Remove FCM token from database (call when user logs out)
     */
    fun removeFCMToken() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
                if (currentUser != null) {
                    val result = notificationRepository.removeDeviceToken(currentUser.id)
                    if (result.isSuccess) {
                        Log.d(TAG, "FCM token removed successfully")
                    } else {
                        Log.e(TAG, "Failed to remove FCM token: ${result.exceptionOrNull()}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing FCM token", e)
            }
        }
    }

    /**
     * Subscribe to topic for general notifications
     */
    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
                }
            }
    }

    /**
     * Unsubscribe from topic
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                }
            }
    }
}
