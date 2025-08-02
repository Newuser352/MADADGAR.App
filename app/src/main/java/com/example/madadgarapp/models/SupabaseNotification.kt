package com.example.madadgarapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Data class representing a notification row in Supabase `notifications` table.
 */
@Serializable
data class SupabaseNotification(
    val id: String? = null,

    @SerialName("user_id")
    val userId: String,

    val type: String,

    /**
     * Arbitrary metadata provided when the notification was created. Stored as JSONB in Supabase.
     */
    val payload: JsonObject? = null,

    @SerialName("is_read")
    val isRead: Boolean = false,

    @SerialName("created_at")
    val createdAt: String? = null,
)
