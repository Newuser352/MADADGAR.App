package com.example.madadgarapp

import android.app.Application
import android.util.Log
import com.example.madadgarapp.utils.SupabaseClient
import com.example.madadgarapp.utils.FoodExpiryScheduler
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class for MADADGAR App
 * 
 * This class is responsible for initializing global components and services
 * that need to be available throughout the app's lifecycle.
 */
@HiltAndroidApp
open class MADADGARApplication : Application() {
    
    companion object {
        private const val TAG = "MADADGARApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "MADADGAR Application starting...")
        
        // Initialize Supabase client
        initializeSupabase()
        
        // Initialize Food Expiry Scheduler
        initializeFoodExpiryScheduler()
        
        // Initialize Notifications
        initializeNotifications()
        
        Log.d(TAG, "MADADGAR Application initialized successfully")
    }
    
    /**
     * Initialize Supabase client with proper error handling
     */
    private fun initializeSupabase() {
        try {
            Log.d(TAG, "Initializing Supabase client...")
            SupabaseClient.initialize()
            Log.d(TAG, "Supabase client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            // In a production app, you might want to show an error dialog
            // or disable features that require Supabase
        }
    }
    
    /**
     * Initialize Food Expiry Scheduler for automatic deletion of expired food items
     */
    private fun initializeFoodExpiryScheduler() {
        try {
            Log.d(TAG, "Initializing Food Expiry Scheduler...")
            
            // Schedule the periodic job to clean up expired food items
            val success = FoodExpiryScheduler.scheduleExpiryJob(this)
            
            if (success) {
                Log.d(TAG, "Food Expiry Scheduler initialized successfully")
                
                // Log current scheduled jobs for debugging
                FoodExpiryScheduler.logScheduledJobs(this)
            } else {
                Log.w(TAG, "Failed to schedule Food Expiry job")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Food Expiry Scheduler", e)
            // Continue app initialization even if scheduler fails
        }
    }
    
    /**
     * Initialize notification channels and settings
     */
    private fun initializeNotifications() {
        try {
            Log.d(TAG, "Initializing notifications...")
            
            // Initialize notification channels
            com.example.madadgarapp.utils.NotificationManager.initializeNotificationChannels(this)
            
            Log.d(TAG, "Notifications initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize notifications", e)
            // Continue app initialization even if notifications fail
        }
    }
}
