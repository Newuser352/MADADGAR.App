package com.example.madadgarapp.utils

import android.content.Context
import android.util.Log
import androidx.annotation.GuardedBy
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.gotrue.providers.builtin.OTP
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.Result

/**
 * SupabaseClient - Singleton for managing Supabase operations
 * 
 * This class provides a centralized way to interact with Supabase services including:
 * - Authentication (GoTrue)
 * - Database operations (Postgrest)
 * - File storage (Storage)
 * 
 * IMPORTANT: You must update the SUPABASE_URL and SUPABASE_ANON_KEY 
 * with your actual project credentials before using this client.
 */
object SupabaseClient {
    
    private const val TAG = "SupabaseClient"
    
    // TODO: Replace these with your actual Supabase project credentials
    // You can find these in your Supabase Dashboard > Settings > API
    // IMPORTANT: You must update these values before using Supabase features
    private const val SUPABASE_URL = "https://crsqhxztqbfguylrgcnt.supabase.co" // e.g., "https://abcdefgh.supabase.co"
    private const val SUPABASE_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImNyc3FoeHp0cWJmZ3V5bHJnY250Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NTA0NDA2ODMsImV4cCI6MjA2NjAxNjY4M30.6uctsEFXC5LhMOU5UXyiorV898F0n5jnH_iuPXuFt6Q" // Your public anon key (starts with 'eyJ')
    
    /**
     * The main Supabase client instance
     * Configured with GoTrue (auth), Postgrest (database), and Storage modules
     */
    val client by lazy {
        if (SUPABASE_URL.isBlank() || SUPABASE_ANON_KEY.isBlank() || 
            SUPABASE_URL == "YOUR_SUPABASE_PROJECT_URL" || SUPABASE_ANON_KEY == "YOUR_SUPABASE_ANON_KEY") {
            Log.e(TAG, "CRITICAL ERROR: Supabase credentials not configured!")
            Log.e(TAG, "Please update SUPABASE_URL and SUPABASE_ANON_KEY in SupabaseClient.kt")
            Log.e(TAG, "Find these values in: Supabase Dashboard > Settings > API")
            throw IllegalStateException("Supabase credentials not configured")
        }
        
        Log.d(TAG, "Initializing Supabase client...")
        Log.d(TAG, "URL: $SUPABASE_URL")
        Log.d(TAG, "Key configured: ${SUPABASE_ANON_KEY.take(20)}...")
        
        createSupabaseClient(
            supabaseUrl = SUPABASE_URL,
            supabaseKey = SUPABASE_ANON_KEY
        ) {
            // Install Auth for authentication
            install(Auth)
            
            // Install Postgrest for database operations
            install(Postgrest)
            
            // Install Storage for file operations
            install(io.github.jan.supabase.storage.Storage)
        }
    }
    
    /**
     * Authentication helper methods
     */
    object AuthHelper {
        
        /**
         * Sign in with Google using ID token
         * 
         * @param idToken Google ID token from Google Sign-In
         * @param accessToken Google access token (optional)
         * @return Result indicating success or failure
         */
        suspend fun signInWithGoogle(idToken: String, accessToken: String? = null): Result<String> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "=== GOOGLE SIGN-IN WITH SUPABASE ===")
                    Log.d(TAG, "ID Token length: ${idToken.length}")
                    Log.d(TAG, "Access Token provided: ${accessToken != null}")
                    
                    // Decode and log ID token payload for debugging audience issues
                    try {
                        val tokenParts = idToken.split(".")
                        if (tokenParts.size >= 2) {
                            val payload = String(android.util.Base64.decode(tokenParts[1], android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING))
                            Log.d(TAG, "ID Token payload (for debugging): $payload")
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not decode ID token for debugging: ${e.message}")
                    }
                    
                    // Sign in with Google using ID token
                    client.auth.signInWith(IDToken) {
                        this.idToken = idToken
                        provider = Google
                    }
                    
                    val user = client.auth.currentUserOrNull()
                    
                    Log.d(TAG, "Google Sign-In successful")
                    Log.d(TAG, "User ID: ${user?.id}")
                    Log.d(TAG, "User Email: ${user?.email}")
                    
                    Result.success("Successfully signed in with Google")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Google Sign-In failed: ${e.message}")
                    Log.e(TAG, "Exception type: ${e.javaClass.simpleName}")
                    if (e.message?.contains("audience", ignoreCase = true) == true) {
                        Log.e(TAG, "AUDIENCE ERROR: This suggests a mismatch between Google OAuth client and Supabase configuration")
                        Log.e(TAG, "Check: 1) Web Client ID in strings.xml matches Supabase, 2) Google Provider is enabled in Supabase with correct client ID")
                    }
                    Result.failure(e)
                }
            }
        }
        
        
        
        /**
         * Sign in with email and password
         * 
         * @param email User's email address
         * @param password User's password
         * @return Result indicating success or failure
         */
        suspend fun signInWithEmailPassword(email: String, password: String): Result<String> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "=== SIGNING IN WITH EMAIL/PASSWORD ===")
                    Log.d(TAG, "Email: $email")
                    
                    client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                    
                    val user = client.auth.currentUserOrNull()
                    
                    Log.d(TAG, "Email/Password sign in successful")
                    Log.d(TAG, "User ID: ${user?.id}")
                    Log.d(TAG, "User Email: ${user?.email}")
                    
                    Result.success("Successfully signed in with email/password")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Email/Password sign in failed", e)
                    Result.failure(e)
                }
            }
        }
        
        /**
         * Sign up with email (sends verification email)
         * 
         * @param email User's email address
         * @return Result indicating success or failure
         */
        suspend fun signUpWithEmail(email: String): Result<String> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "=== SIGNING UP WITH EMAIL ===")
                    Log.d(TAG, "Email: $email")
                    
                    // Use OTP for email verification instead of password signup
                    // This will send a verification email that the user must click
                    client.auth.signInWith(OTP) {
                        this.email = email
                        createUser = true // This will create the user if they don't exist
                    }
                    
                    Log.d(TAG, "Verification email sent successfully to $email")
                    Result.success("Verification email sent to $email")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send verification email", e)
                    Result.failure(e)
                }
            }
        }
        
        /**
         * Sign up with email and password
         * 
         * @param email User's email address
         * @param password User's password
         * @return Result indicating success or failure
         */
        suspend fun signUpWithEmailPassword(email: String, password: String): Result<String> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "=== SIGNING UP WITH EMAIL/PASSWORD ===")
                    Log.d(TAG, "Email: $email")
                    
                    // Sign up with email confirmation required
                    val response = client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                        // Note: Email confirmation is controlled by Supabase project settings
                        // Make sure "Enable email confirmations" is enabled in your Supabase project
                    }
                    
                    val user = client.auth.currentUserOrNull()
                    
                    Log.d(TAG, "Email/Password sign up initiated")
                    Log.d(TAG, "User ID: ${user?.id}")
                    Log.d(TAG, "User Email: ${user?.email}")
                    Log.d(TAG, "Email confirmed: ${user?.emailConfirmedAt != null}")
                    
                    // Check if user needs email confirmation
                    if (user?.emailConfirmedAt == null) {
                        Log.d(TAG, "Email confirmation required - verification email should be sent")
                        Result.success("Account created! Please check your email to verify your account before signing in.")
                    } else {
                        Log.d(TAG, "Account created and email already confirmed")
                        Result.success("Account created successfully!")
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Email/Password sign up failed", e)
                    Result.failure(e)
                }
            }
        }
        
        /**
         * Sign out current user
         * 
         * @return Result indicating success or failure
         */
        suspend fun signOut(): Result<String> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "=== SIGNING OUT ===")
                    
                    client.auth.signOut()
                    
                    Log.d(TAG, "Sign out successful")
                    Result.success("Successfully signed out")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Sign out failed", e)
                    Result.failure(e)
                }
            }
        }
        
        /**
         * Send password reset email
         * 
         * @param email User's email address
         * @return Result indicating success or failure
         */
        suspend fun sendPasswordResetEmail(email: String): Result<String> {
            return withContext(Dispatchers.IO) {
                try {
                    Log.d(TAG, "=== SENDING PASSWORD RESET EMAIL ===")
                    Log.d(TAG, "Email: $email")
                    
                    // Send password reset email using Supabase Auth
                    client.auth.resetPasswordForEmail(email)
                    
                    Log.d(TAG, "Password reset email sent successfully")
                    Result.success("Password reset email sent! Please check your inbox and follow the instructions to reset your password.")
                    
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send password reset email", e)
                    val errorMessage = when {
                        e.message?.contains("Invalid email", ignoreCase = true) == true -> 
                            "Please enter a valid email address"
                        e.message?.contains("User not found", ignoreCase = true) == true -> 
                            "No account found with this email address"
                        e.message?.contains("rate limit", ignoreCase = true) == true -> 
                            "Too many requests. Please wait a few minutes before trying again"
                        else -> "Failed to send password reset email. Please try again later."
                    }
                    Result.failure(Exception(errorMessage))
                }
            }
        }
        
        /**
         * Get current authenticated user
         * 
         * @return Current user or null if not authenticated
         */
        fun getCurrentUser() = client.auth.currentUserOrNull()
        
        /**
         * Check if user is currently authenticated
         * 
         * @return true if authenticated, false otherwise
         */
        fun isAuthenticated() = getCurrentUser() != null
        
        /**
         * Get current session
         * 
         * @return Current session or null if not authenticated
         */
        fun getCurrentSession() = client.auth.currentSessionOrNull()
        
        /**
         * Java-friendly callback interface for authentication operations
         */
        interface AuthCallback {
            fun onSuccess(message: String)
            fun onError(error: String)
        }
        
        /**
         * Java-friendly Google Sign-In method with callback
         * 
         * @param idToken Google ID token from Google Sign-In
         * @param callback Callback to handle success/failure
         */
        @JvmStatic
        fun signInWithGoogleCallback(idToken: String, callback: AuthCallback) {
            // Create a coroutine scope for this operation
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            
            scope.launch {
                try {
                    val result = signInWithGoogle(idToken)
                    
                    if (result.isSuccess) {
                        val message = result.getOrNull() ?: "Successfully signed in with Google"
                        // Switch to main thread for callback
                        withContext(Dispatchers.Main) {
                            callback.onSuccess(message)
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        val errorMessage = error?.message ?: "Unknown error during Google sign-in"
                        // Switch to main thread for callback
                        withContext(Dispatchers.Main) {
                            callback.onError(errorMessage)
                        }
                    }
                } catch (e: Exception) {
                    // Switch to main thread for callback
                    withContext(Dispatchers.Main) {
                        callback.onError(e.message ?: "Exception during Google sign-in")
                    }
                } finally {
                    // Clean up the coroutine scope
                    scope.cancel()
                }
            }
        }
        
        /**
         * Java-friendly password reset method with callback
         * 
         * @param email User's email address
         * @param callback Callback to handle success/failure
         */
        @JvmStatic
        fun sendPasswordResetEmailCallback(email: String, callback: AuthCallback) {
            // Create a coroutine scope for this operation
            val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
            
            scope.launch {
                try {
                    val result = sendPasswordResetEmail(email)
                    
                    if (result.isSuccess) {
                        val message = result.getOrNull() ?: "Password reset email sent successfully"
                        // Switch to main thread for callback
                        withContext(Dispatchers.Main) {
                            callback.onSuccess(message)
                        }
                    } else {
                        val error = result.exceptionOrNull()
                        val errorMessage = error?.message ?: "Failed to send password reset email"
                        // Switch to main thread for callback
                        withContext(Dispatchers.Main) {
                            callback.onError(errorMessage)
                        }
                    }
                } catch (e: Exception) {
                    // Switch to main thread for callback
                    withContext(Dispatchers.Main) {
                        callback.onError(e.message ?: "Exception during password reset")
                    }
                } finally {
                    // Clean up the coroutine scope
                    scope.cancel()
                }
            }
        }
    }
    
    /**
     * Database helper methods
     */
    object Database {
        
        /**
         * Get reference to Postgrest client for database operations
         */
        // Get reference to Postgrest client for database operations
        // Use: client.postgrest["table_name"] for table operations
        
        // Add your database operation methods here
        // Example:
        // suspend fun getUsers() = postgrest.from("users").select().decodeList<User>()
    }
    
    /**
     * Storage helper methods
     */
    object StorageHelper {
        
        /**
         * Get reference to Storage client for file operations
         * Use: client.storage for storage operations
         */
        
        // Add your storage operation methods here
        // Example:
        // suspend fun uploadFile(bucketName: String, fileName: String, file: ByteArray) = 
        //     storage.from(bucketName).upload(fileName, file)
    }
    
    /**
     * Initialize Supabase client (call this in Application class or MainActivity)
     */
    fun initialize() {
        try {
            Log.d(TAG, "Initializing Supabase client...")
            Log.d(TAG, "Supabase URL: $SUPABASE_URL")
            Log.d(TAG, "Auth module: ${client.auth}")
            Log.d(TAG, "Database module: Available")
            Log.d(TAG, "Storage module: Available")
            Log.d(TAG, "Supabase client initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase client", e)
            throw e
        }
    }
}
