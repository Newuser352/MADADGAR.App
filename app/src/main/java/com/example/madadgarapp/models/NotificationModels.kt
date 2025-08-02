package com.example.madadgarapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.EncodeDefault

/**
 * Data class for user_notifications table
 */
@Serializable
data class UserNotification(
    @SerialName("id") val id: Int? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("payload") val payload: JsonElement? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
) {
    override fun equals(other: Any?): Boolean {
        return other is UserNotification && id != null && id == other.id
    }
    override fun hashCode(): Int = id ?: 0
}

/**
 * Data class for creating new notifications
 */
@Serializable
data class NewUserNotification(
    @SerialName("user_id") val userId: String,
    @SerialName("type") val type: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("payload") val payload: JsonElement? = null
)

/**
 * Data class for user_device_tokens table
 */
@Serializable
data class UserDeviceToken(
    @SerialName("id") val id: Int? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("device_token") val deviceToken: String,
    @SerialName("platform") val platform: String = "android",
    @SerialName("is_active") val isActive: Boolean = true,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

/**
 * Data class for creating/updating device tokens
 */
@Serializable
data class DeviceTokenUpdate(
    @SerialName("user_id") val userId: String,
    @SerialName("device_token") val deviceToken: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("platform") val platform: String = "android",
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("is_active") val isActive: Boolean = true
)

/**
 * Data class for deactivating existing device tokens
 */
@Serializable
data class DeviceTokenDeactivate(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("updated_at") val updatedAt: String
)

/**
 * Data class for notification preferences (if you want to add user preferences)
 */
@Serializable
data class NotificationPreferences(
    @SerialName("user_id") val userId: String,
    @SerialName("new_listings_enabled") val newListingsEnabled: Boolean = true,
    @SerialName("deletion_alerts_enabled") val deletionAlertsEnabled: Boolean = true,
    @SerialName("push_notifications_enabled") val pushNotificationsEnabled: Boolean = true
)

/**
 * Data class for notification mark as read update
 */
@Serializable
data class NotificationReadUpdate(
    @SerialName("is_read") val isRead: Boolean = true
)

/**
 * Enum for notification types
 */
enum class NotificationType(val value: String) {
    NEW_LISTING("new_listing"),
    POST_DELETED("post_deleted"),
    SYSTEM_ALERT("system_alert")
}

/**
 * Data class for notification payload data
 */
@Serializable
data class NotificationPayload(
    @SerialName("item_id") val itemId: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("deletion_reason") val deletionReason: String? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)
