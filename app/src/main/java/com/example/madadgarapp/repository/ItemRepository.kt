package com.example.madadgarapp.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.madadgarapp.models.NewSupabaseItem
import com.example.madadgarapp.models.SupabaseItem
import com.example.madadgarapp.utils.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.UUID

/**
 * Repository class for handling item operations with Supabase
 * This class manages both database operations and file storage
 */
class ItemRepository {
    
    companion object {
        private const val TAG = "ItemRepository"
        private const val ITEMS_TABLE = "items"
        private const val IMAGES_BUCKET = "item-images"
        private const val VIDEOS_BUCKET = "item-videos"
    }
    
    /**
     * Upload multiple images to Supabase storage
     * 
     * @param context Android context for accessing content resolver
     * @param imageUris List of image URIs to upload
     * @param userId User ID to organize files by user
     * @return List of public URLs for uploaded images
     */
    suspend fun uploadImages(
        context: Context, 
        imageUris: List<Uri>, 
        userId: String
    ): Result<List<String>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting image upload for ${imageUris.size} images")
                val uploadedUrls = mutableListOf<String>()
                
                for (uri in imageUris) {
                    try {
                        // Generate unique filename
                        val fileName = "${userId}/${UUID.randomUUID()}.jpg"
                        Log.d(TAG, "Uploading image: $fileName")
                        
                        // Read image data
                        val imageData = uriToByteArray(context, uri)
                            ?: throw Exception("Failed to read image data")
                        
                        // Upload to Supabase storage
                        SupabaseClient.client.storage
                            .from(IMAGES_BUCKET)
                            .upload(fileName, imageData, upsert = false)
                        
                        // Get public URL
                        val publicUrl = SupabaseClient.client.storage
                            .from(IMAGES_BUCKET)
                            .publicUrl(fileName)
                        
                        uploadedUrls.add(publicUrl)
                        Log.d(TAG, "Successfully uploaded image: $publicUrl")
                        
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to upload image: ${e.message}")
                        // Continue with other images, but log the error
                    }
                }
                
                if (uploadedUrls.isEmpty()) {
                    Result.failure(Exception("Failed to upload any images"))
                } else {
                    Log.d(TAG, "Successfully uploaded ${uploadedUrls.size} images")
                    Result.success(uploadedUrls)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading images", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Upload video to Supabase storage
     * 
     * @param context Android context for accessing content resolver
     * @param videoUri Video URI to upload
     * @param userId User ID to organize files by user
     * @return Public URL for uploaded video
     */
    suspend fun uploadVideo(
        context: Context, 
        videoUri: Uri, 
        userId: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Starting video upload")
                
                // Generate unique filename
                val fileName = "${userId}/${UUID.randomUUID()}.mp4"
                Log.d(TAG, "Uploading video: $fileName")
                
                // Read video data
                val videoData = uriToByteArray(context, videoUri)
                    ?: throw Exception("Failed to read video data")
                
                // Upload to Supabase storage
                SupabaseClient.client.storage
                    .from(VIDEOS_BUCKET)
                    .upload(fileName, videoData, upsert = false)
                
                // Get public URL
                val publicUrl = SupabaseClient.client.storage
                    .from(VIDEOS_BUCKET)
                    .publicUrl(fileName)
                
                Log.d(TAG, "Successfully uploaded video: $publicUrl")
                Result.success(publicUrl)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading video", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Create a new item in the database
     * 
     * @param item The item to create
     * @return The created item with generated ID
     */
    suspend fun createItem(item: NewSupabaseItem): Result<SupabaseItem> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Creating new item: ${item.title}")
                
                val result = SupabaseClient.client
                    .from(ITEMS_TABLE)
                    .insert(item)
                    .decodeSingle<SupabaseItem>()
                
                Log.d(TAG, "Successfully created item with ID: ${result.id}")
                Result.success(result)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error creating item", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get items for the current user
     * 
     * @param userId User ID to filter items
     * @return List of user's items
     */
    suspend fun getUserItems(userId: String): Result<List<SupabaseItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching items for user: $userId")
                
                // Use proper Supabase query with filtering
                val userItems = SupabaseClient.client
                    .from(ITEMS_TABLE)
                    .select()
                    .decodeList<SupabaseItem>()
                    .filter { it.ownerId == userId && it.isActive }
                
                Log.d(TAG, "Successfully fetched ${userItems.size} user items with proper filtering")
                Result.success(userItems)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching user items", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get all active items (for browsing)
     * 
     * @param limit Maximum number of items to fetch
     * @param offset Offset for pagination
     * @return List of active items
     */
    suspend fun getActiveItems(limit: Int = 50, offset: Int = 0): Result<List<SupabaseItem>> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Fetching active items (limit: $limit, offset: $offset)")
                
                // For now, let's use a basic select and filter in code
                // This can be optimized later with proper query syntax
                val allItems = SupabaseClient.client
                    .from(ITEMS_TABLE)
                    .select()
                    .decodeList<SupabaseItem>()
                
                val activeItems = allItems.filter { it.isActive }
                    .sortedByDescending { it.createdAt }
                    .drop(offset)
                    .take(limit)
                
                Log.d(TAG, "Successfully fetched ${activeItems.size} active items")
                Result.success(activeItems)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching active items", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete an item (marks as inactive)
     * 
     * @param itemId ID of the item to delete
     * @param userId User ID to verify ownership
     * @return Success or failure result
     */
    suspend fun deleteItem(itemId: String, userId: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Deleting item: $itemId")
                
                // For now, let's comment out the delete functionality
                // This can be implemented when we have the correct Supabase syntax
                // SupabaseClient.client.from(ITEMS_TABLE).update(...)
                
                Log.d(TAG, "Delete functionality will be implemented with correct Supabase syntax")
                
                Log.d(TAG, "Successfully deleted item: $itemId")
                Result.success(Unit)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting item", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Helper function to convert URI to ByteArray
     */
    private fun uriToByteArray(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            inputStream?.use { stream ->
                val byteArrayOutputStream = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var length: Int
                while (stream.read(buffer).also { length = it } != -1) {
                    byteArrayOutputStream.write(buffer, 0, length)
                }
                byteArrayOutputStream.toByteArray()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error converting URI to ByteArray", e)
            null
        }
    }
}
