package com.example.madadgarapp.repository

import android.util.Log
import com.example.madadgarapp.models.UserNotification
import com.example.madadgarapp.models.NewUserNotification
import com.example.madadgarapp.models.UserDeviceToken
import com.example.madadgarapp.models.DeviceTokenUpdate
import com.example.madadgarapp.models.DeviceTokenDeactivate
import com.example.madadgarapp.models.NotificationReadUpdate
import com.example.madadgarapp.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class OwnerIdOnly(@SerialName("owner_id") val ownerId: String)

/**
 * Minimal projection for querying user ids from the profiles table
 */
@Serializable
data class UserIdOnly(@SerialName("id") val id: String)

/**
 * Repository for interacting with notification-related database tables.
 */
@Singleton
class NotificationRepository @Inject constructor() {

    companion object {
        private const val TAG = "NotificationRepository"
        private const val NOTIFICATIONS_TABLE = "user_notifications"
        private const val DEVICE_TOKENS_TABLE = "user_device_tokens"
    }

    /**
     * Fetch unread notifications for current user (sorted newest first)
     */
    suspend fun getUnread(userId: String): Result<List<UserNotification>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching unread notifications for $userId")
                val allNotifications = SupabaseClient.client
                    .from(NOTIFICATIONS_TABLE)
                    .select()
                    .decodeList<UserNotification>()
                
                Log.d(TAG, "Retrieved ${allNotifications.size} total notifications from database")
                allNotifications.forEach { notification ->
                    Log.d(TAG, "Raw notification: ID=${notification.id}, UserId=${notification.userId}, Title=${notification.title}, IsRead=${notification.isRead}, CreatedAt=${notification.createdAt}")
                }
                
                val filteredByUser = allNotifications.filter { it.userId == userId }
                Log.d(TAG, "Filtered to ${filteredByUser.size} notifications for user $userId")
                
                val unreadForUser = filteredByUser.filter { !it.isRead }
                Log.d(TAG, "Found ${unreadForUser.size} unread notifications for user $userId")
                
                val list = unreadForUser.sortedByDescending { it.createdAt }
                Log.d(TAG, "Returning ${list.size} sorted unread notifications")
                
                Result.success(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching notifications", e)
                Result.failure(e)
            }
        }

    /**
     * Fetch all notifications for current user (sorted newest first)
     */
    suspend fun getAllNotifications(userId: String): Result<List<UserNotification>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching all notifications for $userId")
                val allNotifications = SupabaseClient.client
                    .from(NOTIFICATIONS_TABLE)
                    .select()
                    .decodeList<UserNotification>()
                
                Log.d(TAG, "getAllNotifications: Retrieved ${allNotifications.size} total notifications from database")
                
                val list = allNotifications
                    .filter { it.userId == userId }
                    .sortedByDescending { it.createdAt }
                
                Log.d(TAG, "getAllNotifications: Returning ${list.size} notifications for user $userId")
                Result.success(list)
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching all notifications", e)
                Result.failure(e)
            }
        }

    /**
     * Delete a single notification
     */
suspend fun deleteNotification(notificationId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting notification: $notificationId")
                SupabaseClient.client
                    .from(NOTIFICATIONS_TABLE)
                    .delete {
                        filter {
                            eq("id", notificationId)
                        }
                    }
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting notification", e)
                Result.failure(e)
            }
        }

    /**
     * Mark a single notification as read
     */
    suspend fun markRead(notificationId: Int): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Marking notification as read: $notificationId")
                
                // Get all notifications and find the one to update
                val allNotifications = SupabaseClient.client
                    .from(NOTIFICATIONS_TABLE)
                    .select()
                    .decodeList<UserNotification>()
                
                // Find the notification to update
                val notification = allNotifications.find { it.id == notificationId }
                if (notification != null) {
                    // For now, just log the action
                    // TODO: Implement actual database update when server-side filtering is working
                    Log.d(TAG, "Found notification to mark as read: ${notification.title}")
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification read", e)
                Result.failure(e)
            }
        }

    /**
     * Update or insert device token for user
     */
    suspend fun updateDeviceToken(userId: String, token: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Updating device token for user: $userId")
                Log.d(TAG, "Token starts with: ${token.take(20)}...")
                
                // Check if the same token already exists for this user
                try {
                    val existingTokens = SupabaseClient.client
                        .from(DEVICE_TOKENS_TABLE)
                        .select()
                        .decodeList<UserDeviceToken>()
                        .filter { it.userId == userId && it.deviceToken == token }
                    
                    if (existingTokens.isNotEmpty()) {
                        Log.d(TAG, "Token already exists for user, just updating to active")
                        
                        // Token already exists, just make sure it's active
                        val updateData = DeviceTokenDeactivate(
                            isActive = true,
                            updatedAt = java.time.Instant.now().toString()
                        )
                        
                        try {
                            SupabaseClient.client
                                .from(DEVICE_TOKENS_TABLE)
                                .update(updateData) {
                                    filter {
                                        eq("user_id", userId)
                                        eq("device_token", token)
                                    }
                                }
                            Log.d(TAG, "Existing token updated to active")
                        } catch (updateError: Exception) {
                            Log.w(TAG, "Could not update existing token: ${updateError.message}")
                        }
                        
                        return@withContext Result.success(Unit)
                    }
                } catch (selectError: Exception) {
                    Log.w(TAG, "Could not check existing tokens (might be RLS): ${selectError.message}")
                    // Continue with insert/upsert approach
                }
                
                // Token doesn't exist or we couldn't check - try to insert
                val deviceTokenData = DeviceTokenUpdate(
                    userId = userId,
                    deviceToken = token,
                    platform = "android",
                    isActive = true
                )
                
                try {
                    // First try to deactivate other tokens for this user
                    Log.d(TAG, "Attempting to deactivate other tokens for user...")
                    val deactivateData = DeviceTokenDeactivate(
                        isActive = false,
                        updatedAt = java.time.Instant.now().toString()
                    )
                    
                    try {
                        SupabaseClient.client
                            .from(DEVICE_TOKENS_TABLE)
                            .update(deactivateData) {
                                filter {
                                    eq("user_id", userId)
                                    eq("is_active", true)
                                    neq("device_token", token) // Don't deactivate the current token
                                }
                            }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not deactivate other tokens (might be RLS): ${e.message}")
                    }
                    
                    // Now try to insert the new token
                    Log.d(TAG, "Inserting new device token...")
                    SupabaseClient.client
                        .from(DEVICE_TOKENS_TABLE)
                        .insert(deviceTokenData)
                    
                    Log.d(TAG, "Device token inserted successfully")
                    Result.success(Unit)
                    
                } catch (insertError: Exception) {
                    Log.w(TAG, "Direct insert failed: ${insertError.message}")
                    
                    // Check if it's a duplicate key error
                    if (insertError.message?.contains("duplicate key") == true || 
                        insertError.message?.contains("unique constraint") == true) {
                        Log.d(TAG, "Token already exists, this is expected. Considering as success.")
                        Result.success(Unit)
                    } else {
                        // Try upsert for other errors
                        try {
                            Log.d(TAG, "Trying upsert...")
                            SupabaseClient.client
                                .from(DEVICE_TOKENS_TABLE)
                                .upsert(deviceTokenData)
                            
                            Log.d(TAG, "Device token upserted successfully")
                            Result.success(Unit)
                        } catch (upsertError: Exception) {
                            Log.e(TAG, "Upsert also failed: ${upsertError.message}")
                            
                            // If it's still a duplicate key error, consider it success
                            if (upsertError.message?.contains("duplicate key") == true || 
                                upsertError.message?.contains("unique constraint") == true) {
                                Log.d(TAG, "Upsert failed with duplicate key, but token exists. Considering as success.")
                                Result.success(Unit)
                            } else {
                                Result.failure(upsertError)
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error updating device token", e)
                
                // Even if there's an error, if it's about duplicate keys, it means the token is already there
                if (e.message?.contains("duplicate key") == true || 
                    e.message?.contains("unique constraint") == true) {
                    Log.d(TAG, "Duplicate key error, but token exists. Considering as success.")
                    Result.success(Unit)
                } else {
                    Result.failure(e)
                }
            }
        }

    /**
     * Remove device token for user (called on logout)
     */
    suspend fun removeDeviceToken(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Removing device token for user: $userId")
                
                // Get existing tokens
                val existingTokens = SupabaseClient.client
                    .from(DEVICE_TOKENS_TABLE)
                    .select()
                    .decodeList<UserDeviceToken>()
                    .filter { it.userId == userId }
                
                // For now, just log the action
                // TODO: Implement actual database deletion when server-side filtering is working
                Log.d(TAG, "Found ${existingTokens.size} tokens to remove")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error removing device token", e)
                Result.failure(e)
            }
        }

    /**
     * Get unread notification count for user
     */
    suspend fun getUnreadCount(userId: String): Result<Int> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting unread count for user: $userId")
                val unreadResult = getUnread(userId)
                if (unreadResult.isSuccess) {
                    val count = unreadResult.getOrNull()?.size ?: 0
                    Result.success(count)
                } else {
                    Result.failure(unreadResult.exceptionOrNull() ?: Exception("Unknown error"))
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting unread count", e)
                Result.failure(e)
            }
        }

    /**
     * Create a new notification for a user
     */
    suspend fun createNotification(
        userId: String,
        type: String,
        title: String,
        body: String,
        payload: kotlinx.serialization.json.JsonElement? = null
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating notification for user: $userId")
                
                val newNotification = NewUserNotification(
                    userId = userId,
                    type = type,
                    title = title,
                    body = body,
                    payload = payload
                )
                
                SupabaseClient.client
                    .from(NOTIFICATIONS_TABLE)
                    .insert(newNotification)
                
                Log.d(TAG, "Successfully created notification for user: $userId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification", e)
                Result.failure(e)
            }
        }
    
    /**
     * Create notifications for multiple users (bulk operation)
     */
    suspend fun createNotificationsForUsers(
        userIds: List<String>,
        type: String,
        title: String,
        body: String,
        payload: kotlinx.serialization.json.JsonElement? = null
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating notifications for ${userIds.size} users")
                
                val notifications = userIds.map { userId ->
                    NewUserNotification(
                        userId = userId,
                        type = type,
                        title = title,
                        body = body,
                        payload = payload
                    )
                }
                
                SupabaseClient.client
                    .from(NOTIFICATIONS_TABLE)
                    .insert(notifications)
                
                Log.d(TAG, "Successfully created notifications for ${userIds.size} users")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error creating bulk notifications", e)
                Result.failure(e)
            }
        }
    
    /**
     * Get all user IDs from the database
     * Database trigger will handle filtering out self-notifications
     */
    suspend fun getAllUserIds(): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting all user IDs from profiles")

                // Prefer users that actually have a registered device token so we only
                // attempt to push to reachable devices.
                val usersWithTokens = try {
                    SupabaseClient.client
                        .from(DEVICE_TOKENS_TABLE)
                        .select()
                        .decodeList<UserDeviceToken>()
                        .map { it.userId }
                        .distinct()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch user IDs from device tokens table", e)
                    emptyList()
                }

                // Fall back to profiles table to cover users who may not have opened the app yet.
                val allProfiles = try {
                    SupabaseClient.client
                        .from("profiles")
                        .select()
                        .decodeList<UserIdOnly>()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch user IDs from profiles table", e)
                    emptyList()
                }

                // Combine all user IDs
                val allUserIds = (usersWithTokens + allProfiles.map { it.id }).distinct()
                Log.d(TAG, "Found ${allUserIds.size} total users")

                Result.success(allUserIds)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user IDs from profiles", e)
                Result.failure(e)
            }
        }

    /**
     * Get every user ID in the `profiles` table except the uploader.
     * This ensures we notify *all* registered users – even those who have
     * never posted an item or received a notification before.
     * 
     * @deprecated Use getAllUserIds() instead - database trigger handles filtering
     */
    @Deprecated("Use getAllUserIds() instead - database trigger handles filtering")
    suspend fun getAllUserIdsExcept(excludeUserId: String): Result<List<String>> =
        withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting all user IDs from profiles except: $excludeUserId")

                // Prefer users that actually have a registered device token so we only
                // attempt to push to reachable devices.
                val usersWithTokens = try {
                    SupabaseClient.client
                        .from(DEVICE_TOKENS_TABLE)
                        .select()
                        .decodeList<UserDeviceToken>()
                        .map { it.userId }
                        .distinct()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch user IDs from device tokens table", e)
                    emptyList()
                }

                // Fall back to profiles table to cover users who may not have opened the app yet.
                val allProfiles = try {
                    SupabaseClient.client
                        .from("profiles")
                        .select()
                        .decodeList<UserIdOnly>()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not fetch user IDs from profiles table", e)
                    emptyList()
                }

                // Combine and filter
                val combinedUserIds = (usersWithTokens + allProfiles.map { it.id }).distinct()
                Log.d(TAG, "Combined user IDs before filtering: ${combinedUserIds.joinToString(", ") { "'$it'" }}")
                Log.d(TAG, "Excluding user ID: '$excludeUserId'")
                
                // Exclude the uploader regardless of potential whitespace or case differences
                val allUserIds = combinedUserIds.filter { candidate ->
                    // Defensive trimming & case-insensitive comparison
                    !candidate.trim().equals(excludeUserId.trim(), ignoreCase = true)
                }
                Log.d(TAG, "User IDs that matched exclude criteria: ${combinedUserIds.filter { it.trim().equals(excludeUserId.trim(), ignoreCase = true) }.joinToString(", ") { "'$it'" }}")
                Log.d(TAG, "Found ${allUserIds.size} other users (excluding $excludeUserId)")
                Log.d(TAG, "Final filtered user IDs: ${allUserIds.joinToString(", ") { "'$it'" }}")

                Result.success(allUserIds)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting user IDs from profiles", e)
                Result.failure(e)
            }
        }

}
