package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

public class AccountFragment extends Fragment {

    // Profile views
    private TextView textUserName, textPhoneNumber;
    private MaterialButton btnEditProfile, btnLogout;

    // Settings views
    private LinearLayout layoutAccountSettings, layoutNotifications, layoutHelpSupport, layoutAbout;
    private MaterialCardView cardProfile, cardSettings;

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
        textPhoneNumber = view.findViewById(R.id.text_phone_number);
        btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        
        // Cards
        cardProfile = view.findViewById(R.id.card_profile);
        cardSettings = view.findViewById(R.id.card_settings);
        
        // Settings section
        layoutAccountSettings = view.findViewById(R.id.layout_account_settings);
        layoutNotifications = view.findViewById(R.id.layout_notifications);
        layoutHelpSupport = view.findViewById(R.id.layout_help_support);
        layoutAbout = view.findViewById(R.id.layout_about);
        
        // Logout button
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    private void setupListeners() {
        // Profile editing
        btnEditProfile.setOnClickListener(v -> onEditProfileClicked());
        
        // Settings options
        layoutAccountSettings.setOnClickListener(v -> onAccountSettingsClicked());
        layoutNotifications.setOnClickListener(v -> onNotificationsClicked());
        layoutHelpSupport.setOnClickListener(v -> onHelpSupportClicked());
        layoutAbout.setOnClickListener(v -> onAboutClicked());
        
        // Logout
        btnLogout.setOnClickListener(v -> onLogoutClicked());
    }

    /**
     * Load user profile data
     * In a real app, this would fetch from SharedPreferences, database, or server
     */
    private void loadUserProfile() {
        // Mock user data for demonstration
        String userName = "John Doe";
        String phoneNumber = "+1 234 567 8900";
        
        // Display the data
        textUserName.setText(userName);
        textPhoneNumber.setText(phoneNumber);
    }

    /**
     * Handle edit profile button click
     */
    private void onEditProfileClicked() {
        // In a real app, this would navigate to an edit profile screen
        Toast.makeText(getContext(), "Edit Profile clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle account settings option click
     */
    private void onAccountSettingsClicked() {
        Toast.makeText(getContext(), "Account Settings clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle notifications option click
     */
    private void onNotificationsClicked() {
        Toast.makeText(getContext(), "Notifications clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle help & support option click
     */
    private void onHelpSupportClicked() {
        Toast.makeText(getContext(), "Help & Support clicked", Toast.LENGTH_SHORT).show();
    }

    /**
     * Handle about option click
     */
    private void onAboutClicked() {
        Toast.makeText(getContext(), "About clicked", Toast.LENGTH_SHORT).show();
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
     * Clear user session and navigate to login screen
     */
    private void logout() {
        // In a real app, this would clear user session data
        // Navigate to login screen
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().finish();
        }
    }
}

