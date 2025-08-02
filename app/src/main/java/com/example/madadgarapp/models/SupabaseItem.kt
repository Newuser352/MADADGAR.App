package com.example.madadgarapp.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data class representing an item in Supabase database
 * This class is used for serialization/deserialization with Supabase Postgrest
 */
@Serializable
data class SupabaseItem(
    val id: String? = null,
    val title: String,
    val description: String,
    @SerialName("main_category")
    val mainCategory: String,
    @SerialName("sub_category")
    val subCategory: String,
    val location: String,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("contact_number")
    val contactNumber: String,
    val contact1: String? = null,
    val contact2: String? = null,
    @SerialName("owner_id")
    val ownerId: String,
    @kotlinx.serialization.Transient
    @SerialName("owner_email")
    val ownerEmail: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("video_url")
    val videoUrl: String? = null
)

/**
 * Data class for inserting new items (without auto-generated fields)
 */
@Serializable
data class NewSupabaseItem(
    val title: String,
    val description: String,
    @SerialName("main_category")
    val mainCategory: String,
    @SerialName("sub_category")
    val subCategory: String,
    val location: String,
    @SerialName("latitude")
    val latitude: Double? = null,
    @SerialName("longitude")
    val longitude: Double? = null,
    @SerialName("contact_number")
    val contactNumber: String,
    val contact1: String? = null,
    val contact2: String? = null,
    @SerialName("owner_id")
    val ownerId: String,
    @kotlinx.serialization.Transient
    @SerialName("owner_email")
    val ownerEmail: String? = null,
    @SerialName("expires_at")
    val expiresAt: String? = null,
    @SerialName("image_urls")
    val imageUrls: List<String> = emptyList(),
    @SerialName("video_url")
    val videoUrl: String? = null
)

/**
 * Extension function to convert Java Item model to Supabase item
 */
fun Item.toSupabaseItem(ownerId: String): NewSupabaseItem {
    return NewSupabaseItem(
        title = this.title ?: "",
        description = this.description ?: "",
        mainCategory = this.mainCategory ?: "",
        subCategory = this.subCategory ?: "",
        location = this.location ?: "",
        contactNumber = this.contactNumber ?: "",
        ownerId = ownerId,
        expiresAt = if (this.expiryTime > 0) {
            // Convert timestamp to ISO string
            java.time.Instant.ofEpochMilli(this.expiryTime)
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toString()
        } else null
    )
}
