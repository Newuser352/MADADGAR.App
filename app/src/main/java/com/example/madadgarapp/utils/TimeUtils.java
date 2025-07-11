package com.example.madadgarapp.utils;

import android.text.format.DateUtils;
import android.util.Log;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * Utility class for handling timestamps and time-related operations
 */
public class TimeUtils {
    
    private static final String TAG = "TimeUtils";
    
    /**
     * Parse timestamp string to long with improved handling for various formats
     * 
     * @param timestamp The timestamp string to parse
     * @return Timestamp in milliseconds, or current time if parsing fails
     */
    public static long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            Log.w(TAG, "Timestamp is null or empty, using current time");
            return System.currentTimeMillis();
        }
        
        try {
            // Try to parse ISO timestamp first (Supabase format: 2024-01-15T10:30:00.000Z)
            return Instant.parse(timestamp).toEpochMilli();
        } catch (Exception e1) {
            try {
                // Try parsing as epoch milliseconds
                return Long.parseLong(timestamp);
            } catch (Exception e2) {
                try {
                    // Try parsing as ISO datetime with timezone
                    return ZonedDateTime.parse(timestamp).toInstant().toEpochMilli();
                } catch (Exception e3) {
                    try {
                        // Try parsing as local datetime (assume UTC)
                        return LocalDateTime.parse(timestamp).atZone(ZoneOffset.UTC).toInstant().toEpochMilli();
                    } catch (Exception e4) {
                        // Log the error for debugging
                        Log.w(TAG, "Failed to parse timestamp: " + timestamp + ", using current time. Error: " + e4.getMessage());
                        // Fallback to current time if all parsing fails
                        return System.currentTimeMillis();
                    }
                }
            }
        }
    }
    
    /**
     * Get relative time string (e.g., "5 minutes ago", "2 hours ago")
     * 
     * @param timestamp The timestamp in milliseconds
     * @return Formatted relative time string
     */
    public static CharSequence getRelativeTimeString(long timestamp) {
        if (timestamp <= 0) {
            return "Unknown time";
        }
        
        try {
            return DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
        } catch (Exception e) {
            Log.w(TAG, "Error formatting relative time for timestamp: " + timestamp, e);
            return "Unknown time";
        }
    }
    
    /**
     * Get relative time string with custom flags
     * 
     * @param timestamp The timestamp in milliseconds
     * @param minResolution Minimum resolution (e.g., DateUtils.MINUTE_IN_MILLIS)
     * @param flags Formatting flags
     * @return Formatted relative time string
     */
    public static CharSequence getRelativeTimeString(long timestamp, long minResolution, int flags) {
        if (timestamp <= 0) {
            return "Unknown time";
        }
        
        try {
            return DateUtils.getRelativeTimeSpanString(
                    timestamp,
                    System.currentTimeMillis(),
                    minResolution,
                    flags
            );
        } catch (Exception e) {
            Log.w(TAG, "Error formatting relative time for timestamp: " + timestamp, e);
            return "Unknown time";
        }
    }
    
    /**
     * Check if a timestamp is valid (not zero or negative)
     * 
     * @param timestamp The timestamp to check
     * @return true if valid, false otherwise
     */
    public static boolean isValidTimestamp(long timestamp) {
        return timestamp > 0;
    }
    
    /**
     * Get current timestamp in milliseconds
     * 
     * @return Current timestamp
     */
    public static long getCurrentTimestamp() {
        return System.currentTimeMillis();
    }
    
    /**
     * Convert timestamp to ISO string format for Supabase
     * 
     * @param timestamp Timestamp in milliseconds
     * @return ISO formatted string
     */
    public static String timestampToIsoString(long timestamp) {
        try {
            return Instant.ofEpochMilli(timestamp).toString();
        } catch (Exception e) {
            Log.w(TAG, "Error converting timestamp to ISO string: " + timestamp, e);
            return Instant.now().toString();
        }
    }
}
