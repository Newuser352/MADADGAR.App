package com.example.madadgarapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.LinearLayout;
import android.widget.VideoView;
import android.widget.TextView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import android.widget.ImageView;
import android.widget.FrameLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import android.media.MediaPlayer;
import android.view.ViewGroup.LayoutParams;
import com.google.android.material.slider.Slider;
import com.example.madadgarapp.repository.SupabaseItemBridge;
import com.example.madadgarapp.models.SupabaseItem;
import com.example.madadgarapp.utils.LocationUtils;
import com.example.madadgarapp.utils.NotificationManager;
import com.example.madadgarapp.fragments.LocationPickerFragment;
import android.Manifest;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;



import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.hbb20.CountryCodePicker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ShareItemFragment extends Fragment {

    private static final String TAG = "ShareItemFragment";
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_MULTIPLE_IMAGES_REQUEST = 2;
    private static final int PICK_VIDEO_REQUEST = 3;
    private static final int MAX_PHOTO_COUNT = 6;

    // Views
    private ImageView imagePreview;
    private ImageView iconPlayVideo;
    private VideoView videoPreview;
    private FrameLayout layoutMainMediaPreview;
    private LinearLayout layoutImagePreviews;
    private TextInputLayout tilItemName, tilItemDescription, tilItemLocation, tilContactNumber;
    private TextInputLayout tilContact1, tilContact2, tilItemSubcategory; // Primary and secondary contact fields
    private TextInputEditText etItemName, etItemDescription, etItemLocation, etContactNumber;
    private TextInputEditText etContact1, etContact2; // Primary and secondary contact EditTexts
    private MaterialAutoCompleteTextView dropdownSubcategory;
    private MaterialButton btnShareItem, btnUploadPhoto, btnUploadVideo, btnClearPhotos, btnClearVideo;
    private CountryCodePicker countryCodePicker;
    private RadioGroup radioGroupCategory;
    private RadioButton radioFood, radioNonFood;
    private LinearLayout layoutExpirySection;
    private Slider sliderExpiry;
    private TextView textExpiryValue;
    private TextView textMediaCount;
    private TextView textVideoStatus;
    private RadioGroup radioGroupTimeUnit;
    private RadioButton radioHours, radioDays;
    
    // Categories arrays
    private String[] foodSubcategories = {"Cooked Food", "Uncooked Food"};
    private String[] nonFoodSubcategories = {"Electronics", "Furniture", "Books", "Clothing", "Other"};

    // State
    private ArrayList<Uri> selectedPhotoUris = new ArrayList<>();
    private Uri selectedVideoUri;
    private boolean isVideoSelected = false;
    private int expiryDays = 3; // Default expiry days
    private int expiryHours = 6; // Default expiry hours
    private boolean isHoursSelected = false; // Track current time unit
    
    // Location-related variables
    private LocationUtils.Coordinates selectedCoordinates;
    private MaterialButton btnSelectLocation;
    
    // Location permission launcher
    private ActivityResultLauncher<String[]> locationPermissionLauncher;

    public ShareItemFragment() {
        // Required empty public constructor
    }

    public static ShareItemFragment newInstance() {
        return new ShareItemFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_share_item, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "onViewCreated: Starting fragment initialization");
        
        try {
            // Initialize location permission launcher
            initLocationPermissionLauncher();
            
            // Initialize views
            initViews(view);
            
            // Set up toolbar
            setupToolbar();
            
            // Set up listeners
            setupListeners();
            
            // Set up subcategory dropdown for default Non-Food category
            setupSubcategoryDropdown(false);
            
            // Set up country code picker
            // Country code picker removed
            
            Log.d(TAG, "onViewCreated: Fragment successfully initialized");
        } catch (Exception e) {
            Log.e(TAG, "onViewCreated: Error initializing fragment", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error initializing share form", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume: Fragment resumed");
    }
    
    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: Fragment paused");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: Cleaning up resources");
        // Remove listeners to prevent memory leaks
        if (countryCodePicker != null) {
            countryCodePicker.setPhoneNumberValidityChangeListener(null);
        }
        super.onDestroy();
    }
    
    private void setupToolbar() {
        // Enable the back button in the action bar
        if (getActivity() != null && getActivity() instanceof AppCompatActivity) {
            ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
            }
        }
        
        // Handle back button click
        setHasOptionsMenu(true);
    }
    
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Clear the menu to prevent showing any menu items from the activity
        menu.clear();
        super.onCreateOptionsMenu(menu, inflater);
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // Handle back button click in the action bar
        if (item.getItemId() == android.R.id.home) {
            closeShareForm();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void closeShareForm() {
        Log.d(TAG, "closeShareForm: Closing share form");
        try {
            // Check if fragment is attached before proceeding
            if (!isAdded()) {
                Log.w(TAG, "closeShareForm: Fragment not attached, aborting");
                return;
            }
            
            // Dismiss the keyboard if it's open
            if (getActivity() != null) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                View view = getActivity().getCurrentFocus();
                if (view != null) {
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }
            }
            
            // Hide the share form by calling the activity's method
            if (getActivity() instanceof MainActivity) {
                Log.d(TAG, "closeShareForm: Calling MainActivity.hideShareItemForm()");
                ((MainActivity) getActivity()).hideShareItemForm();
            } else {
                Log.e(TAG, "closeShareForm: Activity is not MainActivity");
            }
        } catch (Exception e) {
            Log.e(TAG, "closeShareForm: Error closing share form", e);
        }
    }

    private void initViews(View view) {
        try {
            Log.d(TAG, "initViews: Initializing views");
            
            // Media views
            imagePreview = view.findViewById(R.id.image_preview);
            videoPreview = view.findViewById(R.id.video_preview);
            iconPlayVideo = view.findViewById(R.id.icon_play_video);
            layoutMainMediaPreview = view.findViewById(R.id.layout_main_media_preview);
            layoutImagePreviews = view.findViewById(R.id.layout_image_previews);
            textMediaCount = view.findViewById(R.id.text_media_count);
            textVideoStatus = view.findViewById(R.id.text_video_status);
            
            // Category selection
            radioGroupCategory = view.findViewById(R.id.radio_group_category);
            radioFood = view.findViewById(R.id.radio_food);
            radioNonFood = view.findViewById(R.id.radio_non_food);
            
            // Expiry timer
            layoutExpirySection = view.findViewById(R.id.layout_expiry_section);
            sliderExpiry = view.findViewById(R.id.slider_expiry);
            textExpiryValue = view.findViewById(R.id.text_expiry_value);
            
            // Time unit selection
            radioGroupTimeUnit = view.findViewById(R.id.radio_group_time_unit);
            radioHours = view.findViewById(R.id.radio_hours);
            radioDays = view.findViewById(R.id.radio_days);
            
            // TextInputLayouts
            tilItemName = view.findViewById(R.id.til_item_name);
            tilItemDescription = view.findViewById(R.id.til_item_description);
            tilItemSubcategory = view.findViewById(R.id.til_item_subcategory);
            tilItemLocation = view.findViewById(R.id.til_item_location);
            tilContactNumber = view.findViewById(R.id.til_contact_number);
            tilContact1 = view.findViewById(R.id.til_contact1);
            tilContact2 = view.findViewById(R.id.til_contact2);
            
            // EditTexts
            etItemName = view.findViewById(R.id.et_item_name);
            etItemDescription = view.findViewById(R.id.et_item_description);
            etItemLocation = view.findViewById(R.id.et_item_location);
            etContactNumber = view.findViewById(R.id.et_contact_number);
            etContact1 = view.findViewById(R.id.et_contact1);
            etContact2 = view.findViewById(R.id.et_contact2);
            
            // Initialize the dropdown subcategory
            dropdownSubcategory = view.findViewById(R.id.dropdown_subcategory);
            
            // Country Code Picker
            countryCodePicker = view.findViewById(R.id.country_code_picker);
            if (countryCodePicker == null) {
                Log.e(TAG, "initViews: CountryCodePicker not found in layout");
            } else {
                Log.d(TAG, "initViews: CountryCodePicker successfully initialized");
            }
            
            // Buttons
            btnShareItem = view.findViewById(R.id.btn_share_item);
            btnUploadPhoto = view.findViewById(R.id.btn_upload_photo);
            btnUploadVideo = view.findViewById(R.id.btn_upload_video);
            btnClearPhotos = view.findViewById(R.id.btn_clear_photos);
            btnClearVideo = view.findViewById(R.id.btn_clear_video);
            btnSelectLocation = view.findViewById(R.id.btn_select_location);
            
            // Verify all critical views are initialized
            if (etContactNumber == null || tilContactNumber == null) {
                Log.e(TAG, "initViews: Contact number views not initialized");
            }
            
            Log.d(TAG, "initViews: All views initialized successfully");
            
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error initializing views: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupListeners() {
        // Check if fragment is attached
        if (!isAdded()) {
            Log.w(TAG, "setupListeners: Fragment not attached, aborting");
            return;
        }
        
        // Category radio buttons listener
        radioGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_food) {
                setupSubcategoryDropdown(true);
                layoutExpirySection.setVisibility(View.VISIBLE);
            } else {
                setupSubcategoryDropdown(false);
                layoutExpirySection.setVisibility(View.GONE);
            }
        });
        
        // Time unit radio group listener
        radioGroupTimeUnit.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_hours) {
                isHoursSelected = true;
                // Switch to hours mode (1-48 hours)
                sliderExpiry.setValueFrom(1);
                sliderExpiry.setValueTo(48);
                sliderExpiry.setValue(expiryHours);
                updateExpiryText();
            } else if (checkedId == R.id.radio_days) {
                isHoursSelected = false;
                // Switch to days mode (1-30 days)
                sliderExpiry.setValueFrom(1);
                sliderExpiry.setValueTo(30);
                sliderExpiry.setValue(expiryDays);
                updateExpiryText();
            }
        });
        
        // Expiry slider listener
        sliderExpiry.addOnChangeListener((slider, value, fromUser) -> {
            if (isHoursSelected) {
                expiryHours = (int) value;
            } else {
                expiryDays = (int) value;
            }
            updateExpiryText();
        });
        
        // Expiry info button
        if (getView() != null) {
            getView().findViewById(R.id.btn_expiry_info).setOnClickListener(v -> showExpiryInfo());
        }
        
        // Photo upload button
        btnUploadPhoto.setOnClickListener(v -> {
            if (isAdded() && selectedPhotoUris.size() < MAX_PHOTO_COUNT) {
                openMultipleImagePicker();
            } else {
                Toast.makeText(getContext(), "Maximum " + MAX_PHOTO_COUNT + " photos allowed", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Video upload button
        btnUploadVideo.setOnClickListener(v -> {
            if (isAdded()) {
                openVideoPicker();
            }
        });
        
        // Clear photos button
        btnClearPhotos.setOnClickListener(v -> {
            clearPhotos();
        });
        
        // Clear video button
        btnClearVideo.setOnClickListener(v -> {
            clearVideo();
        });
        
        // Location selection button
        if (btnSelectLocation != null) {
            btnSelectLocation.setOnClickListener(v -> {
                openLocationPicker();
            });
        }
        
        // Play video click listener
        iconPlayVideo.setOnClickListener(v -> {
            if (isVideoSelected && videoPreview != null) {
                if (videoPreview.isPlaying()) {
                    videoPreview.pause();
                    iconPlayVideo.setVisibility(View.VISIBLE);
                } else {
                    videoPreview.start();
                    iconPlayVideo.setVisibility(View.GONE);
                }
            }
        });
        
        // Share button click listener
        btnShareItem.setOnClickListener(v -> {
            if (isAdded() && validateForm()) { // Check if still attached before validation
                submitForm();
            }
        });
    }

    private void setupSubcategoryDropdown(boolean isFood) {
        try {
            // Set up the adapter for the dropdown based on category type
            String[] subcategories = isFood ? foodSubcategories : nonFoodSubcategories;
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    subcategories
            );
            
            dropdownSubcategory.setAdapter(adapter);
            
            // Clear previous selection
            dropdownSubcategory.setText("", false);
            
            // Set click listener to show dropdown when clicked
            dropdownSubcategory.setOnClickListener(v -> dropdownSubcategory.showDropDown());
            
        } catch (Exception e) {
            e.printStackTrace();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up subcategories: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupCountryCodePicker() {
        Log.d(TAG, "setupCountryCodePicker: Starting country code picker setup");
        try {
            // Set up country code picker with the main contact number EditText
            if (countryCodePicker != null && etContactNumber != null) {
                Log.d(TAG, "setupCountryCodePicker: CountryCodePicker and etContactNumber found, registering");
                
                // Register the main contact number field with the CountryCodePicker
                try {
                    countryCodePicker.registerCarrierNumberEditText(etContactNumber);
                    Log.d(TAG, "setupCountryCodePicker: Successfully registered carrier number");
                } catch (Exception e) {
                    Log.e(TAG, "setupCountryCodePicker: Error registering carrier number", e);
                    throw e;
                }
                
                // Set validity listener for real-time feedback
                try {
                    countryCodePicker.setPhoneNumberValidityChangeListener(isValidNumber -> {
                        Log.d(TAG, "Phone validity changed: " + isValidNumber);
                        if (!isValidNumber) {
                            tilContactNumber.setError("Invalid main contact number");
                        } else {
                            tilContactNumber.setError(null);
                        }
                    });
                    Log.d(TAG, "setupCountryCodePicker: Successfully set validity listener");
                } catch (Exception e) {
                    Log.e(TAG, "setupCountryCodePicker: Error setting validity listener", e);
                    throw e;
                }
                
                // Set up text change listeners for the other contact fields
                Log.d(TAG, "setupCountryCodePicker: Setting up validation for other contact fields");
                setupContactFieldValidation(etContact1, tilContact1, "Invalid primary contact");
                setupContactFieldValidation(etContact2, tilContact2, "Invalid secondary contact");
                
                Log.d(TAG, "setupCountryCodePicker: Country code picker setup successful");
            } else {
                // Log error if views are null
                String message = "Error: " + 
                    (countryCodePicker == null ? "CountryCodePicker is null" : "") + 
                    (etContactNumber == null ? "Contact field is null" : "");
                
                Log.e(TAG, "setupCountryCodePicker: " + message);
                
                if (getContext() != null) {
                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "setupCountryCodePicker: Error setting up country code picker", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error setting up country code picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Sets up basic phone number validation for other contact fields
     */
    private void setupContactFieldValidation(TextInputEditText editText, TextInputLayout inputLayout, String errorMessage) {
        if (editText == null || inputLayout == null) return;
        
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                // Basic validation - for more advanced validation, consider integrating with libphonenumber
                String phone = s.toString().trim();
                if (!phone.isEmpty() && (phone.length() < 10 || !android.util.Patterns.PHONE.matcher(phone).matches())) {
                    inputLayout.setError(errorMessage);
                } else {
                    inputLayout.setError(null);
                }
            }
        });
    }

    private void openMultipleImagePicker() {
        if (!isAdded()) {
            Log.w(TAG, "openMultipleImagePicker: Fragment not attached, aborting");
            return;
        }
        
        try {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Images"), PICK_MULTIPLE_IMAGES_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "openMultipleImagePicker: Error launching image picker", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening image picker", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void openVideoPicker() {
        if (!isAdded()) {
            Log.w(TAG, "openVideoPicker: Fragment not attached, aborting");
            return;
        }
        
        try {
            Intent intent = new Intent();
            intent.setType("video/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Video"), PICK_VIDEO_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "openVideoPicker: Error launching video picker", e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error opening video picker", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode != getActivity().RESULT_OK || data == null) {
            return;
        }
        
        switch (requestCode) {
            case PICK_MULTIPLE_IMAGES_REQUEST:
                handleMultipleImagesResult(data);
                break;
                
            case PICK_VIDEO_REQUEST:
                handleVideoResult(data);
                break;
        }
    }
    
    private void handleMultipleImagesResult(Intent data) {
        try {
            if (data.getClipData() != null) {
                // Multiple images selected
                int count = data.getClipData().getItemCount();
                int availableSlots = MAX_PHOTO_COUNT - selectedPhotoUris.size();
                int itemsToAdd = Math.min(count, availableSlots);
                
                for (int i = 0; i < itemsToAdd; i++) {
                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                    if (imageUri != null) {
                        selectedPhotoUris.add(imageUri);
                    }
                }
                
                if (count > availableSlots) {
                    Toast.makeText(getContext(), "Only " + availableSlots + " more photos can be added", Toast.LENGTH_SHORT).show();
                }
            } else if (data.getData() != null) {
                // Single image selected
                if (selectedPhotoUris.size() < MAX_PHOTO_COUNT) {
                    selectedPhotoUris.add(data.getData());
                }
            }
            
            updatePhotoUI();
        } catch (Exception e) {
            Log.e(TAG, "handleMultipleImagesResult: Error processing images", e);
            Toast.makeText(getContext(), "Error processing images", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void handleVideoResult(Intent data) {
        try {
            Uri videoUri = data.getData();
            if (videoUri != null) {
                selectedVideoUri = videoUri;
                isVideoSelected = true;
                
                // Update video preview
                videoPreview.setVideoURI(selectedVideoUri);
                videoPreview.setOnPreparedListener(mp -> {
                    mp.setVolume(1.0f, 1.0f);
                    mp.setLooping(false);
                    // Set thumbnail frame
                    mp.seekTo(100);
                });
                
                // Show video UI
                videoPreview.setVisibility(View.VISIBLE);
                iconPlayVideo.setVisibility(View.VISIBLE);
                imagePreview.setVisibility(View.GONE);
                textVideoStatus.setText("Video added - tap to play");
                textVideoStatus.setTextColor(getResources().getColor(R.color.secondary_color, null));
            }
        } catch (Exception e) {
            Log.e(TAG, "handleVideoResult: Error processing video", e);
            Toast.makeText(getContext(), "Error processing video", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updatePhotoUI() {
        // Update photo count
        textMediaCount.setText(selectedPhotoUris.size() + "/" + MAX_PHOTO_COUNT + " Photos");
        
        // Clear thumbnails
        layoutImagePreviews.removeAllViews();
        
        // Add thumbnails for each photo
        for (int i = 0; i < selectedPhotoUris.size(); i++) {
            Uri photoUri = selectedPhotoUris.get(i);
            
            // Create thumbnail ImageView
            ImageView thumbnail = new ImageView(getContext());
            int size = (int) (80 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(8, 0, 8, 0);
            thumbnail.setLayoutParams(params);
            thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumbnail.setImageURI(photoUri);
            
            // Add click listener to make it the main preview
            final int index = i;
            thumbnail.setOnClickListener(v -> {
                imagePreview.setImageURI(selectedPhotoUris.get(index));
                imagePreview.setVisibility(View.VISIBLE);
                videoPreview.setVisibility(View.GONE);
                iconPlayVideo.setVisibility(View.GONE);
            });
            
            layoutImagePreviews.addView(thumbnail);
        }
        
        // Show first photo in main preview if we have photos and no video is shown
        if (!selectedPhotoUris.isEmpty() && !isVideoSelected) {
            imagePreview.setImageURI(selectedPhotoUris.get(0));
            imagePreview.setVisibility(View.VISIBLE);
        }
    }
    
    private void clearPhotos() {
        selectedPhotoUris.clear();
        layoutImagePreviews.removeAllViews();
        textMediaCount.setText("0/" + MAX_PHOTO_COUNT + " Photos");
        
        // If no video is selected, hide the image preview
        if (!isVideoSelected) {
            imagePreview.setVisibility(View.GONE);
        }
    }
    
    private void clearVideo() {
        if (isVideoSelected) {
            selectedVideoUri = null;
            isVideoSelected = false;
            videoPreview.stopPlayback();
            videoPreview.setVisibility(View.GONE);
            iconPlayVideo.setVisibility(View.GONE);
            textVideoStatus.setText("No video uploaded");
            textVideoStatus.setTextColor(getResources().getColor(R.color.hint_color, null));
            
            // Show first photo if available
            if (!selectedPhotoUris.isEmpty()) {
                imagePreview.setImageURI(selectedPhotoUris.get(0));
                imagePreview.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Synchronized validation method to prevent race conditions
     */
    private synchronized boolean validateForm() {
        Log.d(TAG, "validateForm: Starting form validation");
        
        // Check if fragment is attached before proceeding
        if (!isAdded()) {
            Log.w(TAG, "validateForm: Fragment not attached, aborting validation");
            return false;
        }
        
        boolean isValid = true;
        
        try {
            // Validate photos or video
            if (selectedPhotoUris.isEmpty() && !isVideoSelected) {
                Log.d(TAG, "validateForm: No media selected");
                Toast.makeText(getContext(), "Please add at least one photo or video", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
            
            // Validate item name
            String itemName = etItemName != null ? etItemName.getText().toString().trim() : "";
            if (TextUtils.isEmpty(itemName)) {
                Log.d(TAG, "validateForm: Item name is empty");
                tilItemName.setError("Item name is required");
                isValid = false;
            } else {
                tilItemName.setError(null);
            }
        
        // Validate item description
        String itemDescription = etItemDescription.getText().toString().trim();
        if (TextUtils.isEmpty(itemDescription)) {
            tilItemDescription.setError("Description is required");
            isValid = false;
        } else {
            tilItemDescription.setError(null);
        }
        
        // Validate category selection
        if (radioGroupCategory.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Please select a category (Food or Non-Food)", Toast.LENGTH_SHORT).show();
            isValid = false;
        }
        
        // Validate subcategory
        String subcategory = dropdownSubcategory.getText().toString().trim();
        if (TextUtils.isEmpty(subcategory)) {
            tilItemSubcategory.setError("Subcategory is required");
            isValid = false;
        } else {
            tilItemSubcategory.setError(null);
        }
        
        // Validate expiry for food items
        if (radioFood.isChecked()) {
            if (isHoursSelected && expiryHours <= 0) {
                Toast.makeText(getContext(), "Please set a valid expiry time for food items", Toast.LENGTH_SHORT).show();
                isValid = false;
            } else if (!isHoursSelected && expiryDays <= 0) {
                Toast.makeText(getContext(), "Please set a valid expiry time for food items", Toast.LENGTH_SHORT).show();
                isValid = false;
            }
        }
        
        // Validate location
        String location = etItemLocation.getText().toString().trim();
        if (TextUtils.isEmpty(location)) {
            tilItemLocation.setError("Location is required");
            isValid = false;
        } else {
            tilItemLocation.setError(null);
        }
        
        // Validate primary contact (optional but must be valid if provided)
        String primaryContact = etContact1 != null ? etContact1.getText().toString().trim() : "";
        if (!primaryContact.isEmpty()) {
            if (primaryContact.length() < 10 || !android.util.Patterns.PHONE.matcher(primaryContact).matches()) {
                tilContact1.setError("Please enter a valid primary contact number");
                isValid = false;
            } else {
                tilContact1.setError(null);
            }
        }
        
        // Validate secondary contact (optional but must be valid if provided)
        String secondaryContact = etContact2 != null ? etContact2.getText().toString().trim() : "";
        if (!secondaryContact.isEmpty()) {
            if (secondaryContact.length() < 10 || !android.util.Patterns.PHONE.matcher(secondaryContact).matches()) {
                tilContact2.setError("Please enter a valid secondary contact number");
                isValid = false;
            } else {
                tilContact2.setError(null);
            }
        }
        
        Log.d(TAG, "validateForm: Form validation result: " + isValid);
        return isValid;
    } catch (Exception e) {
        Log.e(TAG, "validateForm: Error during validation", e);
        Toast.makeText(getContext(), "Error validating form: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        return false;
    }
}

    private void submitForm() {
        Log.d(TAG, "submitForm: Submitting form");
        
        // Check if fragment is attached before proceeding
        if (!isAdded()) {
            Log.w(TAG, "submitForm: Fragment not attached, aborting submission");
            return;
        }
        
        // Check if user is authenticated
        if (!com.example.madadgarapp.utils.SupabaseClient.AuthHelper.INSTANCE.isAuthenticated()) {
            Log.w(TAG, "submitForm: User not authenticated");
            if (getContext() != null) {
                Toast.makeText(getContext(), "Please sign in to share items", Toast.LENGTH_SHORT).show();
            }
            return;
        }
        
        try {
            // Disable button and show loading state
            btnShareItem.setEnabled(false);
            btnShareItem.setText("Sharing Item...");
            
            // Submit to Supabase using the repository bridge
            submitCompleteItem();
            
            Log.d(TAG, "submitForm: Form submission initiated");
        } catch (Exception e) {
            Log.e(TAG, "submitForm: Error submitting form", e);
            resetButtonState();
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error submitting form: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Submit the form data to Supabase
     */
    private void submitToSupabase() {
        // Run in background thread
        new Thread(() -> {
            try {
                Log.d(TAG, "submitToSupabase: Starting Supabase submission");
                
                // Get current user
                var currentUser = com.example.madadgarapp.utils.SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
                if (currentUser == null) {
                    throw new Exception("User not authenticated");
                }
                String userId = currentUser.getId();
                
                // Create repository instance
                com.example.madadgarapp.repository.ItemRepository repository = new com.example.madadgarapp.repository.ItemRepository();
                
                // Step 1: Upload images if any
                java.util.List<String> imageUrls = new java.util.ArrayList<>();
                if (!selectedPhotoUris.isEmpty()) {
                    Log.d(TAG, "submitToSupabase: Uploading " + selectedPhotoUris.size() + " images");
                    
                    // Note: This is a simplified approach. In production, you might want to use Kotlin coroutines
                    // For now, we'll use a blocking approach with proper error handling
                    try {
                        // Since we're in Java and the repository uses Kotlin coroutines,
                        // we'll need to create a bridge. For now, let's show the structure
                        Log.d(TAG, "Image upload would happen here");
                        // imageUrls = uploadImagesSync(repository, selectedPhotoUris, userId);
                    } catch (Exception e) {
                        Log.e(TAG, "Error uploading images: " + e.getMessage());
                        throw e;
                    }
                }
                
                // Step 2: Upload video if any
                String videoUrl = null;
                if (isVideoSelected && selectedVideoUri != null) {
                    Log.d(TAG, "submitToSupabase: Uploading video");
                    try {
                        // videoUrl = uploadVideoSync(repository, selectedVideoUri, userId);
                        Log.d(TAG, "Video upload would happen here");
                    } catch (Exception e) {
                        Log.e(TAG, "Error uploading video: " + e.getMessage());
                        throw e;
                    }
                }
                
                // Step 3: Create item data
                String itemName = etItemName.getText().toString().trim();
                String itemDescription = etItemDescription.getText().toString().trim();
                String subcategory = dropdownSubcategory.getText().toString().trim();
                String location = etItemLocation.getText().toString().trim();
                String mainContact = countryCodePicker.getFullNumber();
                String contact1 = etContact1.getText().toString().trim();
                String contact2 = etContact2.getText().toString().trim();
                
                // Determine category
                String mainCategory;
                String expiryTime = null;
                if (radioFood.isChecked()) {
                    mainCategory = "Food";
                    // Calculate expiry time
                    long currentTime = System.currentTimeMillis();
                    long expiryMillis;
                    if (isHoursSelected) {
                        expiryMillis = currentTime + (expiryHours * 60 * 60 * 1000L);
                    } else {
                        expiryMillis = currentTime + (expiryDays * 24 * 60 * 60 * 1000L);
                    }
                    expiryTime = java.time.Instant.ofEpochMilli(expiryMillis).toString();
                } else {
                    mainCategory = "Non-Food";
                }
                
                // For now, we'll create a simplified version since we need Kotlin coroutines
                // The actual implementation would use the repository to create the item
                
                Log.d(TAG, "submitToSupabase: Item data prepared");
                Log.d(TAG, "Title: " + itemName);
                Log.d(TAG, "Category: " + mainCategory + " > " + subcategory);
                Log.d(TAG, "Location: " + location);
                Log.d(TAG, "Images: " + selectedPhotoUris.size());
                Log.d(TAG, "Video: " + (isVideoSelected ? "Yes" : "No"));
                
                // Switch back to main thread for UI updates
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        try {
                            // For now, show success (in production, this would be after actual Supabase call)
                            Toast.makeText(getContext(), "Item shared successfully!", Toast.LENGTH_SHORT).show();
                            
                            // Clear form and reset button
                            clearForm();
                            resetButtonState();
                            
                            Log.d(TAG, "submitToSupabase: Form submitted successfully");
                        } catch (Exception e) {
                            Log.e(TAG, "Error in UI update", e);
                        }
                    });
                }
                
            } catch (Exception e) {
                Log.e(TAG, "submitToSupabase: Error in Supabase submission", e);
                
                // Switch back to main thread for error handling
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        resetButtonState();
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Error sharing item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * Submit the complete item to Supabase using the repository bridge
     */
    private void submitCompleteItem() {
        // Context must be available to proceed
        Context context = getContext();
        if (context == null) return;

        // Get current user ID
        var currentUser = com.example.madadgarapp.utils.SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(context, "Please sign in to share items", Toast.LENGTH_SHORT).show();
            resetButtonState();
            return;
        }
        String userId = currentUser.getId();

        // Get required fields
        String itemName = etItemName.getText().toString().trim();
        String itemDescription = etItemDescription.getText().toString().trim();
        String subcategory = dropdownSubcategory.getText().toString().trim();
        String location = etItemLocation.getText().toString().trim();
        String mainContact = countryCodePicker.getFullNumber();
        String contact1 = etContact1.getText().toString().trim();
        String contact2 = etContact2.getText().toString().trim();

        // Determine category
        String mainCategory = radioFood.isChecked() ? "Food" : "Non-Food";
        String expiryTime = null;
        if (radioFood.isChecked()) {
            long currentTime = System.currentTimeMillis();
            long expiryMillis = isHoursSelected ?
                    currentTime + (expiryHours * 60 * 60 * 1000L) :
                    currentTime + (expiryDays * 24 * 60 * 60 * 1000L);
            // Format without timezone Z to avoid Postgres parse error
            expiryTime = java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(expiryMillis),
                    java.time.ZoneOffset.UTC
            ).toString();
        }

        SupabaseItemBridge bridge = new SupabaseItemBridge();
        bridge.createCompleteItem(context,
                itemName,
                itemDescription,
                mainCategory,
                subcategory,
                location,
                selectedCoordinates != null ? selectedCoordinates.getLatitude() : null,
                selectedCoordinates != null ? selectedCoordinates.getLongitude() : null,
                "", // primary contact removed
                contact1,
                contact2,
                userId,
                expiryTime,
                selectedPhotoUris.isEmpty() ? null : selectedPhotoUris,
                isVideoSelected ? selectedVideoUri : null,
                new SupabaseItemBridge.RepositoryCallback<SupabaseItem>() {
                    @Override
                    public void onSuccess(SupabaseItem result) {
                        Toast.makeText(context, "Item successfully shared to Supabase!", Toast.LENGTH_SHORT).show();
                        clearForm();
                        resetButtonState();
                        
                        // Refresh the items list to show the newly shared item
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).refreshItemsList();
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(context, "Error sharing item: " + error, Toast.LENGTH_SHORT).show();
                        resetButtonState();
                    }
                }
        );
    }
    
    /**
     * Reset the button state back to normal
     */
    private void resetButtonState() {
        // Check fragment attachment state before proceeding
        if (!isAdded() || getActivity() == null) {
            Log.w(TAG, "resetButtonState: Fragment not attached or activity is null, aborting");
            return;
        }
        
        getActivity().runOnUiThread(() -> {
            try {
                if (btnShareItem != null) {
                    btnShareItem.setEnabled(true);
                    btnShareItem.setText("Share Item");
                }
            } catch (Exception e) {
                Log.e(TAG, "resetButtonState: Error resetting button state", e);
            }
        });
    }

    private void clearForm() {
        // Check if fragment is attached before proceeding
        if (!isAdded()) {
            Log.w(TAG, "clearForm: Fragment not attached, aborting");
            return;
        }
        
        try {
            // Reset media
            clearPhotos();
            clearVideo();
            
            // Reset category selection
            radioGroupCategory.clearCheck();
            layoutExpirySection.setVisibility(View.GONE);
            
            // Reset expiry timer
            if (sliderExpiry != null) {
                sliderExpiry.setValue(3);
            }
            expiryDays = 3;
            
            // Clear text fields (with null checks)
            if (etItemName != null) etItemName.setText("");
            if (etItemDescription != null) etItemDescription.setText("");
            if (dropdownSubcategory != null) dropdownSubcategory.setText("");
            if (etItemLocation != null) etItemLocation.setText("");
            if (etContactNumber != null) etContactNumber.setText("");
            if (etContact1 != null) etContact1.setText("");
            if (etContact2 != null) etContact2.setText("");
            
            // Clear location
            clearLocation();
            
            // Clear errors (with null checks)
            if (tilItemName != null) tilItemName.setError(null);
            if (tilItemDescription != null) tilItemDescription.setError(null);
            if (tilItemSubcategory != null) tilItemSubcategory.setError(null);
            if (tilItemLocation != null) tilItemLocation.setError(null);
            if (tilContactNumber != null) tilContactNumber.setError(null);
            if (tilContact1 != null) tilContact1.setError(null);
            if (tilContact2 != null) tilContact2.setError(null);
            
            Log.d(TAG, "clearForm: Form cleared successfully");
        } catch (Exception e) {
            Log.e(TAG, "clearForm: Error clearing form", e);
        }
    }
    
    /**
     * Updates the expiry text based on the current time unit selection
     */
    private void updateExpiryText() {
        if (textExpiryValue != null) {
            if (isHoursSelected) {
                textExpiryValue.setText(expiryHours + " hours until expiry");
            } else {
                textExpiryValue.setText(expiryDays + " days until expiry");
            }
        }
    }
    
    /**
     * Show information about automatic deletion of expired food items
     */
    private void showExpiryInfo() {
        if (getContext() != null) {
            String message = "Food items will be automatically deleted after the selected time expires. " +
                           "This helps keep fresh items visible to the community.";
            
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
            builder.setTitle("Auto-Delete Food Items")
                   .setMessage(message)
                   .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                   .setIcon(android.R.drawable.ic_dialog_info)
                   .show();
        }
    }
    
    /**
     * Open the location picker fragment
     */
    private void initLocationPermissionLauncher() {
        // Initialize permission launcher (stub implementation)
        locationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    // Permissions result handled here if needed in future
                });
    }

    private void openLocationPicker() {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        
        try {
            LocationPickerFragment locationPicker = LocationPickerFragment.newInstance();
            locationPicker.setLocationCallback(new LocationPickerFragment.LocationCallback() {
                @Override
                public void onLocationSelected(String location, LocationUtils.Coordinates coordinates) {
                    handleLocationSelected(location, coordinates);
                }
            });
            
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, locationPicker)
                    .addToBackStack(null)
                    .commit();
                    
        } catch (Exception e) {
            Log.e(TAG, "Error opening location picker", e);
            Toast.makeText(getContext(), "Error opening location picker", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle location selection from the location picker
     */
    private void handleLocationSelected(String location, LocationUtils.Coordinates coordinates) {
        try {
            // Update the location field
            if (etItemLocation != null) {
                etItemLocation.setText(location);
            }
            
            // Store the coordinates for later use
            selectedCoordinates = coordinates;
            
            // Update button text to show location is selected
            if (btnSelectLocation != null) {
                btnSelectLocation.setText("Location Selected");
            }
            
            Toast.makeText(getContext(), "Location selected: " + location, Toast.LENGTH_SHORT).show();
            
            Log.d(TAG, "Location selected: " + location + 
                  (coordinates != null ? " (" + coordinates.toString() + ")" : ""));
                  
        } catch (Exception e) {
            Log.e(TAG, "Error handling location selection", e);
            Toast.makeText(getContext(), "Error setting location", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Clear the selected location
     */
    private void clearLocation() {
        try {
            if (etItemLocation != null) {
                etItemLocation.setText("");
            }
            
            selectedCoordinates = null;
            
            if (btnSelectLocation != null) {
                btnSelectLocation.setText("Select Location");
            }
            
            Log.d(TAG, "Location cleared");
            
        } catch (Exception e) {
            Log.e(TAG, "Error clearing location", e);
        }
    }
}

