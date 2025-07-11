package com.example.madadgarapp.utils

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject
import io.github.jan.supabase.gotrue.SessionStatus


/**
 * AuthManager - Modern authentication manager following Android best practices
 * 
 * This class provides a reactive, lifecycle-aware approach to authentication
 * using StateFlow and LiveData for state management.
 * 
 * Features:
 * - Lifecycle-aware authentication state
 * - Reactive state management with StateFlow
 * - Proper error handling and logging
 * - Thread-safe operations
 * - Follows MVVM architecture patterns
 * - Uses Hilt for dependency injection
 * 
 * Usage in Activity/Fragment:
 * ```
 * @AndroidEntryPoint
 * class MyActivity : AppCompatActivity() {
 *     private val authManager: AuthManager by viewModels()
 *     
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         super.onCreate(savedInstanceState)
 *         
 *         lifecycleScope.launch {
 *             authManager.authState.collect { state ->
 *                 when (state) {
 *                     is AuthState.Loading -> showLoading()
 *                     is AuthState.Authenticated -> navigateToMain()
 *                     is AuthState.Unauthenticated -> showLogin()
 *                     is AuthState.Error -> showError(state.message)
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 */
@HiltViewModel
class AuthManager @Inject constructor(
) : ViewModel() {
    
    companion object {
        private const val TAG = "AuthManager"
        private const val RC_SIGN_IN = 1001
    }
    
    // Exception handler for coroutines
    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        Log.e(TAG, "Coroutine exception occurred", exception)
        _authState.value = AuthState.Error(exception.message ?: "Unknown error occurred")
        _authLiveData.value = AuthState.Error(exception.message ?: "Unknown error occurred")
    }
    
    // Authentication state using StateFlow for reactive updates
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    // LiveData for backward compatibility with existing code
    private val _authLiveData = MutableLiveData<AuthState>(AuthState.Unauthenticated)
    val authLiveData: LiveData<AuthState> = _authLiveData
    
    // User information state
    private val _currentUser = MutableStateFlow<UserInfo?>(null)
    val currentUser: StateFlow<UserInfo?> = _currentUser.asStateFlow()
    
    /**
     * Sealed class representing different authentication states
     */
    sealed class AuthState {
        object Loading : AuthState()
        object Authenticated : AuthState()
        object Unauthenticated : AuthState()
        data class Error(val message: String) : AuthState()
    }
    
    /**
     * Data class for user information
     */
    data class UserInfo(
        val id: String,
        val email: String?,
        val name: String?,
        val photoUrl: String?,
        val provider: String // "google", "email", etc.
    )
    
    /**
     * Initialize authentication manager
     */
    init {
        Log.d(TAG, "AuthManager initialized")
        // Start monitoring session status immediately
        startSessionMonitoring()
        // Check initial authentication status
        checkAuthenticationStatus()
    }
    
    /**
     * Start monitoring authentication status with periodic checks
     */
    private fun startSessionMonitoring() {
        viewModelScope.launch(exceptionHandler) {
            try {
                Log.d(TAG, "Starting authentication monitoring...")
                
                // Initial check
                checkAuthenticationStatusInternal()
                
                // Periodic checks every 30 seconds to ensure session persistence
                while (true) {
                    delay(30000) // 30 seconds
                    checkAuthenticationStatusInternal()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in authentication monitoring", e)
                // Fallback to manual checking if monitoring fails
                checkAuthenticationStatus()
            }
        }
    }
    
    /**
     * Sign in with email and password
     * 
     * @param email User's email address
     * @param password User's password
     */
    fun signInWithEmailPassword(email: String, password: String) {
        Log.d(TAG, "=== SIGNING IN WITH EMAIL/PASSWORD ===")
        Log.d(TAG, "Email: $email")
        
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email and password are required")
            _authLiveData.value = AuthState.Error("Email and password are required")
            return
        }
        
        viewModelScope.launch(exceptionHandler) {
            _authState.value = AuthState.Loading
            _authLiveData.value = AuthState.Loading
            
            try {
                val result = SupabaseClient.AuthHelper.signInWithEmailPassword(email, password)
                
                result.fold(
                    onSuccess = { message ->
                        Log.d(TAG, "Sign in successful: $message")
                        
                        // Get user information
                        val user = SupabaseClient.AuthHelper.getCurrentUser()
                        if (user != null) {
                            _currentUser.value = UserInfo(
                                id = user.id,
                                email = user.email,
                                name = user.userMetadata?.get("full_name")?.toString(),
                                photoUrl = null,
                                provider = "email"
                            )
                        }
                        
                        _authState.value = AuthState.Authenticated
                        _authLiveData.value = AuthState.Authenticated
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Sign in failed", exception)
                        _authState.value = AuthState.Error(exception.message ?: "Sign in failed")
                        _authLiveData.value = AuthState.Error(exception.message ?: "Sign in failed")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sign in", e)
                _authState.value = AuthState.Error(e.message ?: "Unexpected error occurred")
                _authLiveData.value = AuthState.Error(e.message ?: "Unexpected error occurred")
            }
        }
    }
    
    /**
     * Sign up with email (sends verification email)
     * 
     * @param email User's email address
     */
    fun signUpWithEmail(email: String) {
        Log.d(TAG, "=== SIGNING UP WITH EMAIL ===")
        Log.d(TAG, "Email: $email")
        
        if (email.isEmpty()) {
            _authState.value = AuthState.Error("Email is required")
            _authLiveData.value = AuthState.Error("Email is required")
            return
        }
        
        viewModelScope.launch(exceptionHandler) {
            _authState.value = AuthState.Loading
            _authLiveData.value = AuthState.Loading
            
            try {
                val result = SupabaseClient.AuthHelper.signUpWithEmail(email)
                
                result.fold(
                    onSuccess = { message ->
                        Log.d(TAG, "Verification email sent successfully: $message")
                        _authState.value = AuthState.Unauthenticated // Stay unauthenticated until email is verified
                        _authLiveData.value = AuthState.Unauthenticated
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to send verification email", exception)
                        _authState.value = AuthState.Error(exception.message ?: "Failed to send verification email")
                        _authLiveData.value = AuthState.Error(exception.message ?: "Failed to send verification email")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sign up", e)
                _authState.value = AuthState.Error(e.message ?: "Unexpected error occurred")
                _authLiveData.value = AuthState.Error(e.message ?: "Unexpected error occurred")
            }
        }
    }
    
    /**
     * Sign up with email and password
     * 
     * @param email User's email address
     * @param password User's password
     */
    fun signUpWithEmailPassword(email: String, password: String) {
        Log.d(TAG, "=== SIGNING UP WITH EMAIL AND PASSWORD ===")
        Log.d(TAG, "Email: $email")
        
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email and password are required")
            _authLiveData.value = AuthState.Error("Email and password are required")
            return
        }
        
        viewModelScope.launch(exceptionHandler) {
            _authState.value = AuthState.Loading
            _authLiveData.value = AuthState.Loading
            
            try {
                val result = SupabaseClient.AuthHelper.signUpWithEmailPassword(email, password)
                
                result.fold(
                    onSuccess = { message ->
                        Log.d(TAG, "Sign up successful: $message")
                        
                        // Get user information
                        val user = SupabaseClient.AuthHelper.getCurrentUser()
                        if (user != null) {
                            _currentUser.value = UserInfo(
                                id = user.id,
                                email = user.email,
                                name = user.userMetadata?.get("full_name")?.toString(),
                                photoUrl = null,
                                provider = "email"
                            )
                        }
                        
                        _authState.value = AuthState.Authenticated
                        _authLiveData.value = AuthState.Authenticated
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Sign up failed", exception)
                        _authState.value = AuthState.Error(exception.message ?: "Sign up failed")
                        _authLiveData.value = AuthState.Error(exception.message ?: "Sign up failed")
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during sign up", e)
                _authState.value = AuthState.Error(e.message ?: "Unexpected error occurred")
                _authLiveData.value = AuthState.Error(e.message ?: "Unexpected error occurred")
            }
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        viewModelScope.launch(exceptionHandler) {
            try {
                // Sign out from Supabase
                val result = SupabaseClient.AuthHelper.signOut()
                
                result.fold(
                    onSuccess = {
                        _currentUser.value = null
                        _authState.value = AuthState.Unauthenticated
                        _authLiveData.value = AuthState.Unauthenticated
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Error signing out", exception)
                        _authState.value = AuthState.Error(exception.message ?: "Error signing out")
                        _authLiveData.value = AuthState.Error(exception.message ?: "Error signing out")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error signing out", e)
                _authState.value = AuthState.Error(e.message ?: "Error signing out")
                _authLiveData.value = AuthState.Error(e.message ?: "Error signing out")
            }
        }
    }
    
    /**
     * Check current authentication status
     */
    private fun checkAuthenticationStatus() {
        viewModelScope.launch(exceptionHandler) {
            checkAuthenticationStatusInternal()
        }
    }
    
    /**
     * Internal method to check authentication status with proper session restoration
     */
    private suspend fun checkAuthenticationStatusInternal() {
        try {
            Log.d(TAG, "Checking authentication status...")
            
            // First check if we have a current session
            val currentSession = SupabaseClient.AuthHelper.getCurrentSession()
            val currentUser = SupabaseClient.AuthHelper.getCurrentUser()
            
            Log.d(TAG, "Current session: ${currentSession != null}")
            Log.d(TAG, "Current user: ${currentUser != null}")
            
            if (currentUser != null && currentSession != null) {
                // User is authenticated and has a valid session
                Log.d(TAG, "User authenticated with valid session")
                
                _currentUser.value = UserInfo(
                    id = currentUser.id,
                    email = currentUser.email,
                    name = currentUser.userMetadata?.get("full_name")?.toString(),
                    photoUrl = null,
                    provider = "supabase"
                )
                _authState.value = AuthState.Authenticated
                _authLiveData.value = AuthState.Authenticated
                
            } else {
                // No valid session or user found
                Log.d(TAG, "No valid session or user found - setting unauthenticated")
                
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
                _authLiveData.value = AuthState.Unauthenticated
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking authentication status", e)
            
            // Only set error state if we can't determine the auth status at all
            // Otherwise, keep the current state to avoid unnecessary logouts
            if (_authState.value == AuthState.Loading) {
                _authState.value = AuthState.Error(e.message ?: "Error checking authentication status")
                _authLiveData.value = AuthState.Error(e.message ?: "Error checking authentication status")
            }
        }
    }
    
    /**
     * Refresh authentication status
     */
    fun refreshAuthStatus() {
        Log.d(TAG, "Refreshing authentication status...")
        checkAuthenticationStatus()
    }


    /**
     * Get current authentication state synchronously
     */
    fun getCurrentAuthState(): AuthState = _authState.value

    /**
     * Check if user is currently authenticated
     */
    fun isAuthenticated(): Boolean = _authState.value is AuthState.Authenticated
    
    /**
     * Set authenticated user (for external auth providers like Google)
     * 
     * @param email User's email address
     * @param name User's display name
     * @param provider Authentication provider (default: "google")
     */
    fun setAuthenticatedUser(email: String?, name: String?, provider: String = "google") {
        Log.d(TAG, "Setting authenticated user - Email: $email, Name: $name, Provider: $provider")
        
        viewModelScope.launch(exceptionHandler) {
            try {
                // Set user information
                _currentUser.value = UserInfo(
                    id = email ?: "unknown",
                    email = email,
                    name = name,
                    photoUrl = null,
                    provider = provider
                )
                
                // Update authentication state
                _authState.value = AuthState.Authenticated
                _authLiveData.value = AuthState.Authenticated
                
                Log.d(TAG, "User authentication state updated successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error setting authenticated user", e)
                _authState.value = AuthState.Error(e.message ?: "Error setting user authentication")
                _authLiveData.value = AuthState.Error(e.message ?: "Error setting user authentication")
            }
        }
    }
}
