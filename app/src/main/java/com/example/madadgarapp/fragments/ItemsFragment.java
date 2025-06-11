package com.example.madadgarapp.fragments;

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

public class ItemsFragment extends Fragment {
    private RecyclerView rvItems;
    private TextInputLayout tilSearchItems;
    private TextInputEditText etSearchItems;
    private TextView textItemsDescription;
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
        textItemsDescription = view.findViewById(R.id.text_items_description);
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
            // Handle item click - could navigate to detail view
            Toast.makeText(getContext(), "Selected: " + item.getTitle(), Toast.LENGTH_SHORT).show();
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
        
        // TODO: Replace this with actual Firebase or API data loading
        // For demo purposes, we'll create some sample items
        new Handler().postDelayed(() -> {
            List<Item> items = createSampleItems();
            itemAdapter.setItems(items);
            applyFilters();
            showLoading(false);
        }, 1000); // Simulate network delay
    }
    
    private List<Item> createSampleItems() {
        List<Item> items = new ArrayList<>();
        
        // Food items
        items.add(new Item("1", "Homemade Biryani", "Freshly made biryani, enough for 3-4 people", 
                "Food", "Cooked Food", "Main Campus", "+923001234567", 
                null, "user1", System.currentTimeMillis(), System.currentTimeMillis() + 86400000));
                
        items.add(new Item("2", "Fresh Vegetables", "Bundle of fresh vegetables from local market", 
                "Food", "Uncooked Food", "North Campus", "+923001234568", 
                null, "user2", System.currentTimeMillis(), System.currentTimeMillis() + 172800000));
                
        // Non-Food items
        items.add(new Item("3", "Engineering Textbooks", "Set of engineering textbooks for first year", 
                "Non-Food", "Books", "South Campus", "+923001234569", 
                null, "user3", System.currentTimeMillis(), System.currentTimeMillis() + 604800000));
                
        items.add(new Item("4", "Study Desk", "Wooden study desk in good condition", 
                "Non-Food", "Furniture", "East Campus", "+923001234570", 
                null, "user4", System.currentTimeMillis(), System.currentTimeMillis() + 1209600000));
                
        items.add(new Item("5", "Laptop Charger", "HP laptop charger, compatible with most models", 
                "Non-Food", "Electronics", "West Campus", "+923001234571", 
                null, "user5", System.currentTimeMillis(), System.currentTimeMillis() + 259200000));
                
        return items;
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
