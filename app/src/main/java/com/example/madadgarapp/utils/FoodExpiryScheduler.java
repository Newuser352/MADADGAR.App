package com.example.madadgarapp.utils;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import com.example.madadgarapp.services.FoodItemExpiryService;

import java.util.concurrent.TimeUnit;

/**
 * Utility class to schedule and manage the Food item expiry cleanup job
 */
public class FoodExpiryScheduler {
    
    private static final String TAG = "FoodExpiryScheduler";
    
    // Schedule job to run every 30 minutes
    private static final long INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(30);
    
    // Allow for some flexibility in scheduling (15 minutes)
    private static final long FLEX_MILLIS = TimeUnit.MINUTES.toMillis(15);
    
    /**
     * Schedule the food expiry cleanup job
     * 
     * @param context Application context
     * @return true if job was scheduled successfully, false otherwise
     */
    public static boolean scheduleExpiryJob(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler service not available");
                return false;
            }
            
            // Check if job is already scheduled
            if (isJobScheduled(jobScheduler, FoodItemExpiryService.JOB_ID)) {
                Log.d(TAG, "Food expiry job already scheduled");
                return true;
            }
            
            ComponentName componentName = new ComponentName(context, FoodItemExpiryService.class);
            
            JobInfo jobInfo = new JobInfo.Builder(FoodItemExpiryService.JOB_ID, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) // Requires network connectivity
                    .setPersisted(true) // Persist across device restarts
                    .setPeriodic(INTERVAL_MILLIS, FLEX_MILLIS) // Run every 30 minutes with 15-minute flexibility
                    .setRequiresCharging(false) // Can run without charging
                    .setRequiresDeviceIdle(false) // Can run when device is not idle
                    .build();
            
            int result = jobScheduler.schedule(jobInfo);
            
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Food expiry job scheduled successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to schedule food expiry job. Result: " + result);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception scheduling food expiry job", e);
            return false;
        }
    }
    
    /**
     * Schedule the food expiry cleanup job to run immediately (for testing)
     * 
     * @param context Application context
     * @return true if job was scheduled successfully, false otherwise
     */
    public static boolean scheduleImmediateExpiryJob(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler service not available");
                return false;
            }
            
            ComponentName componentName = new ComponentName(context, FoodItemExpiryService.class);
            
            // Create a one-time job that runs immediately
            JobInfo jobInfo = new JobInfo.Builder(FoodItemExpiryService.JOB_ID + 1, componentName)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setOverrideDeadline(1000) // Run within 1 second
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false)
                    .build();
            
            int result = jobScheduler.schedule(jobInfo);
            
            if (result == JobScheduler.RESULT_SUCCESS) {
                Log.d(TAG, "Immediate food expiry job scheduled successfully");
                return true;
            } else {
                Log.e(TAG, "Failed to schedule immediate food expiry job. Result: " + result);
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception scheduling immediate food expiry job", e);
            return false;
        }
    }
    
    /**
     * Cancel the food expiry cleanup job
     * 
     * @param context Application context
     * @return true if job was cancelled successfully, false otherwise
     */
    public static boolean cancelExpiryJob(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.e(TAG, "JobScheduler service not available");
                return false;
            }
            
            jobScheduler.cancel(FoodItemExpiryService.JOB_ID);
            Log.d(TAG, "Food expiry job cancelled");
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Exception cancelling food expiry job", e);
            return false;
        }
    }
    
    /**
     * Check if the food expiry job is currently scheduled
     * 
     * @param context Application context
     * @return true if job is scheduled, false otherwise
     */
    public static boolean isExpiryJobScheduled(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                return false;
            }
            
            return isJobScheduled(jobScheduler, FoodItemExpiryService.JOB_ID);
            
        } catch (Exception e) {
            Log.e(TAG, "Exception checking if food expiry job is scheduled", e);
            return false;
        }
    }
    
    /**
     * Helper method to check if a job with given ID is scheduled
     */
    private static boolean isJobScheduled(JobScheduler jobScheduler, int jobId) {
        try {
            for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
                if (jobInfo.getId() == jobId) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Exception checking job schedule status", e);
            return false;
        }
    }
    
    /**
     * Get information about all scheduled jobs (for debugging)
     */
    public static void logScheduledJobs(Context context) {
        try {
            JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
            if (jobScheduler == null) {
                Log.d(TAG, "JobScheduler service not available");
                return;
            }
            
            var pendingJobs = jobScheduler.getAllPendingJobs();
            Log.d(TAG, "Total scheduled jobs: " + pendingJobs.size());
            
            for (JobInfo jobInfo : pendingJobs) {
                Log.d(TAG, "Job ID: " + jobInfo.getId() + 
                      ", Component: " + jobInfo.getService().getClassName() +
                      ", Periodic: " + jobInfo.isPeriodic() +
                      ", Interval: " + jobInfo.getIntervalMillis() + "ms");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Exception logging scheduled jobs", e);
        }
    }
}
