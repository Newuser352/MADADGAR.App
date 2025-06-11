package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hbb20.CountryCodePicker;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilPhone, tilPassword;
    private TextInputEditText etPhone, etPassword;
    private MaterialButton btnLogin, btnSignup, btnForgotPassword;
    private CountryCodePicker countryCodePicker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize views
        initViews();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        
        countryCodePicker = findViewById(R.id.country_code_picker);
        // Initialize CountryCodePicker with default values
        if (countryCodePicker != null) {
            countryCodePicker.registerCarrierNumberEditText(etPhone);
            // Set default country code if needed
            countryCodePicker.setDefaultCountryUsingNameCode("PK");
        }
        
        btnLogin = findViewById(R.id.btn_login);
        btnSignup = findViewById(R.id.btn_signup);
        btnForgotPassword = findViewById(R.id.btn_forgot_password);
    }

    private void setupClickListeners() {
        // Login button click listener
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (validateInputs()) {
                        // Here you would normally perform authentication
                        // For now, just show a toast message
                        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();
                        
                        // Navigate to MainActivity
                        try {
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish(); // Close this activity
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(LoginActivity.this, "Error starting MainActivity: " + e.getMessage(), 
                                Toast.LENGTH_LONG).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(LoginActivity.this, "Login error: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
            }
        });

        // Sign Up button click listener
        btnSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to SignUpActivity
                Intent intent = new Intent(LoginActivity.this, SignUpActivity.class);
                startActivity(intent);
                // Don't finish this activity to allow back navigation
            }
        });

        // Forgot Password button click listener
        btnForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // For now, just show a toast message
                Toast.makeText(LoginActivity.this, "Forgot password functionality coming soon!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validateInputs() {
        try {
            boolean isValid = true;
            
            // Get input values
            String password = "";
            if (etPassword != null && etPassword.getText() != null) {
                password = etPassword.getText().toString().trim();
            }
            
            // Validate phone number using CountryCodePicker's validation
            if (countryCodePicker == null) {
                Toast.makeText(this, "Country code picker not initialized", Toast.LENGTH_SHORT).show();
                return false;
            }
            
            try {
                if (!countryCodePicker.isValidFullNumber()) {
                    if (tilPhone != null) {
                        tilPhone.setError("Please enter a valid phone number");
                    }
                    isValid = false;
                } else if (tilPhone != null) {
                    tilPhone.setError(null);
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (tilPhone != null) {
                    tilPhone.setError("Error validating phone number");
                }
                return false;
            }
            
            // Validate password
            if (password.isEmpty()) {
                if (tilPassword != null) {
                    tilPassword.setError("Password is required");
                }
                isValid = false;
            } else if (password.length() < 6) {
                if (tilPassword != null) {
                    tilPassword.setError("Password must be at least 6 characters");
                }
                isValid = false;
            } else if (tilPassword != null) {
                tilPassword.setError(null);
            }
            
            return isValid;
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Validation error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return false;
        }
    }
    
    /**
     * Get the full phone number with country code
     * @return formatted phone number with country code
     */
    private String getFullPhoneNumber() {
        return countryCodePicker.getFullNumberWithPlus();
    }

    // Clear input errors when text changes
    private void setupTextWatchers() {
        // This would be implemented with TextWatcher for clearing errors as user types
        // Not implementing now for brevity
    }
}

