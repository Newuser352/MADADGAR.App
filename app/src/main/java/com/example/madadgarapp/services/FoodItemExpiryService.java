package com.example.madadgarapp.services;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.madadgarapp.repository.SupabaseItemBridge;
import com.example.madadgarapp.models.SupabaseItem;
import com.example.madadgarapp.utils.TimeUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Background service to automatically delete expired Food items.
 * This service runs periodically to check for expired items and marks them as inactive.
 */
public class FoodItemExpiryService extends JobService {

    private static final String TAG = "FoodItemExpiryService";
    public static final int JOB_ID = 1001;
    
    private volatile boolean jobCancelled = false;
    private SupabaseItemBridge supabaseItemBridge;

    @Override
    public void onCreate() {
        super.onCreate();
        supabaseItemBridge = new SupabaseItemBridge();
        Log.d(TAG, "FoodItemExpiryService created");
    }

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(TAG, "Starting food item expiry check job");
        
        // Run the cleanup task in background thread
        new Thread(() -> {
            performExpiryCleanup(params);
        }).start();
        
        return true; // Job is ongoing
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG, "Job stopped, cancelling operations");
        jobCancelled = true;
        return false; // Don't retry
    }

    /**
     * Perform the actual expiry cleanup
     */
    private void performExpiryCleanup(JobParameters params) {
        try {
            Log.d(TAG, "Starting expiry cleanup process");
            
            // Get all active items to check for expired ones
            supabaseItemBridge.getActiveItems(1000, 0, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
                @Override
                public void onSuccess(List<SupabaseItem> items) {
                    if (jobCancelled) {
                        Log.d(TAG, "Job cancelled, stopping cleanup");
                        jobFinished(params, false);
                        return;
                    }
                    
                    processExpiredItems(items, params);
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error fetching items for expiry check: " + error);
                    jobFinished(params, true); // Reschedule on error
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Exception in expiry cleanup", e);
            jobFinished(params, true); // Reschedule on error
        }
    }

    /**
     * Process the items and delete expired ones
     */
    private void processExpiredItems(List<SupabaseItem> items, JobParameters params) {
        if (items == null || items.isEmpty()) {
            Log.d(TAG, "No items to process for expiry");
            jobFinished(params, false);
            return;
        }

        Log.d(TAG, "Processing " + items.size() + " items for expiry");
        
        // Filter for expired Food items
        long currentTime = System.currentTimeMillis();
        AtomicInteger processedCount = new AtomicInteger(0);
        AtomicInteger deletedCount = new AtomicInteger(0);
        AtomicInteger totalExpiredItems = new AtomicInteger(0);
        
        // First pass: count expired items
        for (SupabaseItem item : items) {
            if (isExpiredFoodItem(item, currentTime)) {
                totalExpiredItems.incrementAndGet();
            }
        }
        
        if (totalExpiredItems.get() == 0) {
            Log.d(TAG, "No expired food items found");
            jobFinished(params, false);
            return;
        }
        
        Log.d(TAG, "Found " + totalExpiredItems.get() + " expired food items");
        
        // Second pass: delete expired items
        for (SupabaseItem item : items) {
            if (jobCancelled) {
                Log.d(TAG, "Job cancelled during processing");
                jobFinished(params, false);
                return;
            }
            
            if (isExpiredFoodItem(item, currentTime)) {
                deleteExpiredItem(item, processedCount, deletedCount, totalExpiredItems.get(), params);
            }
        }
        
        // If no expired items to process, finish immediately
        if (totalExpiredItems.get() == 0) {
            jobFinished(params, false);
        }
    }

    /**
     * Check if an item is an expired Food item
     */
    private boolean isExpiredFoodItem(SupabaseItem item, long currentTime) {
        // Only check Food items
        if (!"Food".equals(item.getMainCategory())) {
            return false;
        }
        
        // Check if item has expiry time
        if (item.getExpiresAt() == null || item.getExpiresAt().isEmpty()) {
            return false;
        }
        
        try {
            // Parse expiry time
            long expiryTime = TimeUtils.parseTimestamp(item.getExpiresAt());
            
            // Check if expired
            boolean isExpired = expiryTime < currentTime;
            
            if (isExpired) {
                Log.d(TAG, "Expired food item found: " + item.getTitle() + 
                      " (expired: " + TimeUtils.getRelativeTimeString(expiryTime) + ")");
            }
            
            return isExpired;
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing expiry time for item: " + item.getTitle(), e);
            return false;
        }
    }

    /**
     * Delete an expired item
     */
    private void deleteExpiredItem(SupabaseItem item, AtomicInteger processedCount, 
                                 AtomicInteger deletedCount, int totalExpiredItems, JobParameters params) {
        
        Log.d(TAG, "Deleting expired food item: " + item.getTitle());
        
        supabaseItemBridge.deleteItemAdmin(item.getId(), 
            new SupabaseItemBridge.RepositoryCallback<kotlin.Unit>() {
                @Override
                public void onSuccess(kotlin.Unit result) {
                    deletedCount.incrementAndGet();
                    int processed = processedCount.incrementAndGet();
                    
                    Log.d(TAG, "Successfully deleted expired item: " + item.getTitle() + 
                          " (" + processed + "/" + totalExpiredItems + ")");
                    
                    // Check if all items have been processed
                    if (processed >= totalExpiredItems) {
                        Log.d(TAG, "Expiry cleanup completed. Deleted " + deletedCount.get() + 
                              " out of " + totalExpiredItems + " expired items");
                        jobFinished(params, false);
                    }
                }

                @Override
                public void onError(String error) {
                    int processed = processedCount.incrementAndGet();
                    Log.e(TAG, "Failed to delete expired item: " + item.getTitle() + 
                          " - Error: " + error);
                    
                    // Continue processing even if some deletions fail
                    if (processed >= totalExpiredItems) {
                        Log.d(TAG, "Expiry cleanup completed with errors. Deleted " + deletedCount.get() + 
                              " out of " + totalExpiredItems + " expired items");
                        jobFinished(params, false);
                    }
                }
            });
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "FoodItemExpiryService destroyed");
        jobCancelled = true;
        
        // Clean up resources
        if (supabaseItemBridge != null) {
            supabaseItemBridge.cleanup();
        }
        
        super.onDestroy();
    }
}
