package com.example.madadgarapp.activities;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.example.madadgarapp.R;

/**
 * Activity to display images in full-screen mode
 */
public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URL = "extra_image_url";
    public static final String EXTRA_IMAGE_TITLE = "extra_image_title";

    private ImageView imageView;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Make activity full-screen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        
        setContentView(R.layout.activity_full_screen_image);

        initializeViews();
        loadImage();
    }

    private void initializeViews() {
        imageView = findViewById(R.id.full_screen_image);
        progressBar = findViewById(R.id.progress_bar);

        // Set click listener to close activity
        imageView.setOnClickListener(v -> finish());
    }

    private void loadImage() {
        String imageUrl = getIntent().getStringExtra(EXTRA_IMAGE_URL);
        String imageTitle = getIntent().getStringExtra(EXTRA_IMAGE_TITLE);

        if (imageUrl == null || imageUrl.isEmpty()) {
            Toast.makeText(this, "Image URL not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Set title if provided
        if (imageTitle != null && !imageTitle.isEmpty() && getSupportActionBar() != null) {
            getSupportActionBar().setTitle(imageTitle);
        }

        // Show progress bar
        progressBar.setVisibility(View.VISIBLE);

        // Load image with Glide
        Glide.with(this)
                .load(imageUrl)
                .apply(new RequestOptions()
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.placeholder_image)
                        .error(R.drawable.placeholder_image))
                .into(imageView);

        // Hide progress bar after loading
        progressBar.setVisibility(View.GONE);
    }
}
