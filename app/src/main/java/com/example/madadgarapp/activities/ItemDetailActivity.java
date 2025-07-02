package com.example.madadgarapp.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.madadgarapp.R;
import com.example.madadgarapp.models.Item;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ItemDetailActivity extends AppCompatActivity {

    public static final String EXTRA_ITEM = "extra_item";

    private ImageView imageItemDetail;
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
        imageItemDetail = findViewById(R.id.image_item_detail);
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
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(currentItem.getTitle());
        }
    }

    private void populateItemDetails() {
        // Load item image with Glide
        if (currentItem.getImageUrl() != null && !currentItem.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentItem.getImageUrl())
                    .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .transform(new RoundedCorners(16)))
                    .into(imageItemDetail);
        } else {
            imageItemDetail.setImageResource(R.drawable.placeholder_image);
        }

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

        // Set owner information
        textOwnerName.setText(currentItem.getOwner());
        
        // For demo purposes, we'll show placeholder contact info
        // In a real app, this would come from the user profile
        textOwnerEmail.setText(currentItem.getOwner().toLowerCase().replace(" ", ".") + "@example.com");
        textOwnerPhone.setText(currentItem.getContactNumber());
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
}

