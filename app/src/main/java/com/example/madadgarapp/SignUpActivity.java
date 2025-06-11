package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.regex.Pattern;

public class SignUpActivity extends AppCompatActivity {

    private TextInputLayout tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etPhone, etPassword, etConfirmPassword;
    private MaterialButton btnCreateAccount, btnLogin;

    // Validation patterns
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10,12}$");
    private static final int MIN_PASSWORD_LENGTH = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize views
        initViews();
        
        // Set up click listeners
        setupClickListeners();
    }

    private void initViews() {
        // TextInputLayouts
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);
        
        // EditTexts
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        
        // Buttons
        btnCreateAccount = findViewById(R.id.btn_create_account);
        btnLogin = findViewById(R.id.btn_login);
    }

    private void setupClickListeners() {
        // Create Account button click listener
        btnCreateAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (validateInputs()) {
                    // Here you would normally perform user registration
                    // For now, just show a toast message
                    Toast.makeText(SignUpActivity.this, "Account created successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate back to LoginActivity
                    Intent intent = new Intent(SignUpActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish(); // Close this activity
                }
            }
        });

        // Login button click listener (already have an account)
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to LoginActivity
                // Since we might have come from LoginActivity, just finish this activity
                finish();
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;
        
        // Validate Phone
        if (!validatePhone()) {
            isValid = false;
        }
        
        // Validate Password
        if (!validatePassword()) {
            isValid = false;
        }
        
        // Validate Confirm Password
        if (!validateConfirmPassword()) {
            isValid = false;
        }
        
        return isValid;
    }
    

    
    private boolean validatePhone() {
        String phone = etPhone.getText().toString().trim();
        
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Phone number is required");
            return false;
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            tilPhone.setError("Please enter a valid phone number (10-12 digits)");
            return false;
        } else {
            tilPhone.setError(null);
            return true;
        }
    }
    
    private boolean validatePassword() {
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return false;
        } else if (password.length() < MIN_PASSWORD_LENGTH) {
            tilPassword.setError("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
            return false;
        } else {
            tilPassword.setError(null);
            return true;
        }
    }
    
    private boolean validateConfirmPassword() {
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError("Please confirm your password");
            return false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Passwords do not match");
            return false;
        } else {
            tilConfirmPassword.setError(null);
            return true;
        }
    }
}

