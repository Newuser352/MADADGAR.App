package com.example.madadgarapp.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.madadgarapp.R;
import com.example.madadgarapp.adapters.MediaAdapter;
import com.example.madadgarapp.models.Item;
import com.example.madadgarapp.utils.SupabaseClient;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM = "extra_item";

    private ViewPager2 mediaViewPager;
    private LinearLayout mediaIndicators;
    private ImageView videoPlayOverlay;
    private TextView textItemTitleDetail;
    private TextView textItemCategoryDetail;
    private TextView textItemLocationDetail;
    private TextView textItemDateDetail;
    private TextView textItemDescriptionDetail;
    private TextView textOwnerName;
    private TextView textOwnerEmail;
    private TextView textOwnerPhone;
    private Button btnContactOwner;
    private Button btnShareItem;

    private Item currentItem;
    private MediaAdapter mediaAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_item_detail);

        // Get item from intent
        currentItem = (Item) getIntent().getSerializableExtra(EXTRA_ITEM);
        if (currentItem == null) {
            Toast.makeText(this, "Error loading item details", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initializeViews();
        setupToolbar();
        populateItemDetails();
        setupClickListeners();
    }

    private void initializeViews() {
        mediaViewPager = findViewById(R.id.media_viewpager);
        mediaIndicators = findViewById(R.id.media_indicators);
        videoPlayOverlay = findViewById(R.id.video_play_overlay);
        textItemTitleDetail = findViewById(R.id.text_item_title_detail);
        textItemCategoryDetail = findViewById(R.id.text_item_category_detail);
        textItemLocationDetail = findViewById(R.id.text_item_location_detail);
        textItemDateDetail = findViewById(R.id.text_item_date_detail);
        textItemDescriptionDetail = findViewById(R.id.text_item_description_detail);
        textOwnerName = findViewById(R.id.text_owner_name);
        textOwnerEmail = findViewById(R.id.text_owner_email);
        textOwnerPhone = findViewById(R.id.text_owner_phone);
        btnContactOwner = findViewById(R.id.btn_contact_owner);
        btnShareItem = findViewById(R.id.btn_share_item);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setTitle(currentItem.getTitle());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Disable menu to remove the three dots on top
        return false;
    }

    private void populateItemDetails() {
        // Setup media gallery
        setupMediaGallery();
        
        // Set item details
        textItemTitleDetail.setText(currentItem.getTitle());
        
        // Format categories
        String category = currentItem.getCategory();
        if (category != null && !category.isEmpty()) {
            textItemCategoryDetail.setText(category);
        } else {
            textItemCategoryDetail.setText("Uncategorized");
        }

        textItemLocationDetail.setText(currentItem.getLocation());
        textItemDescriptionDetail.setText(currentItem.getDescription());

        // Format date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        String formattedDate = "Posted on " + dateFormat.format(new Date(currentItem.getCreatedAt()));
        textItemDateDetail.setText(formattedDate);

        // Set owner information - check if this is the current user's item
        String currentUserId = getCurrentUserId();
        String ownerName = "Item Owner";
        String ownerEmail = "Contact via app";
        String ownerPhone = currentItem.getContactNumber();
        
        if (currentUserId != null && currentUserId.equals(currentItem.getOwnerId())) {
            // This is the current user's item - show their info
            String userEmail = getCurrentUserEmail();
            String userName = getCurrentUserName();
            
            ownerName = userName != null ? userName : "You";
            ownerEmail = userEmail != null ? userEmail : "Your email";
        } else {
            // This is someone else's item - show limited contact info
            ownerName = "Contact Owner";
            ownerEmail = "Contact via app messaging";
        }
        
        textOwnerName.setText(ownerName);
        textOwnerEmail.setText(ownerEmail);
        textOwnerPhone.setText(ownerPhone != null && !ownerPhone.isEmpty() ? ownerPhone : "Contact via app");
    }

    private void setupMediaGallery() {
        List<MediaAdapter.MediaItem> mediaItems = new ArrayList<>();
        
        android.util.Log.d("ItemDetailActivity", "Setting up media gallery for item: " + currentItem.getTitle());
        
        // Add images from imageUrls list (if available)
        if (currentItem.getImageUrls() != null && !currentItem.getImageUrls().isEmpty()) {
            android.util.Log.d("ItemDetailActivity", "Found " + currentItem.getImageUrls().size() + " image URLs");
            for (String imageUrl : currentItem.getImageUrls()) {
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    android.util.Log.d("ItemDetailActivity", "Adding image: " + imageUrl);
                    mediaItems.add(new MediaAdapter.MediaItem(imageUrl, false));
                }
            }
        } else if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            // Fallback to single image URL for backward compatibility
            android.util.Log.d("ItemDetailActivity", "Using fallback single image URL: " + currentItem.getImageUrl());
            mediaItems.add(new MediaAdapter.MediaItem(currentItem.getImageUrl(), false));
        } else {
            android.util.Log.w("ItemDetailActivity", "No image URLs found");
        }
        
        // Add video if available
        if (currentItem.getVideoUrl() != null && !currentItem.getVideoUrl().isEmpty()) {
            android.util.Log.d("ItemDetailActivity", "Adding video: " + currentItem.getVideoUrl());
            mediaItems.add(new MediaAdapter.MediaItem(currentItem.getVideoUrl(), true));
        } else {
            android.util.Log.w("ItemDetailActivity", "No video URL found");
        }
        
        // If no media available, show placeholder
        if (mediaItems.isEmpty()) {
            android.util.Log.w("ItemDetailActivity", "No media found, showing placeholder");
            mediaItems.add(new MediaAdapter.MediaItem("", false)); // Empty URL will show placeholder
        }
        
        android.util.Log.d("ItemDetailActivity", "Total media items: " + mediaItems.size());
        
        // Setup adapter
        mediaAdapter = new MediaAdapter(this, mediaItems);
        mediaViewPager.setAdapter(mediaAdapter);
        
        // Setup indicators if more than one media item
        if (mediaItems.size() > 1) {
            setupMediaIndicators(mediaItems.size());
        } else {
            mediaIndicators.setVisibility(View.GONE);
        }
    }
    
    private void setupMediaIndicators(int count) {
        mediaIndicators.removeAllViews();
        mediaIndicators.setVisibility(View.VISIBLE);
        
        for (int i = 0; i < count; i++) {
            ImageView indicator = new ImageView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.indicator_size),
                getResources().getDimensionPixelSize(R.dimen.indicator_size)
            );
            params.setMargins(4, 0, 4, 0);
            indicator.setLayoutParams(params);
            indicator.setImageResource(i == 0 ? R.drawable.indicator_selected : R.drawable.indicator_unselected);
            mediaIndicators.addView(indicator);
        }
        
        // Update indicators when page changes
        mediaViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
            }
        });
    }
    
    private void updateIndicators(int selectedPosition) {
        for (int i = 0; i < mediaIndicators.getChildCount(); i++) {
            ImageView indicator = (ImageView) mediaIndicators.getChildAt(i);
            indicator.setImageResource(i == selectedPosition ? R.drawable.indicator_selected : R.drawable.indicator_unselected);
        }
    }

    private void setupClickListeners() {
        btnContactOwner.setOnClickListener(v -> contactOwner());
        btnShareItem.setOnClickListener(v -> shareItem());
    }

    private void contactOwner() {
        // Create intent to send email or message
        String[] options = {"Send Email", "Call Phone", "Send SMS"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Contact " + currentItem.getOwner())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Send Email
                            sendEmail();
                            break;
                        case 1: // Call Phone
                            callPhone();
                            break;
                        case 2: // Send SMS
                            sendSMS();
                            break;
                    }
                })
                .show();
    }

    private void sendEmail() {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
        emailIntent.setData(Uri.parse("mailto:" + textOwnerEmail.getText().toString()));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Inquiry about: " + currentItem.getTitle());
        emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi " + currentItem.getOwner() + ",\n\n" +
                "I'm interested in your item: " + currentItem.getTitle() + "\n\n" +
                "Please let me know if it's still available.\n\n" +
                "Thanks!");
        
        try {
            startActivity(Intent.createChooser(emailIntent, "Send Email"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No email app found", Toast.LENGTH_SHORT).show();
        }
    }

    private void callPhone() {
        if (currentItem.getContactNumber() != null && !currentItem.getContactNumber().isEmpty()) {
            Intent callIntent = new Intent(Intent.ACTION_DIAL);
            callIntent.setData(Uri.parse("tel:" + currentItem.getContactNumber()));
            try {
                startActivity(callIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No phone app found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendSMS() {
        if (currentItem.getContactNumber() != null && !currentItem.getContactNumber().isEmpty()) {
            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
            smsIntent.setData(Uri.parse("smsto:" + currentItem.getContactNumber()));
            smsIntent.putExtra("sms_body", "Hi " + currentItem.getOwner() + ", I'm interested in your item: " + currentItem.getTitle());
            
            try {
                startActivity(smsIntent);
            } catch (android.content.ActivityNotFoundException ex) {
                Toast.makeText(this, "No SMS app found", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "No phone number available", Toast.LENGTH_SHORT).show();
        }
    }

    private void shareItem() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        
        String shareText = "Check out this item: " + currentItem.getTitle() + "\n" +
                "Description: " + currentItem.getDescription() + "\n" +
                "Location: " + currentItem.getLocation() + "\n" +
                "Contact: " + currentItem.getOwner();
        
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            shareText += "\nImage: " + currentItem.getImageUrl();
        }
        
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared Item: " + currentItem.getTitle());
        
        try {
            startActivity(Intent.createChooser(shareIntent, "Share Item"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "No app available to share", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Get current user ID from Supabase
     */
    private String getCurrentUserId() {
        try {
            var currentUser = SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
            return currentUser != null ? currentUser.getId() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get current user email from Supabase
     */
    private String getCurrentUserEmail() {
        try {
            var currentUser = SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
            return currentUser != null ? currentUser.getEmail() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Get current user name from Supabase metadata
     */
    private String getCurrentUserName() {
        try {
            var currentUser = SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
            if (currentUser != null && currentUser.getUserMetadata() != null) {
                Object fullName = currentUser.getUserMetadata().get("full_name");
                if (fullName != null) {
                    return fullName.toString();
                }
                // Fallback to email username if no full name
                String email = currentUser.getEmail();
                if (email != null) {
                    return email.split("@")[0]; // Use part before @ as name
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}

