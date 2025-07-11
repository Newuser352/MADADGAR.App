package com.example.madadgarapp.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.madadgarapp.services.FoodItemExpiryService;

/**
 * Utility class to test and demonstrate the Food expiry feature
 * This class provides methods to manually trigger expiry checks and test the feature
 */
public class FoodExpiryTester {
    
    private static final String TAG = "FoodExpiryTester";
    
    /**
     * Test the Food expiry feature by running an immediate cleanup
     * 
     * @param context Application context
     */
    public static void testFoodExpiry(Context context) {
        Log.d(TAG, "Testing Food expiry feature...");
        
        try {
            // Schedule an immediate job to test the expiry cleanup
            boolean success = FoodExpiryScheduler.scheduleImmediateExpiryJob(context);
            
            if (success) {
                Log.d(TAG, "Immediate expiry job scheduled successfully");
                Toast.makeText(context, "Running expiry cleanup test...", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to schedule immediate expiry job");
                Toast.makeText(context, "Failed to start expiry test", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error testing food expiry", e);
            Toast.makeText(context, "Error testing expiry: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Check if the expiry scheduler is properly set up
     * 
     * @param context Application context
     * @return true if scheduler is working, false otherwise
     */
    public static boolean isExpirySchedulerWorking(Context context) {
        try {
            boolean isScheduled = FoodExpiryScheduler.isExpiryJobScheduled(context);
            Log.d(TAG, "Expiry scheduler status: " + (isScheduled ? "Running" : "Not running"));
            return isScheduled;
        } catch (Exception e) {
            Log.e(TAG, "Error checking expiry scheduler status", e);
            return false;
        }
    }
    
    /**
     * Show information about the current expiry scheduler setup
     * 
     * @param context Application context
     */
    public static void showExpiryInfo(Context context) {
        try {
            boolean isScheduled = isExpirySchedulerWorking(context);
            
            String message = "Food Expiry Scheduler Status:\n\n" +
                           "• Scheduled: " + (isScheduled ? "✓ Yes" : "✗ No") + "\n" +
                           "• Runs every: 30 minutes\n" +
                           "• Checks for: Expired Food items\n" +
                           "• Action: Automatic deletion\n\n" +
                           "Food items will be automatically deleted after their expiry time to keep fresh items visible.";
            
            Log.d(TAG, "Expiry scheduler info: " + message);
            
            // Show detailed job info in logs
            FoodExpiryScheduler.logScheduledJobs(context);
            
            Toast.makeText(context, 
                          "Expiry Scheduler: " + (isScheduled ? "Running" : "Not running"), 
                          Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e(TAG, "Error showing expiry info", e);
            Toast.makeText(context, "Error getting expiry info: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Reset the expiry scheduler (cancel and reschedule)
     * 
     * @param context Application context
     */
    public static void resetExpiryScheduler(Context context) {
        try {
            Log.d(TAG, "Resetting expiry scheduler...");
            
            // Cancel existing job
            FoodExpiryScheduler.cancelExpiryJob(context);
            
            // Schedule new job
            boolean success = FoodExpiryScheduler.scheduleExpiryJob(context);
            
            if (success) {
                Log.d(TAG, "Expiry scheduler reset successfully");
                Toast.makeText(context, "Expiry scheduler reset successfully", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "Failed to reset expiry scheduler");
                Toast.makeText(context, "Failed to reset expiry scheduler", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error resetting expiry scheduler", e);
            Toast.makeText(context, "Error resetting scheduler: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Calculate expiry time for a food item
     * 
     * @param hoursOrDays Number of hours or days
     * @param isHours true for hours, false for days
     * @return Expiry time in milliseconds
     */
    public static long calculateExpiryTime(int hoursOrDays, boolean isHours) {
        long currentTime = System.currentTimeMillis();
        
        if (isHours) {
            return currentTime + (hoursOrDays * 60 * 60 * 1000L);
        } else {
            return currentTime + (hoursOrDays * 24 * 60 * 60 * 1000L);
        }
    }
    
    /**
     * Format expiry time for display
     * 
     * @param expiryTime Expiry time in milliseconds
     * @return Formatted string showing when item will expire
     */
    public static String formatExpiryTime(long expiryTime) {
        try {
            java.util.Date date = new java.util.Date(expiryTime);
            java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault());
            return "Will expire on " + format.format(date);
        } catch (Exception e) {
            Log.e(TAG, "Error formatting expiry time", e);
            return "Expiry time unavailable";
        }
    }
}
