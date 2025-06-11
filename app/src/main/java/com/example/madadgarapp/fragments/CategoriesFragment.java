package com.example.madadgarapp.fragments;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.madadgarapp.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CategoriesFragment extends Fragment {
    private TextView textEmptyCategories;
    private TextView textSelectedCategory;
    private MaterialCardView cardSelectCategories;
    
    // Main categories
    private String selectedMainCategory = "";
    private String selectedSubCategory = "";
    
    // Dialog views
    private View dialogView;
    private LinearLayout layoutFoodSubcategories;
    private LinearLayout layoutNonFoodSubcategories;
    private ImageView imageFoodDropdown;
    private ImageView imageNonFoodDropdown;

    public static CategoriesFragment newInstance() {
        return new CategoriesFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        initViews(view);
        
        // Set up category selection
        setupCategorySelection();
    }

    private void initViews(View view) {
        textEmptyCategories = view.findViewById(R.id.text_empty_categories);
        textSelectedCategory = view.findViewById(R.id.text_selected_category);
        cardSelectCategories = view.findViewById(R.id.card_select_categories);
    }

    private void setupCategorySelection() {
        cardSelectCategories.setOnClickListener(v -> showCategoryDialog());
    }

    private void showCategoryDialog() {
        // Inflate the dialog view
        dialogView = getLayoutInflater().inflate(R.layout.dialog_categories, null);
        
        // Initialize dialog views
        initDialogViews();
        
        // Set up click listeners for categories
        setupCategoryListeners();
        
        // Create and show the dialog
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true);
        
        builder.create().show();
    }
    
    private void initDialogViews() {
        // Main categories layouts
        LinearLayout layoutFoodHeader = dialogView.findViewById(R.id.layout_food_header);
        LinearLayout layoutNonFoodHeader = dialogView.findViewById(R.id.layout_non_food_header);
        
        // Subcategories layouts
        layoutFoodSubcategories = dialogView.findViewById(R.id.layout_food_subcategories);
        layoutNonFoodSubcategories = dialogView.findViewById(R.id.layout_non_food_subcategories);
        
        // Dropdown icons
        imageFoodDropdown = dialogView.findViewById(R.id.image_food_dropdown);
        imageNonFoodDropdown = dialogView.findViewById(R.id.image_non_food_dropdown);
        
        // Food subcategories
        LinearLayout layoutCookedFood = dialogView.findViewById(R.id.layout_cooked_food);
        LinearLayout layoutUncookedFood = dialogView.findViewById(R.id.layout_uncooked_food);
        
        // Non-Food subcategories
        LinearLayout layoutElectronics = dialogView.findViewById(R.id.layout_electronics);
        LinearLayout layoutFurniture = dialogView.findViewById(R.id.layout_furniture);
        LinearLayout layoutBooks = dialogView.findViewById(R.id.layout_books);
        LinearLayout layoutClothing = dialogView.findViewById(R.id.layout_clothing);
        LinearLayout layoutOther = dialogView.findViewById(R.id.layout_other);
        
        // Buttons
        View buttonCancel = dialogView.findViewById(R.id.button_cancel);
        View buttonSelect = dialogView.findViewById(R.id.button_select);
        
        // Set click listeners for buttons
        buttonCancel.setOnClickListener(v -> {
            // Reset selected categories if canceled
            selectedMainCategory = "";
            selectedSubCategory = "";
            ((DialogInterface) v.getTag()).dismiss();
        });
        
        buttonSelect.setOnClickListener(v -> {
            // Update UI with selected category
            updateSelectedCategory();
            ((DialogInterface) v.getTag()).dismiss();
        });
    }
    
    private void setupCategoryListeners() {
        // Food category header click listener
        dialogView.findViewById(R.id.layout_food_header).setOnClickListener(v -> {
            // Toggle food subcategories visibility
            boolean isVisible = layoutFoodSubcategories.getVisibility() == View.VISIBLE;
            layoutFoodSubcategories.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            imageFoodDropdown.setRotation(isVisible ? 0 : 180);
            
            // Hide non-food subcategories if they're visible
            layoutNonFoodSubcategories.setVisibility(View.GONE);
            imageNonFoodDropdown.setRotation(0);
            
            // Set selected main category
            selectedMainCategory = "Food";
        });
        
        // Non-Food category header click listener
        dialogView.findViewById(R.id.layout_non_food_header).setOnClickListener(v -> {
            // Toggle non-food subcategories visibility
            boolean isVisible = layoutNonFoodSubcategories.getVisibility() == View.VISIBLE;
            layoutNonFoodSubcategories.setVisibility(isVisible ? View.GONE : View.VISIBLE);
            imageNonFoodDropdown.setRotation(isVisible ? 0 : 180);
            
            // Hide food subcategories if they're visible
            layoutFoodSubcategories.setVisibility(View.GONE);
            imageFoodDropdown.setRotation(0);
            
            // Set selected main category
            selectedMainCategory = "Non-Food";
        });
        
        // Food subcategories click listeners
        dialogView.findViewById(R.id.layout_cooked_food).setOnClickListener(v -> {
            selectedSubCategory = "Cooked Food";
        });
        
        dialogView.findViewById(R.id.layout_uncooked_food).setOnClickListener(v -> {
            selectedSubCategory = "Uncooked Food";
        });
        
        // Non-Food subcategories click listeners
        dialogView.findViewById(R.id.layout_electronics).setOnClickListener(v -> {
            selectedSubCategory = "Electronics";
        });
        
        dialogView.findViewById(R.id.layout_furniture).setOnClickListener(v -> {
            selectedSubCategory = "Furniture";
        });
        
        dialogView.findViewById(R.id.layout_books).setOnClickListener(v -> {
            selectedSubCategory = "Books";
        });
        
        dialogView.findViewById(R.id.layout_clothing).setOnClickListener(v -> {
            selectedSubCategory = "Clothing";
        });
        
        dialogView.findViewById(R.id.layout_other).setOnClickListener(v -> {
            selectedSubCategory = "Other";
        });
    }
    
    private void updateSelectedCategory() {
        if (!selectedMainCategory.isEmpty() && !selectedSubCategory.isEmpty()) {
            String fullCategory = selectedMainCategory + " > " + selectedSubCategory;
            textSelectedCategory.setText(fullCategory);
            textSelectedCategory.setTextColor(getResources().getColor(R.color.text_color, null));
            showEmptyState(false);
        } else {
            textSelectedCategory.setText("No category selected");
            textSelectedCategory.setTextColor(getResources().getColor(R.color.hint_color, null));
            showEmptyState(true);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            textEmptyCategories.setVisibility(View.VISIBLE);
        } else {
            textEmptyCategories.setVisibility(View.GONE);
        }
    }
}
