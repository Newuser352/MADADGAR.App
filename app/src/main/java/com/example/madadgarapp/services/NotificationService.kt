package com.example.madadgarapp.services

import android.util.Log
import com.example.madadgarapp.models.NotificationType
import com.example.madadgarapp.models.SupabaseItem
import com.example.madadgarapp.repository.NotificationRepository
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for managing notification operations
 */
@Singleton
class NotificationService @Inject constructor(
    private val notificationRepository: NotificationRepository
) {
    
    companion object {
        private const val TAG = "NotificationService"
    }
    
    /**
     * Create notifications for all users when a new post is uploaded
     * Excludes the uploader before sending.
     */
    suspend fun createNewPostNotifications(
        item: SupabaseItem,
        uploaderUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating new post notifications for item: ${item.title}")
            Log.d(TAG, "Uploader user ID: $uploaderUserId")

            // Get all user IDs and filter out the uploader client-side for safety
            val allUsersResult = notificationRepository.getAllUserIds()

            if (allUsersResult.isFailure) {
                Log.e(TAG, "Failed to get user IDs", allUsersResult.exceptionOrNull())
                return@withContext Result.failure(allUsersResult.exceptionOrNull() ?: Exception("Failed to get user IDs"))
            }

            val allUserIds = allUsersResult.getOrNull() ?: emptyList()
            
            // Enhanced client-side filtering with multiple comparison methods
            val otherUserIds = allUserIds.filter { userId ->
                val trimmedUserId = userId.trim()
                val trimmedUploaderId = uploaderUserId.trim()
                
                // Multiple comparison methods for safety
                val isUploader = trimmedUserId.equals(trimmedUploaderId, ignoreCase = true) ||
                               userId == uploaderUserId ||
                               userId.equals(uploaderUserId, ignoreCase = false)
                
                if (isUploader) {
                    Log.d(TAG, "Excluding uploader from notifications: $userId")
                }
                
                !isUploader
            }
            
            Log.d(TAG, "Found ${allUserIds.size} total users, ${otherUserIds.size} other users to notify (excluding uploader $uploaderUserId)")
            Log.d(TAG, "Filtered out uploader: ${allUserIds.size - otherUserIds.size} user(s)")
            
            // Additional validation
            if (otherUserIds.contains(uploaderUserId)) {
                Log.e(TAG, "ERROR: Uploader $uploaderUserId still in notification list! This should not happen.")
                // Remove uploader as final safety check
                val finalUserIds = otherUserIds.filter { it != uploaderUserId }
                Log.d(TAG, "Final safety filter applied, ${finalUserIds.size} users will receive notifications")
            }

            if (otherUserIds.isEmpty()) {
                Log.w(TAG, "No other users found to notify")
                return@withContext Result.success(Unit)
            }

            // Create notification payload
            val payload = createNotificationPayload(item, uploaderUserId)

            // Notification content
            val title = "New ${item.mainCategory} Available"
            val body = "${item.title} has been shared in ${item.location}"

            val result = notificationRepository.createNotificationsForUsers(
                userIds = otherUserIds,
                type = NotificationType.NEW_LISTING.value,
                title = title,
                body = body,
                payload = payload
            )

            if (result.isSuccess) {
                Log.d(TAG, "Successfully created notifications for ${otherUserIds.size} users")
            } else {
                Log.e(TAG, "Failed to create notifications", result.exceptionOrNull())
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error creating new post notifications", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create notification when a post is deleted (excludes uploader)
     */
    suspend fun createPostDeletedNotifications(
        item: SupabaseItem,
        uploaderUserId: String,
        deletionReason: String = "Post removed by owner"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating post deleted notifications for item: ${item.title}")

            // Get all user IDs and filter out the uploader client-side for safety
            val allUsersResult = notificationRepository.getAllUserIds()

            if (allUsersResult.isFailure) {
                Log.e(TAG, "Failed to get user IDs", allUsersResult.exceptionOrNull())
                return@withContext Result.failure(allUsersResult.exceptionOrNull() ?: Exception("Failed to get user IDs"))
            }

            val allUserIds = allUsersResult.getOrNull() ?: emptyList()
            // Client-side filtering as additional safety
            val otherUserIds = allUserIds.filter { userId ->
                !userId.trim().equals(uploaderUserId.trim(), ignoreCase = true)
            }
            
            Log.d(TAG, "Found ${allUserIds.size} total users, ${otherUserIds.size} other users to notify (excluding uploader $uploaderUserId)")
            Log.d(TAG, "Filtered out uploader: ${allUserIds.size - otherUserIds.size} user(s)")

            if (otherUserIds.isEmpty()) {
                Log.d(TAG, "No other users found to notify")
                return@withContext Result.success(Unit)
            }

            // Create notification payload
            val payload = createNotificationPayload(item, uploaderUserId, deletionReason)

            val title = "Post No Longer Available"
            val body = "${item.title} has been removed from ${item.location}"

            val result = notificationRepository.createNotificationsForUsers(
                userIds = otherUserIds,
                type = NotificationType.POST_DELETED.value,
                title = title,
                body = body,
                payload = payload
            )

            if (result.isSuccess) {
                Log.d(TAG, "Successfully created deletion notifications for ${otherUserIds.size} users")
            } else {
                Log.e(TAG, "Failed to create deletion notifications", result.exceptionOrNull())
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error creating post deleted notifications", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create system alert notification
     */
    suspend fun createSystemAlert(
        userIds: List<String>,
        title: String,
        body: String,
        payload: JsonElement? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Creating system alert for ${userIds.size} users: $title")
            
            val result = notificationRepository.createNotificationsForUsers(
                userIds = userIds,
                type = NotificationType.SYSTEM_ALERT.value,
                title = title,
                body = body,
                payload = payload
            )
            
            if (result.isSuccess) {
                Log.d(TAG, "Successfully created system alert for ${userIds.size} users")
            } else {
                Log.e(TAG, "Failed to create system alert", result.exceptionOrNull())
            }
            
            result
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating system alert", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create notification payload from item data
     */
    private fun createNotificationPayload(
        item: SupabaseItem,
        uploaderUserId: String? = null,
        deletionReason: String? = null
    ): JsonElement {
        return buildJsonObject {
            item.id?.let { put("item_id", it) }
            put("category", item.mainCategory)
            put("subcategory", item.subCategory ?: "")
            put("location", item.location)
            put("title", item.title)
            // Use provided uploaderUserId or fall back to item.ownerId
            val uploaderId = uploaderUserId ?: item.ownerId
            uploaderId?.let { put("uploader_id", it) }
            deletionReason?.let { put("deletion_reason", it) }
            if (deletionReason != null) {
                put("deleted_at", System.currentTimeMillis().toString())
            }
        }
    }
    
}
