package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.button.MaterialButton;
import com.example.madadgarapp.utils.AuthManager;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private EditText emailInput, passwordInput;
    private MaterialButton btnLogin;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        initAuthManager();
        setupClickListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void initAuthManager() {
        authManager = new ViewModelProvider(this).get(AuthManager.class);
        
        // Observe authentication state
        authManager.getAuthLiveData().observe(this, authState -> {
            Log.d(TAG, "Auth state changed: " + authState.getClass().getSimpleName());
            
            if (authState instanceof AuthManager.AuthState.Loading) {
                // Show loading state
                btnLogin.setEnabled(false);
                btnLogin.setText("Signing in...");
            } else if (authState instanceof AuthManager.AuthState.Authenticated) {
                // User is authenticated, navigate to main activity
                Log.d(TAG, "User authenticated successfully");
                navigateToMainActivity();
            } else if (authState instanceof AuthManager.AuthState.Unauthenticated) {
                // Reset UI to normal state
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
            } else if (authState instanceof AuthManager.AuthState.Error) {
                // Handle error state
                AuthManager.AuthState.Error errorState = (AuthManager.AuthState.Error) authState;
                String message = errorState.getMessage() != null ? errorState.getMessage() : "Login failed";
                Log.e(TAG, "Authentication error: " + message);

                // Reset UI
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                
                // Show error message
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginActivity.this, "Please enter email and password", Toast.LENGTH_SHORT).show();
                } else if (!isValidEmail(email)) {
                    Toast.makeText(LoginActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                } else {
                    // Call authentication method
                    authenticateUser(email, password);
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void authenticateUser(String email, String password) {
        Log.d(TAG, "Attempting to authenticate user: " + email);
        authManager.signInWithEmailPassword(email, password);
    }
    
    private void navigateToMainActivity() {
        Log.d(TAG, "Navigating to MainActivity...");
        
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
