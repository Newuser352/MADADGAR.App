package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AccountFragment extends Fragment {

    // Profile views
    private TextView textUserName, textEmailAddress;
    private MaterialButton btnEditProfile, btnLogout;
    

    // Cards
    private MaterialCardView cardProfile;

    public AccountFragment() {
        // Required empty public constructor
    }

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        initViews(view);
        
        // Set up listeners
        setupListeners();
        
        // Load user profile data
        loadUserProfile();
    }

    private void initViews(View view) {
        
        
        // Profile section
        textUserName = view.findViewById(R.id.text_user_name);
        textEmailAddress = view.findViewById(R.id.text_email_address);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        
        // Cards
        cardProfile = view.findViewById(R.id.card_profile);
        
        // Logout button
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        // Profile editing
        btnEditProfile.setOnClickListener(v -> onEditProfileClicked());
        
        // Logout
        btnLogout.setOnClickListener(v -> onLogoutClicked());
    }

    /**
     * Load user profile data
     */
    private void loadUserProfile() {
        // Logic for loading user data
        textUserName.setText("Example User");
        textEmailAddress.setText("user@example.com");
    }

    /**
     * Handle edit profile button click
     */
    private void onEditProfileClicked() {
        // In a real app, this would navigate to an edit profile screen
        Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle logout button click
     */
    private void onLogoutClicked() {
        // Show confirmation dialog in a real app
        logout();
    }

    /**
     * Perform logout operation
     * Navigate to login screen
     */
    private void logout() {
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Navigate to login screen
        Intent intent = new Intent(getActivity(), AuthSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }
}

