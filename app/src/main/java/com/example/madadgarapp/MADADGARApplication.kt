package com.example.madadgarapp

import android.app.Application
import android.util.Log
import com.example.madadgarapp.utils.SupabaseClient
import dagger.hilt.android.HiltAndroidApp

/**
 * Custom Application class for MADADGAR App
 * 
 * This class is responsible for initializing global components and services
 * that need to be available throughout the app's lifecycle.
 */
@HiltAndroidApp
class MADADGARApplication : Application() {
    
    companion object {
        private const val TAG = "MADADGARApplication"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "MADADGAR Application starting...")
        
        // Initialize Supabase client
        initializeSupabase()
        
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
}
