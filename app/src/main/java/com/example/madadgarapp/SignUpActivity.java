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
public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";
    private EditText emailInput;
    private EditText passwordInput;
    private MaterialButton btnSignup;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        initViews();
        initAuthManager();
        setupClickListeners();
    }

    private void initViews() {
        emailInput = findViewById(R.id.input_email);
        passwordInput = findViewById(R.id.input_password);
        btnSignup = findViewById(R.id.btn_signup);
    }

    private void initAuthManager() {
        authManager = new ViewModelProvider(this).get(AuthManager.class);
        
        // Observe authentication state
        authManager.getAuthLiveData().observe(this, authState -> {
            Log.d(TAG, "Auth state changed: " + authState.getClass().getSimpleName());
            
            if (authState instanceof AuthManager.AuthState.Loading) {
                // Show loading state
                btnSignup.setEnabled(false);
                btnSignup.setText("Sending email...");
            } else if (authState instanceof AuthManager.AuthState.Unauthenticated) {
                // Reset UI to normal state - this is expected after signup
                btnSignup.setEnabled(true);
                btnSignup.setText("Sign Up");
            } else if (authState instanceof AuthManager.AuthState.Authenticated) {
                // Sign up finished successfully
                btnSignup.setEnabled(true);
                btnSignup.setText("Sign Up");

                Toast.makeText(this, "Account created successfully! Please check your email for verification.", Toast.LENGTH_LONG).show();

                // Navigate to login screen after showing the toast so that the coroutine is not cancelled prematurely
                emailInput.postDelayed(() -> {
                    Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }, 2000);

            } else if (authState instanceof AuthManager.AuthState.Error) {
                // Handle error state
                AuthManager.AuthState.Error errorState = (AuthManager.AuthState.Error) authState;
                String message = errorState.getMessage() != null ? errorState.getMessage() : "Signup failed";
                Log.e(TAG, "Signup error: " + message);

                // Reset UI
                btnSignup.setEnabled(true);
                btnSignup.setText("Sign Up");
                
                // Show error message
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();

                if (email.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter your email address", Toast.LENGTH_SHORT).show();
                } else if (!isValidEmail(email)) {
                    Toast.makeText(SignupActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
                } else if (password.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Please enter a password", Toast.LENGTH_SHORT).show();
                } else if (password.length() < 6) {
                    Toast.makeText(SignupActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                } else {
                    // Call signup method with email and password
                    signUpWithEmailPassword(email, password);
                }
            }
        });
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void signUpWithEmailPassword(String email, String password) {
        Log.d(TAG, "Attempting to sign up with email: " + email);
        authManager.signUpWithEmailPassword(email, password);
        

    }
}
