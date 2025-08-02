package com.example.madadgarapp.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.madadgarapp.models.NewSupabaseItem
import com.example.madadgarapp.models.SupabaseItem
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import com.example.madadgarapp.services.NotificationService
import javax.inject.Inject

/**
 * Kotlin bridge for real Supabase operations
 * This class provides Java-friendly methods with real Supabase integration
 */
class SupabaseItemBridge : CoroutineScope {
    
    companion object {
        private const val TAG = "SupabaseItemBridge"
    }
    
    // Use SupervisorJob to handle failures gracefully
    private val job = SupervisorJob()
    
    // Use Main dispatcher with IO for background work
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job
    
    private val repository = ItemRepository()
    private val notificationService = NotificationService(com.example.madadgarapp.repository.NotificationRepository())
    
    /**
     * Interface for callbacks from async operations (Java-friendly)
     */
    interface RepositoryCallback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }
    
    /**
     * Create a new item in Supabase database (real operation)
     * 
     * @param item The item to create
     * @param callback Callback for success/error handling
     */
    fun createItem(item: NewSupabaseItem, callback: RepositoryCallback<SupabaseItem>) {
        Log.d(TAG, "Creating item in Supabase: ${item.title}")
        
        launch {
            try {
                // Switch to IO dispatcher for network operation
                val result = withContext(Dispatchers.IO) {
                    repository.createItem(item)
                }
                
                if (result.isSuccess) {
                    val createdItem = result.getOrNull()
                    if (createdItem != null) {
                        Log.d(TAG, "Item created successfully in Supabase: ${createdItem.id}")
                        callback.onSuccess(createdItem)
                    } else {
                        Log.e(TAG, "Created item is null")
                        callback.onError("Item creation returned null result")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = error?.message ?: "Unknown error creating item"
                    Log.e(TAG, "Failed to create item in Supabase: $errorMessage", error)
                    callback.onError(errorMessage)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception creating item in Supabase", e)
                callback.onError(e.message ?: "Exception during item creation")
            }
        }
    }
    
    /**
     * Upload images to Supabase storage (real operation)
     * 
     * @param context Android context
     * @param imageUris List of image URIs to upload
     * @param userId User ID for organizing files
     * @param callback Callback for success/error handling
     */
    fun uploadImages(
        context: Context, 
        imageUris: List<Uri>, 
        userId: String, 
        callback: RepositoryCallback<List<String>>
    ) {
        Log.d(TAG, "Uploading ${imageUris.size} images to Supabase for user: $userId")
        
        launch {
            try {
                // Switch to IO dispatcher for network operation
                val result = withContext(Dispatchers.IO) {
                    repository.uploadImages(context, imageUris, userId)
                }
                
                if (result.isSuccess) {
                    val imageUrls = result.getOrNull()
                    if (imageUrls != null) {
                        Log.d(TAG, "Images uploaded successfully to Supabase: ${imageUrls.size} URLs")
                        callback.onSuccess(imageUrls)
                    } else {
                        Log.e(TAG, "Image upload returned null result")
                        callback.onError("Image upload returned null result")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = error?.message ?: "Unknown error uploading images"
                    Log.e(TAG, "Failed to upload images to Supabase: $errorMessage", error)
                    callback.onError(errorMessage)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception uploading images to Supabase", e)
                callback.onError(e.message ?: "Exception during image upload")
            }
        }
    }
    
    /**
     * Upload video to Supabase storage (real operation)
     * 
     * @param context Android context
     * @param videoUri Video URI to upload
     * @param userId User ID for organizing files
     * @param callback Callback for success/error handling
     */
    fun uploadVideo(
        context: Context, 
        videoUri: Uri, 
        userId: String, 
        callback: RepositoryCallback<String>
    ) {
        Log.d(TAG, "Uploading video to Supabase for user: $userId")
        
        launch {
            try {
                // Switch to IO dispatcher for network operation
                val result = withContext(Dispatchers.IO) {
                    repository.uploadVideo(context, videoUri, userId)
                }
                
                if (result.isSuccess) {
                    val videoUrl = result.getOrNull()
                    if (videoUrl != null) {
                        Log.d(TAG, "Video uploaded successfully to Supabase: $videoUrl")
                        callback.onSuccess(videoUrl)
                    } else {
                        Log.e(TAG, "Video upload returned null result")
                        callback.onError("Video upload returned null result")
                    }
                } else {
                    val error = result.exceptionOrNull()
                    val errorMessage = error?.message ?: "Unknown error uploading video"
                    Log.e(TAG, "Failed to upload video to Supabase: $errorMessage", error)
                    callback.onError(errorMessage)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception uploading video to Supabase", e)
                callback.onError(e.message ?: "Exception during video upload")
            }
        }
    }
    
    /**
     * Complete item creation workflow with real Supabase operations
     * 
     * @param context Android context for file operations
     * @param title Item title
     * @param description Item description
     * @param mainCategory Main category (Food/Non-Food)
     * @param subCategory Sub category
     * @param location Item location
     * @param latitude Item latitude (optional)
     * @param longitude Item longitude (optional)
     * @param contactNumber Main contact number
     * @param contact1 Additional contact 1 (optional)
     * @param contact2 Additional contact 2 (optional)
     * @param userId Owner user ID
     * @param ownerEmail Owner email (optional)
     * @param expiresAt Expiry date (optional, for food items)
     * @param imageUris List of image URIs to upload (optional)
     * @param videoUri Video URI to upload (optional)
     * @param callback Callback for final result
     */
    fun createCompleteItem(
        context: Context,
        title: String,
        description: String,
        mainCategory: String,
        subCategory: String,
        location: String,
        latitude: Double?,
        longitude: Double?,
        contactNumber: String,
        contact1: String?,
        contact2: String?,
        userId: String,
        ownerEmail: String?,
        expiresAt: String?,
        imageUris: List<Uri>?,
        videoUri: Uri?,
        callback: RepositoryCallback<SupabaseItem>
    ) {
        Log.d(TAG, "Starting complete item creation workflow with Supabase")
        
        launch {
            try {
                var imageUrls: List<String> = emptyList()
                var videoUrl: String? = null
                
                // Step 1: Upload images if any
                if (!imageUris.isNullOrEmpty()) {
                    Log.d(TAG, "Uploading images to Supabase...")
                    val imageResult = withContext(Dispatchers.IO) {
                        repository.uploadImages(context, imageUris, userId)
                    }
                    
                    if (imageResult.isSuccess) {
                        imageUrls = imageResult.getOrNull() ?: emptyList()
                        Log.d(TAG, "Images uploaded successfully: ${imageUrls.size} URLs")
                    } else {
                        val error = imageResult.exceptionOrNull()?.message ?: "Failed to upload images"
                        Log.e(TAG, "Image upload failed: $error")
                        callback.onError("Failed to upload images: $error")
                        return@launch
                    }
                }
                
                // Step 2: Upload video if any
                if (videoUri != null) {
                    Log.d(TAG, "Uploading video to Supabase...")
                    val videoResult = withContext(Dispatchers.IO) {
                        repository.uploadVideo(context, videoUri, userId)
                    }
                    
                    if (videoResult.isSuccess) {
                        videoUrl = videoResult.getOrNull()
                        Log.d(TAG, "Video uploaded successfully: $videoUrl")
                    } else {
                        val error = videoResult.exceptionOrNull()?.message ?: "Failed to upload video"
                        Log.e(TAG, "Video upload failed: $error")
                        callback.onError("Failed to upload video: $error")
                        return@launch
                    }
                }
                
                // Step 3: Create item in database
                Log.d(TAG, "Creating item in Supabase database...")
                val newItem = NewSupabaseItem(
                    title = title,
                    description = description,
                    mainCategory = mainCategory,
                    subCategory = subCategory,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    contactNumber = contactNumber,
                    contact1 = contact1,
                    contact2 = contact2,
                    ownerId = userId,
                    ownerEmail = ownerEmail,
                    expiresAt = expiresAt,
                    imageUrls = imageUrls,
                    videoUrl = videoUrl
                )
                
                val itemResult = withContext(Dispatchers.IO) {
                    repository.createItem(newItem)
                }
                
                if (itemResult.isSuccess) {
                    val createdItem = itemResult.getOrNull()
                    if (createdItem != null) {
                        Log.d(TAG, "Complete item creation successful! Item ID: ${createdItem.id}")
                        
                        // Create notifications for other users about the new post
                        Log.d(TAG, "Creating notifications for new post...")
                        try {
                            val notificationResult = notificationService.createNewPostNotifications(
                                item = createdItem,
                                uploaderUserId = userId
                            )
                            
                            if (notificationResult.isSuccess) {
                                Log.d(TAG, "Successfully created notifications for new post")
                            } else {
                                Log.w(TAG, "Failed to create notifications for new post, but item creation was successful", notificationResult.exceptionOrNull())
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Exception creating notifications for new post, but item creation was successful", e)
                        }
                        
                        callback.onSuccess(createdItem)
                    } else {
                        Log.e(TAG, "Item creation returned null")
                        callback.onError("Item creation returned null result")
                    }
                } else {
                    val error = itemResult.exceptionOrNull()?.message ?: "Failed to create item"
                    Log.e(TAG, "Item creation failed: $error")
                    callback.onError("Failed to create item: $error")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception in complete item creation workflow", e)
                callback.onError(e.message ?: "Exception during item creation")
            }
        }
    }
    
    /**
     * Get all active items from Supabase (for main items list)
     */
    fun getActiveItems(limit: Int, offset: Int, callback: RepositoryCallback<List<SupabaseItem>>) {
        Log.d(TAG, "Fetching active items from Supabase (limit: $limit, offset: $offset)")
        
        launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getActiveItems(limit, offset)
                }
                
                if (result.isSuccess) {
                    val items = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Fetched ${items.size} active items from Supabase")
                    callback.onSuccess(items)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to fetch active items"
                    Log.e(TAG, "Failed to fetch active items: $error")
                    callback.onError(error)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching active items", e)
                callback.onError(e.message ?: "Exception fetching items")
            }
        }
    }
    
    /**
     * Get active items within a bounding box (for location-based filtering)
     */
    fun getActiveItemsInBoundingBox(
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int,
        offset: Int,
        callback: RepositoryCallback<List<SupabaseItem>>
    ) {
        Log.d(TAG, "Fetching active items in bounding box from Supabase")
        
        launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getActiveItemsInBoundingBox(minLat, maxLat, minLng, maxLng, limit, offset)
                }
                
                if (result.isSuccess) {
                    val items = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Fetched ${items.size} active items in bounding box from Supabase")
                    callback.onSuccess(items)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to fetch active items in bounding box"
                    Log.e(TAG, "Failed to fetch active items in bounding box: $error")
                    callback.onError(error)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching active items in bounding box", e)
                callback.onError(e.message ?: "Exception fetching items in bounding box")
            }
        }
    }
    
    /**
     * Get user's items from Supabase (bonus method for future use)
     */
    fun getUserItems(userId: String, callback: RepositoryCallback<List<SupabaseItem>>) {
        Log.d(TAG, "Fetching user items from Supabase for user: $userId")
        
        launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getUserItems(userId)
                }
                
                if (result.isSuccess) {
                    val items = result.getOrNull() ?: emptyList()
                    Log.d(TAG, "Fetched ${items.size} items from Supabase")
                    callback.onSuccess(items)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to fetch items"
                    Log.e(TAG, "Failed to fetch user items: $error")
                    callback.onError(error)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching user items", e)
                callback.onError(e.message ?: "Exception fetching items")
            }
        }
    }
    
    /**
     * Delete an item from Supabase (marks as inactive)
     * 
     * @param itemId ID of the item to delete
     * @param userId User ID to verify ownership
     * @param callback Callback for success/error handling
     */
    fun deleteItem(itemId: String, userId: String, callback: RepositoryCallback<Unit>) {
    deleteItemInternal(itemId, userIdFilter = userId, callback)
}

/**
 * Delete an item without owner check (admin/service use only)
 */
fun deleteItemAdmin(itemId: String, callback: RepositoryCallback<Unit>) {
    deleteItemInternal(itemId, userIdFilter = null, callback)
}

private fun deleteItemInternal(itemId: String, userIdFilter: String?, callback: RepositoryCallback<Unit>) {
        Log.d(TAG, "Deleting item from Supabase: $itemId" + if (userIdFilter != null) " for user: $userIdFilter" else " (admin)")
        
        launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    if (userIdFilter != null) {
                        repository.deleteItem(itemId, userIdFilter)
                    } else {
                        repository.deleteItemAdmin(itemId)
                    }
                }
                
                if (result.isSuccess) {
                    Log.d(TAG, "Item deleted successfully from Supabase: $itemId")
                    callback.onSuccess(Unit)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Failed to delete item"
                    Log.e(TAG, "Failed to delete item from Supabase: $error")
                    callback.onError(error)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting item from Supabase", e)
                callback.onError(e.message ?: "Exception deleting item")
            }
        }
    }
    
    /**
     * Clean up coroutines when done
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up SupabaseItemBridge")
        job.cancel()
    }
}
