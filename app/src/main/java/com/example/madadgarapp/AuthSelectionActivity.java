package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.example.madadgarapp.utils.SupabaseClient;
import com.example.madadgarapp.utils.AuthManager;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class AuthSelectionActivity extends AppCompatActivity {

    private static final String TAG = "AuthSelectionActivity";
    private static final String WEB_CLIENT_ID = "715506916742-n3esait80m4fbf6urqqls5fv8qbaeoff.apps.googleusercontent.com";
    
    private MaterialButton btnEmailLogin;
    private MaterialButton btnGoogleSignIn;
    private AuthManager authManager;
    
    // Google Sign-In
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private ActivityResultLauncher<Intent> googleSignInLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply saved theme (dark / light) before inflating layout
        com.example.madadgarapp.utils.ThemeUtils.applyTheme(this);
        setContentView(R.layout.activity_auth_selection);

        // Initialize views
        initViews();
        
        // Initialize Google Sign-In
        initGoogleSignIn();
        
        // Initialize modern AuthManager
        initAuthManager();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        btnEmailLogin = findViewById(R.id.btn_email_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_signin);
    }
    
    /**
     * Initialize Google Sign-In configuration
     */
    private void initGoogleSignIn() {
        // Configure Google Sign-In options
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(WEB_CLIENT_ID)
                .requestEmail()
                .build();

        // Initialize Google Sign-In client
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        
        // Initialize Firebase Auth
        firebaseAuth = FirebaseAuth.getInstance();
        
        // Initialize activity result launcher for Google Sign-In
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                        handleGoogleSignInResult(task);
                    } else {
                        Log.w(TAG, "Google Sign-In canceled or failed");
                        Toast.makeText(this, "Google Sign-In canceled", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }
    
    /**
     * Initialize modern AuthManager with lifecycle-aware state observation
     */
    private void initAuthManager() {
        Log.d("AuthSelection", "Initializing modern AuthManager...");
        
        try {
            // Initialize AuthManager using ViewModelProvider with Hilt
            authManager = new ViewModelProvider(this)
                    .get(AuthManager.class);
            
            // Observe authentication state changes
            authManager.getAuthLiveData().observe(this, authState -> {
                Log.d("AuthSelection", "Auth state changed: " + authState.getClass().getSimpleName());
                
                if (authState instanceof AuthManager.AuthState.Loading) {
                    // Show loading state
                    btnEmailLogin.setEnabled(false);
                    btnGoogleSignIn.setEnabled(false);
                } else if (authState instanceof AuthManager.AuthState.Authenticated) {
                    // User is authenticated, navigate to main activity
                    Log.d("AuthSelection", "User authenticated successfully via AuthManager");
                    navigateToMainActivity();
                } else if (authState instanceof AuthManager.AuthState.Unauthenticated) {
                    // Reset UI to normal state
                    btnEmailLogin.setEnabled(true);
                    resetGoogleSignInButton();
                } else if (authState instanceof AuthManager.AuthState.Error) {
                    // Handle error state
                    AuthManager.AuthState.Error errorState = (AuthManager.AuthState.Error) authState;
                    String rawMessage = errorState.getMessage() != null ? errorState.getMessage() : "Unknown error";
                    Log.e("AuthSelection", "AuthManager error: " + rawMessage);

                    // Reset UI
                    btnEmailLogin.setEnabled(true);

                    // Create a more helpful error message
                    String displayMessage = "Authentication error: " + rawMessage;
                    
                    // Show error message
                    Toast.makeText(this, displayMessage, Toast.LENGTH_LONG).show();
                }
            });
            
            Log.d("AuthSelection", "AuthManager initialized successfully");
            
        } catch (Exception e) {
            Log.e("AuthSelection", "Error initializing AuthManager", e);
            Toast.makeText(this, "Error initializing authentication system", Toast.LENGTH_SHORT).show();
        }
    }
    

    private void setupClickListeners() {
        // Email Login button click listener
        btnEmailLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to EmailAuthSelectionActivity for login/signup selection
                Intent intent = new Intent(AuthSelectionActivity.this, EmailAuthSelectionActivity.class);
                startActivity(intent);
            }
        });
        
        // Google Sign-In button click listener
        btnGoogleSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateGoogleSignIn();
            }
        });
    }
    
    /**
     * Initiate Google Sign-In process
     */
    private void initiateGoogleSignIn() {
        Log.d(TAG, "Initiating Google Sign-In...");
        
        // Disable button to prevent multiple clicks
        btnGoogleSignIn.setEnabled(false);
        btnGoogleSignIn.setText("Signing in...");
        
        // Sign out from previous session to ensure account picker is shown
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // Start Google Sign-In intent
            Intent signInIntent = googleSignInClient.getSignInIntent();
            googleSignInLauncher.launch(signInIntent);
        });
    }
    
    /**
     * Handle Google Sign-In result
     */
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(TAG, "Google Sign-In successful for: " + account.getEmail());
            Log.d(TAG, "ID Token available: " + (account.getIdToken() != null));
            
            if (account.getIdToken() == null) {
                Log.e(TAG, "ID Token is null - Web Client ID might be incorrect");
                resetGoogleSignInButton();
                Toast.makeText(this, "Authentication configuration error. Please contact support.", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Authenticate with Firebase
            firebaseAuthWithGoogle(account.getIdToken());
            
        } catch (ApiException e) {
            Log.w(TAG, "Google Sign-In failed with code: " + e.getStatusCode(), e);
            Log.w(TAG, "Error message: " + e.getMessage());
            
            // Reset button state
            resetGoogleSignInButton();
            
            // Show user-friendly error message
            String errorMessage = getGoogleSignInErrorMessage(e.getStatusCode());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * Authenticate with Supabase using Google credentials
     */
    private void firebaseAuthWithGoogle(String idToken) {
        Log.d(TAG, "Authenticating with Supabase using Google...");
        
        // First authenticate with Firebase for validation
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase authentication successful");
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                        
                        if (firebaseUser != null) {
                            Log.d(TAG, "Firebase User - Name: " + firebaseUser.getDisplayName() + ", Email: " + firebaseUser.getEmail());
                            
                            // Now authenticate with Supabase using the ID token
                            authenticateWithSupabase(idToken, firebaseUser);
                        }
                    } else {
                        Log.w(TAG, "Firebase authentication failed", task.getException());
                        
                        // Reset button state
                        resetGoogleSignInButton();
                        
                        Toast.makeText(this, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    /**
     * Authenticate with Supabase using Google ID token
     */
    private void authenticateWithSupabase(String idToken, FirebaseUser firebaseUser) {
        Log.d(TAG, "=== STARTING SUPABASE AUTHENTICATION ===");
        Log.d(TAG, "Firebase User Email: " + firebaseUser.getEmail());
        Log.d(TAG, "Firebase User Name: " + firebaseUser.getDisplayName());
        Log.d(TAG, "AuthManager is null: " + (authManager == null));
        
        // Now authenticate with Supabase using the Firebase ID token
        Log.d(TAG, "Authenticating with Supabase using ID token...");
        
        // Use the Java-friendly callback method
        SupabaseClient.AuthHelper.signInWithGoogleCallback(idToken, new SupabaseClient.AuthHelper.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d(TAG, "Supabase authentication successful: " + message);
                
                // Update AuthManager with the authenticated user
                if (authManager != null) {
                    Log.d(TAG, "Updating AuthManager with user info...");
                    authManager.setAuthenticatedUser(
                        firebaseUser.getEmail(), 
                        firebaseUser.getDisplayName(), 
                        "google"
                    );
                    Log.d(TAG, "AuthManager.setAuthenticatedUser() called successfully");
                } else {
                    Log.e(TAG, "ERROR: AuthManager is null! Cannot update authentication state");
                    Toast.makeText(AuthSelectionActivity.this, "Authentication manager error", Toast.LENGTH_LONG).show();
                    resetGoogleSignInButton();
                    return;
                }
                
                // Navigate to main activity
                Log.d(TAG, "About to navigate to MainActivity...");
                navigateToMainActivity();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Supabase authentication failed: " + error);
                
                // Handle Supabase authentication failure
                Toast.makeText(AuthSelectionActivity.this, "Backend authentication failed: " + error, Toast.LENGTH_LONG).show();
                resetGoogleSignInButton();
            }
        });
    }
    
    /**
     * Reset Google Sign-In button to original state
     */
    private void resetGoogleSignInButton() {
        btnGoogleSignIn.setEnabled(true);
        btnGoogleSignIn.setText("Continue with Google");
    }
    
    /**
     * Get user-friendly error message for Google Sign-In errors
     */
    private String getGoogleSignInErrorMessage(int statusCode) {
        switch (statusCode) {
            case 12501: // SIGN_IN_CANCELLED
                return "Sign-in was cancelled";
            case 12502: // SIGN_IN_FAILED
                return "Sign-in failed. Please try again";
            case 12500: // SIGN_IN_REQUIRED
                return "Please sign in to continue";
            case 7: // NETWORK_ERROR
                return "Network error. Please check your connection";
            default:
                return "Google Sign-In failed. Please try again";
        }
    }

    
    
    
    
    /**
     * Navigate to MainActivity after successful authentication
     */
    private void navigateToMainActivity() {
        Log.d("AuthSelection", "Navigating to MainActivity...");
        
        Intent intent = new Intent(AuthSelectionActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
