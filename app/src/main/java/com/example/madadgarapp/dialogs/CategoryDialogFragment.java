package com.example.madadgarapp.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.madadgarapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CategoryDialogFragment extends DialogFragment {
    private String selectedMainCategory = "";
    private String selectedSubCategory = "";
    
    // Dialog views
    private View dialogView;
    private LinearLayout layoutFoodSubcategories;
    private LinearLayout layoutNonFoodSubcategories;
    private ImageView imageFoodDropdown;
    private ImageView imageNonFoodDropdown;
    
    // Callback interface
    public interface OnCategorySelectedListener {
        void onCategorySelected(String mainCategory, String subCategory);
    }
    
    private OnCategorySelectedListener listener;
    
    public void setOnCategorySelectedListener(OnCategorySelectedListener listener) {
        this.listener = listener;
    }
    
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Inflate the dialog view
        dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_categories, null);
        
        // Initialize dialog views
        initDialogViews();
        
        // Set up click listeners for categories
        setupCategoryListeners();
        
        // Create and return the dialog
        return new MaterialAlertDialogBuilder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();
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
        
        // Buttons
        View buttonCancel = dialogView.findViewById(R.id.button_cancel);
        View buttonSelect = dialogView.findViewById(R.id.button_select);
        
        // Set click listeners for buttons
        buttonCancel.setOnClickListener(v -> {
            // Reset selected categories if canceled
            selectedMainCategory = "";
            selectedSubCategory = "";
            if (listener != null) {
                listener.onCategorySelected(selectedMainCategory, selectedSubCategory);
            }
            dismiss();
        });
        
        buttonSelect.setOnClickListener(v -> {
            // Notify listener of selected category
            if (listener != null) {
                listener.onCategorySelected(selectedMainCategory, selectedSubCategory);
            }
            dismiss();
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
}

