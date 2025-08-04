package com.example.madadgarapp.fragments;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    private TextView textUserName, textEmailAddress;
    private MaterialButton btnLogout;
    private LinearLayout layoutRateUs, layoutJoinCommunity, layoutCustomerSupport, layoutShareApp, layoutReportProblem, layoutSavedPosts;
    private ImageView imageBackArrow;
    private com.google.android.material.switchmaterial.SwitchMaterial switchDarkMode;

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
        
        // Initialize switch state based on saved preference
        boolean isDark = com.example.madadgarapp.utils.ThemeUtils.isDarkModeEnabled(requireContext());
        switchDarkMode.setChecked(isDark);

        // Load user profile data
        loadUserProfile();
    }

    private void initViews(View view) {
        // Profile section
        textUserName = view.findViewById(R.id.text_user_name);
        
        // Option layouts
        layoutRateUs = view.findViewById(R.id.layout_rate_us);
        layoutJoinCommunity = view.findViewById(R.id.layout_join_community);
        layoutShareApp = view.findViewById(R.id.layout_share_app);
        layoutCustomerSupport = view.findViewById(R.id.layout_customer_support);
        layoutReportProblem = view.findViewById(R.id.layout_report_problem);
        layoutSavedPosts = view.findViewById(R.id.layout_saved_posts);

        // Logout button
        btnLogout = view.findViewById(R.id.btn_logout);

        // Back arrow
        imageBackArrow = view.findViewById(R.id.image_back_arrow);

        // Dark mode switch
        switchDarkMode = view.findViewById(R.id.switch_dark_mode);

        // User email
        textEmailAddress = view.findViewById(R.id.text_email_address);
    }

    /**
     * Set up click listeners for all interactive elements
     */
    private void setupListeners() {
        // Options click listeners
        layoutRateUs.setOnClickListener(v -> onRateUsClicked());
        layoutJoinCommunity.setOnClickListener(v -> onJoinCommunityClicked());
        layoutShareApp.setOnClickListener(v -> onShareAppClicked());
        layoutCustomerSupport.setOnClickListener(v -> onCustomerSupportClicked());
        layoutReportProblem.setOnClickListener(v -> onReportProblemClicked());
        layoutSavedPosts.setOnClickListener(v -> onSavedPostsClicked());
        
        // Back arrow
        imageBackArrow.setOnClickListener(v -> requireActivity().onBackPressed());
        
        // Dark mode toggle listener
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            com.example.madadgarapp.utils.ThemeUtils.setDarkModeEnabled(requireContext(), isChecked);
        });
        
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
        
        // Fetch current user email dynamically
        String email = null;
        try {
            com.example.madadgarapp.utils.AuthManager authManager = new ViewModelProvider(requireActivity()).get(com.example.madadgarapp.utils.AuthManager.class);
            com.example.madadgarapp.utils.AuthManager.UserInfo user = (com.example.madadgarapp.utils.AuthManager.UserInfo) authManager.getCurrentUser().getValue();
            if (user != null) {
                email = user.getEmail();
            }
        } catch (Exception e) {
            // Log but keep UI graceful
            android.util.Log.w("AccountFragment", "Could not fetch current user email", e);
        }
        if (email == null || email.isEmpty()) {
            email = "Unknown";
        }
        textEmailAddress.setText(email);
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
     * Opens WhatsApp community link
     */
    private void onJoinCommunityClicked() {
        try {
            // WhatsApp community URL
            String communityUrl = "https://chat.whatsapp.com/J3b8mS8ooGgGGcvaGpTBi2";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(communityUrl));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Could not open WhatsApp community link", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle CUSTOMER SUPPORT option click
     * Opens email client to contact customer support
     */
    private void onCustomerSupportClicked() {
        try {
            // Email details for customer support
            String[] recipients = {"madadgarappteam@gmail.com"};
            String subject = getString(R.string.app_name) + " - Customer Support Request";
            String body = "Hello Madadgar Support Team,\n\n" +
                          "I need assistance with:\n\n" +
                          "[Please describe your issue or question here]\n\n" +
                          "Device Information:\n" +
                          "Android Version: " + android.os.Build.VERSION.RELEASE + "\n" +
                          "Device Model: " + android.os.Build.MODEL + "\n\n" +
                          "Thank you for your help!";
            
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
    private void onSavedPostsClicked() {
        // Navigate to SavedPostsFragment
        if (getActivity() != null) {
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.main_fragment_container, com.example.madadgarapp.fragments.SavedPostsFragment.newInstance())
                    .addToBackStack(null)
                    .commit();
        }
    }

    private void onReportProblemClicked() {
        try {
            // Email details
            String[] recipients = {"madadgarappteam@gmail.com"};
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
     * Clear user session and navigate to login screen
     */
    private void logout() {
        try {
            // Obtain AuthManager via ViewModelProvider
            AuthManager authManager = new ViewModelProvider(requireActivity()).get(AuthManager.class);
            // Sign out asynchronously
            authManager.signOut();
            // Inform the user
            Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
            // NOTE: Navigation is handled by MainActivity's AuthState observer once sign-out completes.
        } catch (Exception e) {
            Toast.makeText(getContext(), "Error during logout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

}
