package com.example.madadgarapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.madadgarapp.R;
import com.example.madadgarapp.activities.ItemDetailActivity;
import com.example.madadgarapp.adapters.ItemAdapter;
import com.example.madadgarapp.dialogs.CategoryDialogFragment;
import com.example.madadgarapp.models.Item;
import com.example.madadgarapp.models.SupabaseItem;
import com.example.madadgarapp.repository.SupabaseItemBridge;
import com.example.madadgarapp.utils.SupabaseClient;
import com.example.madadgarapp.utils.TimeUtils;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import com.example.madadgarapp.utils.LocationUtils;

import java.util.ArrayList;
import java.util.List;

public class ItemsFragment extends Fragment {
    private RecyclerView rvItems;
    private SwipeRefreshLayout swipeRefreshLayout;
    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private TextInputLayout tilSearchItems;
    private TextInputEditText etSearchItems;
    private ImageButton btnFilter;
    private LinearLayout emptyStateContainer;
    private TextView textEmptyItems;
    private TextView textEmptyWithFilters;
    private ProgressBar progressItems;
    private ChipGroup filterChipGroup;
    private Chip chipCategory;
    private Chip chipClearFilters;
    
    private ItemAdapter itemAdapter;
    private String currentSearchQuery = "";
    private String selectedCategory = "";
    
    // For search debouncing
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private static final long SEARCH_DELAY_MS = 300;
    private Runnable searchRunnable;

    public static ItemsFragment newInstance() {
        return new ItemsFragment();
    }

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireContext());
        initViews(view);
        
        // Set up RecyclerView with adapter
        setupRecyclerView();
        
        // Set up search and filter functionality
        setupSearch();
        setupFilterButton();
        setupFilterChips();
        setupSwipeToRefresh();
        
        // Get current location first (non-blocking)
        fetchCurrentLocation();

        // Load items from Supabase
        requestLocationPermission();
        loadItems();
    }

    private void requestLocationPermission() {
        if (!LocationUtils.hasLocationPermission(requireContext())) {
            ActivityCompat.requestPermissions(requireActivity(),
                LocationUtils.REQUIRED_PERMISSIONS,
                LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            fetchLocationAndUpdateAdapter();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocationAndUpdateAdapter();
            } else {
                Toast.makeText(requireContext(), "Location permission is required for geofiltering", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchLocationAndUpdateAdapter() {
        // Use coroutines to get location asynchronously
        new Thread(() -> {
            try {
                LocationUtils.Coordinates coordinates = LocationUtils.getCurrentLocationSync(requireContext());
                if (coordinates != null) {
                    requireActivity().runOnUiThread(() -> {
                        itemAdapter.setCurrentLocation(coordinates);
                    });
                }
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Failed to fetch location", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        rvItems = view.findViewById(R.id.rv_items);
        emptyStateContainer = view.findViewById(R.id.empty_state_container);
        textEmptyItems = view.findViewById(R.id.text_empty_items);
        textEmptyWithFilters = view.findViewById(R.id.text_empty_with_filters);
        tilSearchItems = view.findViewById(R.id.til_search_items);
        etSearchItems = view.findViewById(R.id.et_search_items);
        btnFilter = view.findViewById(R.id.btn_filter);
        progressItems = view.findViewById(R.id.progress_items);
        filterChipGroup = view.findViewById(R.id.filter_chip_group);
        chipCategory = view.findViewById(R.id.chip_category);
        chipClearFilters = view.findViewById(R.id.chip_clear_filters);
    }

    private void setupRecyclerView() {
        rvItems.setLayoutManager(new LinearLayoutManager(getContext()));
        rvItems.setHasFixedSize(true);
        
        // Initialize the adapter with click listener
        itemAdapter = new ItemAdapter(requireContext(), item -> {
            // Launch ItemDetailActivity on item click
            Intent intent = new Intent(getContext(), ItemDetailActivity.class);
            intent.putExtra(ItemDetailActivity.EXTRA_ITEM, item);
            startActivity(intent);
        });
        
        rvItems.setAdapter(itemAdapter);
    }
    
    private void setupSearch() {
        // Add TextWatcher with debounce for search functionality
        etSearchItems.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Cancel any pending searches
                searchHandler.removeCallbacks(searchRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                currentSearchQuery = s.toString().trim();
                
                // Debounce search to avoid too frequent updates
                searchRunnable = () -> applyFilters();
                searchHandler.postDelayed(searchRunnable, SEARCH_DELAY_MS);
            }
        });
    }
    
    private void setupFilterButton() {
        btnFilter.setOnClickListener(v -> showCategoriesDialog());
    }
    
    private void setupFilterChips() {
        // Set up the category chip close icon listener
        chipCategory.setOnCloseIconClickListener(v -> {
            selectedCategory = "";
            applyFilters();
            updateFilterChips();
        });
        
        // Set up clear all filters button
        chipClearFilters.setOnClickListener(v -> {
            selectedCategory = "";
            currentSearchQuery = "";
            etSearchItems.setText("");
            applyFilters();
            updateFilterChips();
        });
    }

    // Load / refresh items
    private void fetchCurrentLocation() {
        if (!com.example.madadgarapp.utils.LocationUtils.hasLocationPermission(requireContext())) {
            // Location permission not granted; skip radius filter
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                com.example.madadgarapp.utils.LocationUtils.Coordinates coords =
                        new com.example.madadgarapp.utils.LocationUtils.Coordinates(location.getLatitude(), location.getLongitude());
                if (itemAdapter != null) {
                    itemAdapter.setCurrentLocation(coords);
                }
            }
        });
    }

    private void loadItems() {
        if (!swipeRefreshLayout.isRefreshing()) {
            showLoading(true);
        }
        
        // Load items from Supabase
        SupabaseItemBridge bridge = new SupabaseItemBridge();
        bridge.getActiveItems(50, 0, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
            @Override
            public void onSuccess(List<SupabaseItem> supabaseItems) {
                // Convert SupabaseItems to Items for adapter, excluding current user's own posts
                List<Item> items = new ArrayList<>();

                // Identify current user (if authenticated)
                String currentUserId = null;
                if (SupabaseClient.AuthHelper.INSTANCE.isAuthenticated()) {
                    var currentUser = SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
                    if (currentUser != null) {
                        currentUserId = currentUser.getId();
                    }
                }

                for (SupabaseItem supabaseItem : supabaseItems) {
                    // Skip items that belong to the current user
                    if (currentUserId != null && currentUserId.equals(supabaseItem.getOwnerId())) {
                        continue;
                    }
                    Item item = convertSupabaseItemToItem(supabaseItem);
                    items.add(item);
                }
                
                itemAdapter.setItems(items);
                applyFilters();
                showLoading(false);
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                
                if (items.isEmpty()) {
                    if (isAdded()) {
                        Toast.makeText(requireContext(), "No items found. Share your first item!", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (isAdded()) {
                    Toast.makeText(requireContext(), "Error loading items: " + error, Toast.LENGTH_SHORT).show();
                }
                
                // Show empty list on error
                itemAdapter.setItems(new ArrayList<>());
                updateEmptyState();
            }
        });
    }
    
    /**
     * Convert SupabaseItem to Item for adapter compatibility
     */
    private String determinePrimaryContact(SupabaseItem supabaseItem) {
        String primaryContact = supabaseItem.getContactNumber();
        if ((primaryContact == null || primaryContact.isEmpty()) && supabaseItem.getContact1() != null && !supabaseItem.getContact1().isEmpty()) {
            primaryContact = supabaseItem.getContact1();
        }
        if ((primaryContact == null || primaryContact.isEmpty()) && supabaseItem.getContact2() != null && !supabaseItem.getContact2().isEmpty()) {
            primaryContact = supabaseItem.getContact2();
        }
        return primaryContact;
    }

    private Item convertSupabaseItemToItem(SupabaseItem supabaseItem) {
        // Convert SupabaseItem to our Java Item model while preserving all media data

        // Parse timestamps using TimeUtils for better formatting
        long createdAt = supabaseItem.getCreatedAt() != null ?
                TimeUtils.parseTimestamp(supabaseItem.getCreatedAt()) : System.currentTimeMillis();
        long expiresAt = supabaseItem.getExpiresAt() != null ?
                TimeUtils.parseTimestamp(supabaseItem.getExpiresAt()) : Long.MAX_VALUE;

        // Use the first image as a cover/thumbnail for quick preview in the list
        // Determine primary contact number
        String primaryContact = determinePrimaryContact(supabaseItem);
        if ((primaryContact == null || primaryContact.isEmpty()) && supabaseItem.getContact1() != null && !supabaseItem.getContact1().isEmpty()) {
            primaryContact = supabaseItem.getContact1();
        }
        if ((primaryContact == null || primaryContact.isEmpty()) && supabaseItem.getContact2() != null && !supabaseItem.getContact2().isEmpty()) {
            primaryContact = supabaseItem.getContact2();
        }

        String coverImageUrl = null;
        if (supabaseItem.getImageUrls() != null && !supabaseItem.getImageUrls().isEmpty()) {
            coverImageUrl = supabaseItem.getImageUrls().get(0);
        }

        // Create Item instance with coordinates
                Item item = new Item(
                        supabaseItem.getId(),
                        supabaseItem.getTitle(),
                        supabaseItem.getDescription(),
                        supabaseItem.getMainCategory(),
                        supabaseItem.getSubCategory(),
                        supabaseItem.getLocation(),
                        supabaseItem.getLatitude(),
                        supabaseItem.getLongitude(),
                        primaryContact,
                        supabaseItem.getOwnerEmail(),
                        coverImageUrl,
                        supabaseItem.getOwnerId(),
                        createdAt,
                        expiresAt
                );

        // Attach full media lists so that ItemDetailActivity can render them
        item.setImageUrls(supabaseItem.getImageUrls());
        item.setVideoUrl(supabaseItem.getVideoUrl());

        return item;
    }
    
    private void applyFilters() {
        itemAdapter.filterItems(currentSearchQuery, selectedCategory);
        updateFilterChips();
        updateEmptyState();
    }
    
    private void updateFilterChips() {
        boolean hasFilters = !currentSearchQuery.isEmpty() || !selectedCategory.isEmpty();
        
        // Show/hide the filter chip group
        filterChipGroup.setVisibility(hasFilters ? View.VISIBLE : View.GONE);
        
        // Update category chip if needed
        if (!selectedCategory.isEmpty()) {
            chipCategory.setText(getString(R.string.filter_category, selectedCategory));
            chipCategory.setVisibility(View.VISIBLE);
        } else {
            chipCategory.setVisibility(View.GONE);
        }
    }
    
    private void updateEmptyState() {
        int itemCount = itemAdapter.getFilteredItemCount();
        boolean hasFilters = itemAdapter.hasActiveFilters();
        
        if (itemCount == 0) {
            rvItems.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.VISIBLE);
            
            // Show different message if filters are active
            if (hasFilters) {
                textEmptyWithFilters.setVisibility(View.VISIBLE);
            } else {
                textEmptyWithFilters.setVisibility(View.GONE);
            }
        } else {
            rvItems.setVisibility(View.VISIBLE);
            emptyStateContainer.setVisibility(View.GONE);
        }
    }
    
    private void setupSwipeToRefresh() {
        swipeRefreshLayout.setOnRefreshListener(this::loadItems);
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            progressItems.setVisibility(View.VISIBLE);
            rvItems.setVisibility(View.GONE);
            emptyStateContainer.setVisibility(View.GONE);
        } else {
            progressItems.setVisibility(View.GONE);
        }
    }

    
    /**
     * Shows the categories dialog when the filter button is clicked
     */
    private void showCategoriesDialog() {
        CategoryDialogFragment dialogFragment = new CategoryDialogFragment();
        dialogFragment.setOnCategorySelectedListener((mainCategory, subCategory) -> {
            // Handle category selection
            if (!mainCategory.isEmpty() && !subCategory.isEmpty()) {
                selectedCategory = mainCategory + " > " + subCategory;
            } else {
                selectedCategory = "";
            }
            applyFilters();
        });
        dialogFragment.show(getChildFragmentManager(), "CategoryDialog");
    }
    
    @Override
    public void onDestroyView() {
        // Remove any pending search callbacks
        searchHandler.removeCallbacks(searchRunnable);
        super.onDestroyView();
    }
    
    /**
     * Public method to refresh items list
     * Can be called from activity or other fragments
     */
    public void refreshItems() {
        loadItems();
    }
    
    /**
     * Open specific item by ID (called from notification clicks)
     */
    public void openItemById(String itemId) {
        try {
            android.util.Log.d("ItemsFragment", "=== OPENING ITEM BY ID ===");
            android.util.Log.d("ItemsFragment", "Target item ID: " + itemId);
            android.util.Log.d("ItemsFragment", "Adapter null? " + (itemAdapter == null));
            android.util.Log.d("ItemsFragment", "Fragment added? " + isAdded());
            android.util.Log.d("ItemsFragment", "Context null? " + (getContext() == null));
            
            if (itemAdapter == null) {
                android.util.Log.w("ItemsFragment", "Adapter not ready, loading items first");
                Toast.makeText(getContext(), "Loading items...", Toast.LENGTH_SHORT).show();
                
                // Try to reload items and then search again
                loadItemsWithCallback(() -> {
                    android.util.Log.d("ItemsFragment", "Items loaded, now searching for item: " + itemId);
                    findAndOpenItem(itemId);
                });
                return;
            }
            
            android.util.Log.d("ItemsFragment", "Adapter ready, searching for item");
            findAndOpenItem(itemId);
            
        } catch (Exception e) {
            android.util.Log.e("ItemsFragment", "Error opening item by ID", e);
            Toast.makeText(getContext(), "Error opening item: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Find and open item by ID
     */
    private void findAndOpenItem(String itemId) {
        try {
            android.util.Log.d("ItemsFragment", "=== FINDING ITEM ===");
            android.util.Log.d("ItemsFragment", "Looking for item ID: " + itemId);
            
            Item targetItem = null;
            
            // Get current items from adapter
            List<Item> currentItems = itemAdapter.getAllItems();
            android.util.Log.d("ItemsFragment", "Current items list size: " + (currentItems != null ? currentItems.size() : "null"));
            
            if (currentItems != null) {
                android.util.Log.d("ItemsFragment", "Searching through " + currentItems.size() + " items:");
                
                // Search through current items list
                for (int i = 0; i < currentItems.size(); i++) {
                    Item item = currentItems.get(i);
                    android.util.Log.d("ItemsFragment", "  Item " + (i+1) + ": ID=" + item.getId() + ", Title=" + item.getTitle());
                    
                    if (itemId.equals(item.getId())) {
                        targetItem = item;
                        android.util.Log.d("ItemsFragment", "  *** MATCH FOUND! ***");
                        break;
                    }
                }
            }
            
            if (targetItem != null) {
                android.util.Log.d("ItemsFragment", "Found item: " + targetItem.getTitle());
                // Open the item detail activity
                Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
                intent.putExtra(ItemDetailActivity.EXTRA_ITEM, targetItem);
                startActivity(intent);
                
                Toast.makeText(getContext(), "Opening: " + targetItem.getTitle(), Toast.LENGTH_SHORT).show();
            } else {
                android.util.Log.w("ItemsFragment", "Item not found in current list: " + itemId);
                android.util.Log.w("ItemsFragment", "Will try to fetch from database...");
                Toast.makeText(getContext(), "Item not found in list, fetching...", Toast.LENGTH_SHORT).show();
                
                // Try to fetch the specific item from the database
                fetchSpecificItemFromSupabase(itemId);
            }
            
        } catch (Exception e) {
            android.util.Log.e("ItemsFragment", "Error in findAndOpenItem", e);
            Toast.makeText(getContext(), "Error finding item", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Load items with a callback for when loading is complete
     */
    private void loadItemsWithCallback(Runnable callback) {
        try {
            android.util.Log.d("ItemsFragment", "Loading items with callback");
            
            // Show loading
            showLoading(true);
            
            // Load items from Supabase
            SupabaseItemBridge bridge = new SupabaseItemBridge();
            bridge.getActiveItems(50, 0, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
                @Override
                public void onSuccess(List<SupabaseItem> supabaseItems) {
                    // Convert SupabaseItems to Items for adapter, excluding current user's own posts
                    List<Item> items = new ArrayList<>();

                    // Identify current user (if authenticated)
                    String currentUserId = null;
                    if (SupabaseClient.AuthHelper.INSTANCE.isAuthenticated()) {
                        var currentUser = SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
                        if (currentUser != null) {
                            currentUserId = currentUser.getId();
                        }
                    }

                    for (SupabaseItem supabaseItem : supabaseItems) {
                        // Skip items that belong to the current user
                        if (currentUserId != null && currentUserId.equals(supabaseItem.getOwnerId())) {
                            continue;
                        }
                        Item item = convertSupabaseItemToItem(supabaseItem);
                        items.add(item);
                    }
                    
                    itemAdapter.setItems(items);
                    applyFilters();
                    showLoading(false);
                    
                    android.util.Log.d("ItemsFragment", "Items loaded successfully: " + items.size());
                    
                    // Execute callback
                    if (callback != null) {
                        callback.run();
                    }
                }
                
                @Override
                public void onError(String error) {
                    showLoading(false);
                    android.util.Log.e("ItemsFragment", "Error loading items: " + error);
                    Toast.makeText(getContext(), "Error loading items: " + error, Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            android.util.Log.e("ItemsFragment", "Error in loadItemsWithCallback", e);
            showLoading(false);
        }
    }
    
    /**
     * Fetch a specific item from Supabase if not found in current list
     */
    private void fetchSpecificItemFromSupabase(String itemId) {
        try {
            android.util.Log.d("ItemsFragment", "Fetching specific item from Supabase: " + itemId);
            
            // Show loading toast
            Toast.makeText(getContext(), "Fetching item...", Toast.LENGTH_SHORT).show();
            
            // Use SupabaseItemBridge to get the specific item
            SupabaseItemBridge bridge = new SupabaseItemBridge();
            
            // Create a simple callback to get single item by filtering active items
            bridge.getActiveItems(1000, 0, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
                @Override
                public void onSuccess(List<SupabaseItem> supabaseItems) {
                    try {
                        SupabaseItem targetSupabaseItem = null;
                        
                        // Find the specific item
                        for (SupabaseItem supabaseItem : supabaseItems) {
                            if (itemId.equals(supabaseItem.getId())) {
                                targetSupabaseItem = supabaseItem;
                                break;
                            }
                        }
                        
                        if (targetSupabaseItem != null) {
                            Item item = convertSupabaseItemToItem(targetSupabaseItem);
                            android.util.Log.d("ItemsFragment", "Successfully fetched item: " + item.getTitle());
                            
                            // Open the item detail activity
                            Intent intent = new Intent(getActivity(), ItemDetailActivity.class);
                            intent.putExtra(ItemDetailActivity.EXTRA_ITEM, item);
                            startActivity(intent);
                            
                            Toast.makeText(getContext(), "Opening: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), "Item not found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        android.util.Log.e("ItemsFragment", "Error processing fetched item", e);
                        Toast.makeText(getContext(), "Error opening item", Toast.LENGTH_SHORT).show();
                    }
                }
                
                @Override
                public void onError(String error) {
                    android.util.Log.e("ItemsFragment", "Error fetching specific item: " + error);
                    Toast.makeText(getContext(), "Item not found or removed", Toast.LENGTH_SHORT).show();
                }
            });
            
        } catch (Exception e) {
            android.util.Log.e("ItemsFragment", "Error in fetchSpecificItemFromSupabase", e);
            Toast.makeText(getContext(), "Error fetching item", Toast.LENGTH_SHORT).show();
        }
    }
}
