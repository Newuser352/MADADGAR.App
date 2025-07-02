package com.example.madadgarapp.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.madadgarapp.AuthSelectionActivity;
import com.example.madadgarapp.R;
import com.example.madadgarapp.utils.AuthManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Fragment to display the user account page with options:
 * - RATE US
 * - JOIN COMMUNITY
 * - SHARE APP
 * - REPORT PROBLEM
 * - LOGOUT
 */
@AndroidEntryPoint
public class AccountFragment extends Fragment {

    // UI Elements
    private TextView textUserName;
    private MaterialButton btnLogout;
    private LinearLayout layoutRateUs, layoutJoinCommunity, layoutShareApp, layoutReportProblem;
    private MaterialCardView cardProfile, cardOptions;

    public AccountFragment() {
        // Required empty public constructor
    }

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
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

    /**
     * Initialize all view references
     */
    private void initViews(View view) {
        // Profile section
        textUserName = view.findViewById(R.id.text_user_name);
        
        // Cards
        cardProfile = view.findViewById(R.id.card_profile);
        cardOptions = view.findViewById(R.id.card_options);
        
        // Option layouts
        layoutRateUs = view.findViewById(R.id.layout_rate_us);
        layoutJoinCommunity = view.findViewById(R.id.layout_join_community);
        layoutShareApp = view.findViewById(R.id.layout_share_app);
        layoutReportProblem = view.findViewById(R.id.layout_report_problem);
        
        // Logout button
        btnLogout = view.findViewById(R.id.btn_logout);
    }

    /**
     * Set up click listeners for all interactive elements
     */
    private void setupListeners() {
        // Options click listeners
        layoutRateUs.setOnClickListener(v -> onRateUsClicked());
        layoutJoinCommunity.setOnClickListener(v -> onJoinCommunityClicked());
        layoutShareApp.setOnClickListener(v -> onShareAppClicked());
        layoutReportProblem.setOnClickListener(v -> onReportProblemClicked());
        
        // Logout button
        btnLogout.setOnClickListener(v -> onLogoutClicked());
    }

    /**
     * Load user profile data from fragment arguments
     */
    private void loadUserProfile() {
        // Default value if no username is provided
        String username = "User";
        
        // Get username from arguments if available
        Bundle args = getArguments();
        if (args != null && args.containsKey("username")) {
            String argUsername = args.getString("username");
            if (argUsername != null && !argUsername.isEmpty()) {
                username = argUsername;
            }
        }
        
        // Display username
        textUserName.setText(username);
    }

    /**
     * Handle RATE US option click
     * Opens the app's page in Google Play Store
     */
    private void onRateUsClicked() {
        try {
            // Open Play Store app
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("market://details?id=" + requireActivity().getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // If Play Store app is not installed, open web browser
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://play.google.com/store/apps/details?id=" + 
                    requireActivity().getPackageName()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open app store", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle JOIN COMMUNITY option click
     * Opens a link to join the community platform
     */
    private void onJoinCommunityClicked() {
        try {
            // Open community URL (could be a social media group, Discord, etc.)
            String communityUrl = "https://example.com/madadgar-community";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(communityUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open community link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle SHARE APP option click
     * Creates a share intent for the app
     */
    private void onShareAppClicked() {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            
            // App sharing information
            String appName = getString(R.string.app_name);
            String shareMessage = "Check out " + appName + " app! It helps connect people in need. " +
                    "Download it from: https://play.google.com/store/apps/details?id=" + 
                    requireActivity().getPackageName();
            
            // Set share intent data
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, appName);
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            
            // Show share chooser
            startActivity(Intent.createChooser(shareIntent, "Share " + appName + " via"));
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not share app", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle REPORT PROBLEM option click
     * Opens email client to report a problem
     */
    private void onReportProblemClicked() {
        try {
            // Email details
            String[] recipients = {"support@madadgarapp.com"};
            String subject = getString(R.string.app_name) + " - Problem Report";
            String body = "Device Information:\n" +
                          "Android Version: " + android.os.Build.VERSION.RELEASE + "\n" +
                          "Device Model: " + android.os.Build.MODEL + "\n\n" +
                          "Please describe the problem you encountered:";
            
            // Create email intent
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:"));
            emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
            emailIntent.putExtra(Intent.EXTRA_TEXT, body);
            
            // Verify that the intent will resolve to an activity
            if (emailIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(emailIntent);
            } else {
                Toast.makeText(getContext(), "No email app installed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open email client", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle logout button click
     * Shows confirmation dialog before logging out
     */
    private void onLogoutClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Logout")
               .setMessage("Are you sure you want to logout?")
               .setPositiveButton("Yes", (dialog, which) -> logout())
               .setNegativeButton("No", null)
               .show();
    }

    /**
     * Perform logout operation
     * Clear user session - MainActivity will handle navigation via state observation
     */
    private void logout() {
        try {
            // Obtain AuthManager via ViewModelProvider
            AuthManager authManager = new ViewModelProvider(this).get(AuthManager.class);
            
            // Sign out from the authentication system
            // MainActivity observes auth state and will handle navigation
            authManager.signOut();
            
            // Show success message
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error during logout: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }
}
