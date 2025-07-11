package com.example.madadgarapp.utils;

import android.util.Log;
import android.webkit.URLUtil;

import java.util.regex.Pattern;

/**
 * Utility class for media URL validation and processing
 */
public class MediaUtils {
    
    private static final String TAG = "MediaUtils";
    
    // Common image file extensions
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
        ".*\\.(jpg|jpeg|png|gif|bmp|webp)$", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Common video file extensions
    private static final Pattern VIDEO_PATTERN = Pattern.compile(
        ".*\\.(mp4|avi|mkv|mov|wmv|flv|webm|3gp)$", 
        Pattern.CASE_INSENSITIVE
    );
    
    // Supabase storage URL pattern
    private static final Pattern SUPABASE_URL_PATTERN = Pattern.compile(
        "https://[\\w-]+\\.supabase\\.co/storage/v1/object/public/.*",
        Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Validates if a URL is a valid media URL
     * 
     * @param url The URL to validate
     * @return true if valid, false otherwise
     */
    public static boolean isValidMediaUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            Log.w(TAG, "URL is null or empty");
            return false;
        }
        
        // Check if URL is properly formatted
        if (!URLUtil.isValidUrl(url)) {
            Log.w(TAG, "Invalid URL format: " + url);
            return false;
        }
        
        // Check if it's a supported media format
        if (!isImage(url) && !isVideo(url)) {
            Log.w(TAG, "Unsupported media format: " + url);
            return false;
        }
        
        Log.d(TAG, "Valid media URL: " + url);
        return true;
    }
    
    /**
     * Checks if URL points to an image
     * 
     * @param url The URL to check
     * @return true if it's an image URL
     */
    public static boolean isImage(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        return IMAGE_PATTERN.matcher(url).matches() || 
               url.contains("image") || 
               isSupabaseImageUrl(url);
    }
    
    /**
     * Checks if URL points to a video
     * 
     * @param url The URL to check
     * @return true if it's a video URL
     */
    public static boolean isVideo(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        
        return VIDEO_PATTERN.matcher(url).matches() || 
               url.contains("video") || 
               isSupabaseVideoUrl(url);
    }
    
    /**
     * Checks if URL is from Supabase storage for images
     * 
     * @param url The URL to check
     * @return true if it's a Supabase image URL
     */
    private static boolean isSupabaseImageUrl(String url) {
        return SUPABASE_URL_PATTERN.matcher(url).matches() && 
               url.contains("item-images");
    }
    
    /**
     * Checks if URL is from Supabase storage for videos
     * 
     * @param url The URL to check
     * @return true if it's a Supabase video URL
     */
    private static boolean isSupabaseVideoUrl(String url) {
        return SUPABASE_URL_PATTERN.matcher(url).matches() && 
               url.contains("item-videos");
    }
    
    /**
     * Cleans and processes a media URL
     * 
     * @param url The URL to process
     * @return The processed URL
     */
    public static String processMediaUrl(String url) {
        if (url == null) {
            return null;
        }
        
        // Trim whitespace
        url = url.trim();
        
        // If empty after trimming, return null
        if (url.isEmpty()) {
            return null;
        }
        
        // Log the processed URL
        Log.d(TAG, "Processed media URL: " + url);
        
        return url;
    }
    
    /**
     * Gets a descriptive name for the media type
     * 
     * @param url The media URL
     * @return Description of the media type
     */
    public static String getMediaTypeDescription(String url) {
        if (url == null || url.isEmpty()) {
            return "Unknown";
        }
        
        if (isImage(url)) {
            return "Image";
        } else if (isVideo(url)) {
            return "Video";
        } else {
            return "Media";
        }
    }
    
    /**
     * Extracts file extension from URL
     * 
     * @param url The URL to extract extension from
     * @return The file extension (without dot) or null if not found
     */
    public static String getFileExtension(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        // Remove query parameters
        int queryIndex = url.indexOf('?');
        if (queryIndex != -1) {
            url = url.substring(0, queryIndex);
        }
        
        // Get file extension
        int lastDot = url.lastIndexOf('.');
        if (lastDot > 0 && lastDot < url.length() - 1) {
            return url.substring(lastDot + 1).toLowerCase();
        }
        
        return null;
    }
}
