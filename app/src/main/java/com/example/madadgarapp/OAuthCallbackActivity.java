package com.example.madadgarapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.madadgarapp.utils.SupabaseAuth;

/**
 * Activity to handle OAuth callback redirects from Google and other providers
 * This activity is launched when the OAuth flow redirects back to the app
 */
public class OAuthCallbackActivity extends AppCompatActivity {

    private SupabaseAuth authHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        authHelper = new SupabaseAuth(this);
        
        // Get the intent that started this activity
        Intent intent = getIntent();
        Uri data = intent.getData();
        
        if (data != null) {
            handleOAuthCallback(data);
        } else {
            showErrorAndFinish("Invalid OAuth callback");
        }
    }
    
    private void handleOAuthCallback(Uri data) {
        String scheme = data.getScheme();
        String host = data.getHost();
        String fullUrl = data.toString();
        
        Log.d("OAuthCallback", "Handling callback. Scheme: " + scheme + ", Host: " + host);
        Log.d("OAuthCallback", "Full URL: " + fullUrl);

        // Check for Supabase OTP/magic link
        if ("https".equals(scheme) && host != null && 
            (host.contains("supabase.co") || host.contains("supabase.in"))) {
            
            if (fullUrl.contains("/auth/v1/verify") || fullUrl.contains("/auth/v1/callback")) {
                handleSupabaseAuthCallback(fullUrl);
                return;
            }
        }
        
        // Check if this is our expected callback
        if ("com.example.madadgarapp".equals(scheme)) {
            if ("auth-callback".equals(host)) {
                handleGeneralAuthCallback(fullUrl);
            } else if ("magic-link".equals(host)) {
                handleMagicLinkCallback(fullUrl);
            } else {
                showErrorAndFinish("Unknown OAuth callback: " + host);
            }
        } else {
            showErrorAndFinish("Unsupported OAuth scheme: " + scheme);
        }
    }
    
    private void handleGeneralAuthCallback(String url) {
        // Handle general OAuth callback (for other providers)
        authHelper.handleOAuthCallback(url, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(OAuthCallbackActivity.this, 
                        "Authentication successful!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to MainActivity
                    Intent intent = new Intent(OAuthCallbackActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showErrorAndFinish("Authentication failed: " + error);
                });
            }
        });
    }
    
    private void handleMagicLinkCallback(String url) {
        // Handle magic link callback from Supabase
        authHelper.handleOAuthCallback(url, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(OAuthCallbackActivity.this, 
                        "Magic link authentication successful!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to MainActivity
                    Intent intent = new Intent(OAuthCallbackActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showErrorAndFinish("Magic link authentication failed: " + error);
                });
            }
        });
    }
    
    private void handleSupabaseAuthCallback(String url) {
        Log.d("OAuthCallback", "Handling Supabase auth callback");
        
        // Handle the Supabase auth callback
        authHelper.handleOAuthCallback(url, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                Log.d("OAuthCallback", "Supabase auth successful");
                runOnUiThread(() -> {
                    Toast.makeText(OAuthCallbackActivity.this, 
                        "Authentication successful!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to MainActivity
                    Intent intent = new Intent(OAuthCallbackActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                Log.e("OAuthCallback", "Supabase auth error: " + error);
                showErrorAndFinish("Authentication failed: " + error);
            }
        });
    }
    
    private void showErrorAndFinish(String errorMessage) {
        Log.e("OAuthCallback", errorMessage);
        runOnUiThread(() -> {
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            
            // Navigate back to AuthSelectionActivity
            Intent intent = new Intent(this, AuthSelectionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }
}
