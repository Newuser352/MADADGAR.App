package com.example.madadgarapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.madadgarapp.utils.SupabaseAuth;

public class WebViewOAuthActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private SupabaseAuth authHelper;
    private String provider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_webview_oauth);

        // Initialize views
        initViews();
        
        // Initialize auth helper
        authHelper = new SupabaseAuth();
        
        // Get intent data
        Intent intent = getIntent();
        String oauthUrl = intent.getStringExtra("oauth_url");
        provider = intent.getStringExtra("provider");
        
        if (oauthUrl != null) {
            setupWebView();
            webView.loadUrl(oauthUrl);
        } else {
            Toast.makeText(this, "Invalid OAuth URL", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Sign in with " + (provider != null ? provider : "OAuth"));
        }
        
        webView = findViewById(R.id.webview);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                
                // Check if this is a callback URL
                if (url.contains("com.example.madadgarapp")) {
                    handleOAuthCallback(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // Check if this is a callback URL
                if (url.contains("com.example.madadgarapp")) {
                    handleOAuthCallback(url);
                    return true;
                }
                return false;
            }
        });
    }

    private void handleOAuthCallback(String url) {
        progressBar.setVisibility(View.VISIBLE);
        
        authHelper.handleOAuthCallback(url, new SupabaseAuth.AuthCallback() {
            @Override
            public void onSuccess(String message) {
                runOnUiThread(() -> {
                    Toast.makeText(WebViewOAuthActivity.this, 
                        provider + " login successful!", Toast.LENGTH_SHORT).show();
                    
                    // Navigate to MainActivity
                    Intent intent = new Intent(WebViewOAuthActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
            }
            
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(WebViewOAuthActivity.this, 
                        provider + " login failed: " + error, Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
