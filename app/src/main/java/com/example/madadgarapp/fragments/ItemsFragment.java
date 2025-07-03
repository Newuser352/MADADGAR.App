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

import com.example.madadgarapp.R;
import com.example.madadgarapp.activities.ItemDetailActivity;
import com.example.madadgarapp.adapters.ItemAdapter;
import com.example.madadgarapp.dialogs.CategoryDialogFragment;
import com.example.madadgarapp.models.Item;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;
import com.example.madadgarapp.R;
import com.example.madadgarapp.dialogs.CategoryDialogFragment;
import com.example.madadgarapp.repository.SupabaseItemBridge;
import com.example.madadgarapp.models.SupabaseItem;

public class ItemsFragment extends Fragment {
    private RecyclerView rvItems;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_items, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        initViews(view);
        
        // Set up RecyclerView with adapter
        setupRecyclerView();
        
        // Set up search and filter functionality
        setupSearch();
        setupFilterButton();
        setupFilterChips();
        
        // Load items from Firebase
        loadItems();
    }

    private void initViews(View view) {
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
            intent.putExtra("ITEM_ID", item.getId());
            intent.putExtra("ITEM_TITLE", item.getTitle());
            intent.putExtra("ITEM_DESCRIPTION", item.getDescription());
            intent.putExtra("ITEM_CATEGORY", item.getCategory());
            intent.putExtra("ITEM_SUBCATEGORY", item.getSubCategory());
            intent.putExtra("ITEM_LOCATION", item.getLocation());
            intent.putExtra("ITEM_CONTACT", item.getContact());
            intent.putExtra("ITEM_USER", item.getOwner());
            intent.putExtra("ITEM_TIMESTAMP", item.getTimestamp());
            intent.putExtra("ITEM_EXPIRATION", item.getExpiration());
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

    private void loadItems() {
        showLoading(true);
        
        // Load items from Supabase
        SupabaseItemBridge bridge = new SupabaseItemBridge();
        bridge.getActiveItems(50, 0, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
            @Override
            public void onSuccess(List<SupabaseItem> supabaseItems) {
                // Convert SupabaseItems to Items for adapter
                List<Item> items = new ArrayList<>();
                for (SupabaseItem supabaseItem : supabaseItems) {
                    Item item = convertSupabaseItemToItem(supabaseItem);
                    items.add(item);
                }
                
                itemAdapter.setItems(items);
                applyFilters();
                showLoading(false);
                
                if (items.isEmpty()) {
                    Toast.makeText(getContext(), "No items found. Share your first item!", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String error) {
                showLoading(false);
                Toast.makeText(getContext(), "Error loading items: " + error, Toast.LENGTH_SHORT).show();
                
                // Show empty list on error
                itemAdapter.setItems(new ArrayList<>());
                updateEmptyState();
            }
        });
    }
    
    /**
     * Convert SupabaseItem to Item for adapter compatibility
     */
    private Item convertSupabaseItemToItem(SupabaseItem supabaseItem) {
        // Parse timestamps
        long createdAt = parseTimestamp(supabaseItem.getCreatedAt());
        long expiresAt = parseTimestamp(supabaseItem.getExpiresAt());
        
        // Get the first image URL if available
        String imageUrl = null;
        if (supabaseItem.getImageUrls() != null && !supabaseItem.getImageUrls().isEmpty()) {
            imageUrl = supabaseItem.getImageUrls().get(0);
        }
        
        return new Item(
            supabaseItem.getId(),
            supabaseItem.getTitle(),
            supabaseItem.getDescription(),
            supabaseItem.getMainCategory(),
            supabaseItem.getSubCategory(),
            supabaseItem.getLocation(),
            supabaseItem.getContactNumber(),
            imageUrl,
            supabaseItem.getOwnerId(),
            createdAt,
            expiresAt
        );
    }
    
    /**
     * Parse timestamp string to long
     */
    private long parseTimestamp(String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return System.currentTimeMillis();
        }
        
        try {
            // Parse ISO timestamp
            return java.time.Instant.parse(timestamp).toEpochMilli();
        } catch (Exception e) {
            // Fallback to current time if parsing fails
            return System.currentTimeMillis();
        }
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
}
