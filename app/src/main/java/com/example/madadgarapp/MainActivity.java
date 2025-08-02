package com.example.madadgarapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;
import android.widget.FrameLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.AppBarLayout;

import com.example.madadgarapp.fragments.ItemsFragment;
import com.example.madadgarapp.ShareItemFragment;
import com.example.madadgarapp.fragments.MyPostsFragment;
import com.example.madadgarapp.fragments.NotificationsFragment;
import com.example.madadgarapp.fragments.AccountFragment;
import com.example.madadgarapp.utils.AuthManager;

import android.app.Dialog;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.example.madadgarapp.utils.AuthManager;
import dagger.hilt.android.AndroidEntryPoint;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;


@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private static final int BACK_PRESS_INTERVAL = 2000; // 2 seconds
    private long backPressedTime;
    
    private Toolbar toolbar;
    private BottomNavigationView bottomNavigationView;
    private FrameLayout mainFragmentContainer;
    private FragmentManager fragmentManager;
    private ItemsFragment itemsFragment;
    private ShareItemFragment shareItemFragment;
    private MyPostsFragment myPostsFragment;
    private NotificationsFragment notificationsFragment;
    private AccountFragment accountFragment;
    
    // For categories dialog
    private String selectedMainCategory = "";
    private String selectedSubCategory = "";
    
    // Current fragment
    private Fragment activeFragment;
private static final String STATE_ACTIVE_FRAGMENT = "active_fragment";

        private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Apply saved theme (dark / light) before inflating layout
        com.example.madadgarapp.utils.ThemeUtils.applyTheme(this);
        setContentView(R.layout.activity_main);
        
        // Initialize views
        initViews();
        // Initialize AuthManager ViewModel
        authManager = new ViewModelProvider(this).get(AuthManager.class);

        // Refresh authentication status immediately
        authManager.refreshAuthStatus();
        
        // Setup toolbar
        setupToolbar();
        
        // Initialize fragments
        initFragments(savedInstanceState);
        
        // Setup bottom navigation
        setupBottomNavigation();

        // Handle back press
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // First, let the fragment manager handle back stack within the container
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                    return;
                }

                // If we're on any fragment other than items (handled via bottom nav), go back to items
                if (activeFragment != null && activeFragment != itemsFragment) {
                    switchFragment(itemsFragment, "items");
                    bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                    return;
                }
                
                // Standard double-back-to-exit behavior
                long currentTime = System.currentTimeMillis();
                if (backPressedTime + BACK_PRESS_INTERVAL > currentTime) {
                    // User pressed back button twice within the interval, exit the app
                    finish();
                } else {
                    // First back press, show toast and update time
                    Toast.makeText(MainActivity.this, "Press back again to exit", Toast.LENGTH_SHORT).show();
                    backPressedTime = currentTime;
                }
            }
        });
        
        // Observe authentication state
        authManager.getAuthLiveData().observe(this, new Observer<AuthManager.AuthState>() {
            @Override
            public void onChanged(AuthManager.AuthState authState) {
                if (authState instanceof AuthManager.AuthState.Unauthenticated) {
                    // Navigate to AuthSelectionActivity immediately
                    Intent intent = new Intent(MainActivity.this, AuthSelectionActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                }
            }
        });
        
        // Handle notification intent AFTER everything is set up
        // Use a small delay to ensure fragments are ready
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            handleNotificationIntent(getIntent());
        }, 100); // 100ms delay
    }

    private void initViews() {
        try {
            toolbar = findViewById(R.id.toolbar);
            bottomNavigationView = findViewById(R.id.bottom_navigation);
            mainFragmentContainer = findViewById(R.id.main_fragment_container);
            
            // Verify all essential views are found
            if (bottomNavigationView == null || mainFragmentContainer == null) {
                Toast.makeText(this, "Error: Essential UI components not found", Toast.LENGTH_LONG).show();
            }
            
            // Initialize containers
            if (mainFragmentContainer != null) {
                mainFragmentContainer.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error initializing views: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowHomeEnabled(false);
        }
        
        // Add AppBarLayout elevation listener for scroll behavior
        AppBarLayout appBarLayout = findViewById(R.id.appbar);
        if (appBarLayout != null) {
            appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
                @Override
                public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                    // When scrolled, increase elevation for shadow effect
                    if (verticalOffset == 0) {
                        // Fully expanded - reduce elevation
                        appBarLayout.setElevation(0f);
                    } else {
                        // Scrolled - increase elevation
                        appBarLayout.setElevation(8f);
                    }
                }
            });
        }
    }
    
    private void initFragments(Bundle savedInstanceState) {
        // Create new fragment instances if they don't exist
        if (itemsFragment == null) itemsFragment = ItemsFragment.newInstance();
        if (shareItemFragment == null) shareItemFragment = ShareItemFragment.newInstance();
        if (myPostsFragment == null) myPostsFragment = MyPostsFragment.newInstance();
        if (notificationsFragment == null) notificationsFragment = new NotificationsFragment();
        if (accountFragment == null) accountFragment = AccountFragment.newInstance();
        
        // Initialize the fragment manager
        fragmentManager = getSupportFragmentManager();
        
        // Set initial fragment
        if (savedInstanceState == null) {
            // First time initialization - show Items fragment by default
            activeFragment = itemsFragment;
            
            // Show the default fragment immediately
            try {
                getSupportFragmentManager().beginTransaction()
                    .add(R.id.main_fragment_container, itemsFragment, "items")
                    .commit();
                
                bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                updateToolbarTitle("items");
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Error initializing fragments: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            // Restore state - find active fragment
            String activeFragmentTag = savedInstanceState.getString(STATE_ACTIVE_FRAGMENT, "items");
            
            // Find all fragments from fragment manager
            FragmentManager fragmentManager = getSupportFragmentManager();
            itemsFragment = (ItemsFragment) fragmentManager.findFragmentByTag("items");
            shareItemFragment = (ShareItemFragment) fragmentManager.findFragmentByTag("share_item");
            myPostsFragment = (MyPostsFragment) fragmentManager.findFragmentByTag("my_posts");
            notificationsFragment = (NotificationsFragment) fragmentManager.findFragmentByTag("notifications");
            accountFragment = (AccountFragment) fragmentManager.findFragmentByTag("account");
            
            // If fragments weren't found (which shouldn't happen but just in case), create new instances
            if (itemsFragment == null) itemsFragment = ItemsFragment.newInstance();
            if (shareItemFragment == null) shareItemFragment = ShareItemFragment.newInstance();
            if (myPostsFragment == null) myPostsFragment = MyPostsFragment.newInstance();
            if (notificationsFragment == null) notificationsFragment = new NotificationsFragment();
            if (accountFragment == null) accountFragment = AccountFragment.newInstance();
            
            // Set active fragment
            switch (activeFragmentTag) {
                case "items":
                    activeFragment = itemsFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                    break;
                case "categories":
                    // Categories is now handled via dialog, stay on current fragment
                    activeFragment = itemsFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                    break;
                case "share_item":
                    activeFragment = shareItemFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_add);
                    break;
                case "my_posts":
                    activeFragment = myPostsFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_my_posts);
                    break;
                case "notifications":
                    activeFragment = notificationsFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_notifications);
                    break;
                case "account":
                    activeFragment = accountFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_account);
                    break;
                default:
                    activeFragment = itemsFragment;
                    bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                    break;
            }
        }
    }
    
    private void setupBottomNavigation() {
        if (bottomNavigationView == null) {
            Toast.makeText(this, "Error: Bottom navigation not initialized", Toast.LENGTH_LONG).show();
            return;
        }

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            // Before handling any navigation, ensure any previous transaction has completed
            if (fragmentManager == null || fragmentManager.isStateSaved()) {
                // If the state is saved, defer the navigation until after the state is restored
                return false;
            }
            
            if (itemId == R.id.navigation_items) {
                switchFragment(itemsFragment, "items");
                return true;
            } else if (itemId == R.id.navigation_add) {
                switchFragment(shareItemFragment, "share_item");
                return true;
            } else if (itemId == R.id.navigation_my_posts) {
                switchFragment(myPostsFragment, "my_posts");
                return true;
            } else if (itemId == R.id.navigation_notifications) {
                switchFragment(notificationsFragment, "notifications");
                return true;
            } else if (itemId == R.id.navigation_account) {
                switchFragment(accountFragment, "account");
                return true;
            }
            
            return false;
        });
        
        // Handle navigation item reselection
        bottomNavigationView.setOnItemReselectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_items) {
                // Show categories dialog when items tab is pressed again
                showCategoriesDialog();
            } else if (itemId == R.id.navigation_add) {
                // Reset or show add form when add tab is reselected
                if (shareItemFragment != null && shareItemFragment.isVisible()) {
                    Toast.makeText(this, "Add Item Form", Toast.LENGTH_SHORT).show();
                }
            } else if (itemId == R.id.navigation_my_posts) {
                // Refresh my posts when tab is reselected
                if (myPostsFragment != null && myPostsFragment.isVisible()) {
                    // You could trigger a refresh here if needed
                    Toast.makeText(this, "Refreshing My Posts", Toast.LENGTH_SHORT).show();
                }
            } else if (itemId == R.id.navigation_notifications) {
                // Refresh notifications when tab is reselected
                if (notificationsFragment != null && notificationsFragment.isVisible()) {
                    // You could trigger a refresh here if needed
                    Toast.makeText(this, "Refreshing Notifications", Toast.LENGTH_SHORT).show();
                }
            } else if (itemId == R.id.navigation_account) {
                // Refresh account when tab is reselected
                if (accountFragment != null && accountFragment.isVisible()) {
                    // You could trigger a refresh here if needed
                    Toast.makeText(this, "Refreshing Account", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
    
    
    private void switchFragment(Fragment fragment, String tag) {
        try {
            if (fragment == null) {
                Toast.makeText(this, "Error: Fragment not found", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get current index and target index for determining animation direction
            int currentIndex = getFragmentIndex(activeFragment);
            int newIndex = getFragmentIndex(fragment);
            
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction transaction = fragmentManager.beginTransaction();
            
            // Set custom animations based on navigation direction
            if (currentIndex < newIndex) {
                // Moving right in the navigation
                transaction.setCustomAnimations(
                    R.anim.slide_in_right, 
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.slide_out_right
                );
            } else if (currentIndex > newIndex) {
                // Moving left in the navigation
                transaction.setCustomAnimations(
                    R.anim.slide_in_left, 
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.slide_out_left
                );
            } else {
                // Same fragment, use fade animations
                transaction.setCustomAnimations(
                    R.anim.fade_in, 
                    R.anim.fade_out,
                    R.anim.fade_in,
                    R.anim.fade_out
                );
            }
            
            // Hide current active fragment if exists
            if (activeFragment != null && activeFragment.isAdded()) {
                transaction.hide(activeFragment);
            }
            
            // If fragment is not added, add it first
            if (!fragment.isAdded()) {
                transaction.add(R.id.main_fragment_container, fragment, tag);
            } else {
                // If already added, just show it
                transaction.show(fragment);
            }
            
            // Update the active fragment
            activeFragment = fragment;
            updateToolbarTitle(tag);
            
            // Commit the transaction
            transaction.commitAllowingStateLoss();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error switching fragment: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void showShareItemForm() {
        try {
            // Get or create the ShareItemFragment
            if (shareItemFragment == null) {
                shareItemFragment = ShareItemFragment.newInstance();
            }
            
            // Simply switch to the shareItemFragment using the main container
            switchFragment(shareItemFragment, "share_item");
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing share form: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    public void hideShareItemForm() {
        try {
            // Simply switch back to the items fragment
            if (itemsFragment != null) {
                try {
                    switchFragment(itemsFragment, "items");
                    bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error returning to items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error hiding share form: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateToolbarTitle(String fragmentTag) {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); // Clear the toolbar title
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh authentication status when app comes to foreground
        // This helps restore session if the app was backgrounded
        if (authManager != null) {
            authManager.refreshAuthStatus();
        }
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Handle new intents when app is already running (singleTop/singleTask)
        setIntent(intent);
        handleNotificationIntent(intent);
    }
    
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        
        // Save the active fragment state
        String activeFragmentTag;
        
        if (activeFragment == null) {
            // Default to items fragment if activeFragment is somehow null
            activeFragmentTag = "items";
        } else if (activeFragment == itemsFragment) {
            activeFragmentTag = "items";
        // Categories is now handled through dialog, not a fragment
        } else if (activeFragment == shareItemFragment) {
            activeFragmentTag = "share_item";
        } else if (activeFragment == myPostsFragment) {
            activeFragmentTag = "my_posts";
        } else if (activeFragment == notificationsFragment) {
            activeFragmentTag = "notifications";
        } else if (activeFragment == accountFragment) {
            activeFragmentTag = "account";
        } else {
            // Fallback to items fragment
            activeFragmentTag = "items";
        }
        
        outState.putString(STATE_ACTIVE_FRAGMENT, activeFragmentTag);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Don't inflate any menu to remove the three dots
        return false;
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        // Handle menu item clicks
        if (id == R.id.action_logout) {
            // Logout functionality
            logout();
            return true;
        } else if (id == R.id.action_profile) {
            // Make sure we're not in the middle of a fragment transaction
            if (!fragmentManager.isStateSaved()) {
                // Navigate to account fragment
                bottomNavigationView.setSelectedItemId(R.id.navigation_account);
            }
            return true;
        } else if (id == R.id.action_settings) {
            // Make sure we're not in the middle of a fragment transaction
            if (!fragmentManager.isStateSaved()) {
                // Settings functionality - can navigate to account fragment for now
                bottomNavigationView.setSelectedItemId(R.id.navigation_account);
            }
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Shows the categories dialog with Food and Non-Food options
     */
    private void showCategoriesDialog() {
        try {
            // Reset selection
            selectedMainCategory = "";
            selectedSubCategory = "";
            
            // Inflate the dialog view
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_categories, null);
            
            // Get references to views in the dialog
            LinearLayout layoutFoodSubcategories = dialogView.findViewById(R.id.layout_food_subcategories);
            LinearLayout layoutNonFoodSubcategories = dialogView.findViewById(R.id.layout_non_food_subcategories);
            ImageView imageFoodDropdown = dialogView.findViewById(R.id.image_food_dropdown);
            ImageView imageNonFoodDropdown = dialogView.findViewById(R.id.image_non_food_dropdown);
            
            // Create and show the dialog with animation
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                    .setView(dialogView)
                    .setCancelable(true);
                    
            Dialog dialog = builder.create();
            dialog.getWindow().getAttributes().windowAnimations = R.style.Animation_App_Dialog;
            
            // Set button tags to reference the dialog
            dialogView.findViewById(R.id.button_cancel).setTag(dialog);
            dialogView.findViewById(R.id.button_select).setTag(dialog);
            
            // Food category header click listener
            dialogView.findViewById(R.id.layout_food_header).setOnClickListener(v -> {
                // Toggle food subcategories visibility
                boolean isVisible = layoutFoodSubcategories.getVisibility() == View.VISIBLE;
                layoutFoodSubcategories.setVisibility(isVisible ? View.GONE : View.VISIBLE);
                imageFoodDropdown.setRotation(isVisible ? 0 : 180);
                
                // Hide non-food subcategories if they're visible
                layoutNonFoodSubcategories.setVisibility(View.GONE);
                imageNonFoodDropdown.setRotation(0);
                
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
                
                selectedMainCategory = "Non-Food";
            });
            
            // Food subcategories click listeners
            dialogView.findViewById(R.id.layout_cooked_food).setOnClickListener(v -> {
                selectedSubCategory = "Cooked Food";
                highlightSelectedOption(layoutFoodSubcategories, v);
            });
            
            dialogView.findViewById(R.id.layout_uncooked_food).setOnClickListener(v -> {
                selectedSubCategory = "Uncooked Food";
                highlightSelectedOption(layoutFoodSubcategories, v);
            });
            
            // Non-Food subcategories click listeners
            dialogView.findViewById(R.id.layout_electronics).setOnClickListener(v -> {
                selectedSubCategory = "Electronics";
                highlightSelectedOption(layoutNonFoodSubcategories, v);
            });
            
            dialogView.findViewById(R.id.layout_furniture).setOnClickListener(v -> {
                selectedSubCategory = "Furniture";
                highlightSelectedOption(layoutNonFoodSubcategories, v);
            });
            
            dialogView.findViewById(R.id.layout_books).setOnClickListener(v -> {
                selectedSubCategory = "Books";
                highlightSelectedOption(layoutNonFoodSubcategories, v);
            });
            
            dialogView.findViewById(R.id.layout_clothing).setOnClickListener(v -> {
                selectedSubCategory = "Clothing";
                highlightSelectedOption(layoutNonFoodSubcategories, v);
            });
            
            dialogView.findViewById(R.id.layout_other).setOnClickListener(v -> {
                selectedSubCategory = "Other";
                highlightSelectedOption(layoutNonFoodSubcategories, v);
            });
            
            // Set up button click listeners
            dialogView.findViewById(R.id.button_cancel).setOnClickListener(v -> {
                // Reset selection if canceled
                selectedMainCategory = "";
                selectedSubCategory = "";
                
                // Always navigate back to items tab
                if (bottomNavigationView != null) {
                    bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                }
                
                dialog.dismiss();
            });
            
            dialogView.findViewById(R.id.button_select).setOnClickListener(v -> {
                if (!selectedMainCategory.isEmpty() && !selectedSubCategory.isEmpty()) {
                    String fullCategory = selectedMainCategory + " > " + selectedSubCategory;
                    Toast.makeText(this, "Selected: " + fullCategory, Toast.LENGTH_SHORT).show();
                    
                    // Here you can implement actions based on the selected category
                    // For example, filter items based on the selected category
                    
                    // Navigate to items tab after selection to show filtered items
                    if (bottomNavigationView != null) {
                        bottomNavigationView.setSelectedItemId(R.id.navigation_items);
                    }
                } else {
                    Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
                }
                
                dialog.dismiss();
            });
            
            dialog.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error showing categories dialog: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            
            // Navigate back to items tab in case of error
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_items);
            }
        }
    }
    
    /**
     * Helper method to visually highlight the selected option
     */
    private void highlightSelectedOption(ViewGroup parentLayout, View selectedView) {
        // Reset all backgrounds to default
        for (int i = 0; i < parentLayout.getChildCount(); i++) {
            View child = parentLayout.getChildAt(i);
            if (child instanceof LinearLayout) {
                TypedValue outValue = new TypedValue();
                getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                child.setBackgroundResource(outValue.resourceId);
            }
        }
        
        // Highlight selected view with a slight background tint
        selectedView.setBackgroundColor(getResources().getColor(R.color.primary_color, null));
    }

    /**
     * Helper method to determine the index of a fragment for animation direction
     * @param fragment The fragment to find the index for
     * @return The index of the fragment in the navigation order
     */
    private int getFragmentIndex(Fragment fragment) {
        if (fragment == itemsFragment) {
            return 0;
        } else if (fragment == shareItemFragment) {
            return 1;
        } else if (fragment == myPostsFragment) {
            return 2;
        } else if (fragment == notificationsFragment) {
            return 3;
        } else if (fragment == accountFragment) {
            return 4;
        }
        return 0; // Default to first position
    }

    /**
     * Public method to refresh the items list in ItemsFragment
     * Called when a new item is shared to update the dashboard
     */
    public void refreshItemsList() {
        try {
            if (itemsFragment != null && itemsFragment.isAdded()) {
                itemsFragment.refreshItems();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error refreshing items: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        try {
            // Use the AuthManager to sign out
            authManager.signOut();
            Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
            // Navigation will be handled automatically by the auth state observer
        } catch (Exception e) {
            Toast.makeText(this, "Error during logout: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Handle notification intent to open specific item or perform action
     */
    private void handleNotificationIntent(Intent intent) {
        android.util.Log.d("MainActivity", "handleNotificationIntent called");
        
        if (intent == null) {
            android.util.Log.d("MainActivity", "Intent is null");
            return;
        }
        
        // Log all extras for debugging
        android.os.Bundle extras = intent.getExtras();
        if (extras != null) {
            android.util.Log.d("MainActivity", "Intent extras found:");
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                android.util.Log.d("MainActivity", "  " + key + " = " + value);
            }
        } else {
            android.util.Log.d("MainActivity", "No intent extras found");
        }
        
        try {
            boolean openedFromNotification = intent.getBooleanExtra("opened_from_notification", false);
            android.util.Log.d("MainActivity", "openedFromNotification: " + openedFromNotification);
            
            if (!openedFromNotification) {
                android.util.Log.d("MainActivity", "Not opened from notification, returning");
                return;
            }
            
            String action = intent.getStringExtra("action");
            String notificationType = intent.getStringExtra("notification_type");
            String itemId = intent.getStringExtra("target_item_id");
            
            android.util.Log.d("MainActivity", "Handling notification intent:");
            android.util.Log.d("MainActivity", "  Action: " + action);
            android.util.Log.d("MainActivity", "  Type: " + notificationType);
            android.util.Log.d("MainActivity", "  Item ID: " + itemId);
            
            if ("open_item".equals(action)) {
                if (itemId != null && !itemId.isEmpty()) {
                    android.util.Log.d("MainActivity", "Opening item from notification: " + itemId);
                    openItemFromNotification(itemId);
                } else {
                    android.util.Log.w("MainActivity", "No item ID found in notification intent");
                    // Fallback: navigate to items tab
                    navigateToItemsTab();
                    Toast.makeText(this, "Opening app from notification", Toast.LENGTH_SHORT).show();
                }
            } else if ("new_listing".equals(notificationType)) {
                // If we can't open specific item, at least navigate to items tab
                android.util.Log.d("MainActivity", "New listing notification - navigating to items tab");
                navigateToItemsTab();
                Toast.makeText(this, "New item available!", Toast.LENGTH_SHORT).show();
            } else {
                android.util.Log.d("MainActivity", "Unhandled notification type, navigating to items tab");
                navigateToItemsTab();
                Toast.makeText(this, "Opening app from notification", Toast.LENGTH_SHORT).show();
            }
            
            // Clear the intent extras to prevent re-processing
            intent.removeExtra("opened_from_notification");
            intent.removeExtra("action");
            intent.removeExtra("target_item_id");
            intent.removeExtra("notification_type");
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error handling notification intent", e);
            // Fallback: navigate to items tab
            navigateToItemsTab();
            Toast.makeText(this, "Error opening from notification", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Navigate to items tab
     */
    private void navigateToItemsTab() {
        try {
            if (bottomNavigationView != null) {
                bottomNavigationView.setSelectedItemId(R.id.navigation_items);
            }
            if (itemsFragment != null) {
                switchFragment(itemsFragment, "items");
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error navigating to items tab", e);
        }
    }
    
    /**
     * Open specific item from notification
     */
    private void openItemFromNotification(String itemId) {
        try {
            android.util.Log.d("MainActivity", "Attempting to open item: " + itemId);
            
            // First, navigate to items tab to ensure we're on the right fragment
            navigateToItemsTab();
            
            // Give the fragment a moment to load, then try to open the item
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                try {
                    if (itemsFragment != null && itemsFragment.isAdded()) {
                        // Try to open the specific item
                        itemsFragment.openItemById(itemId);
                    } else {
                        android.util.Log.w("MainActivity", "ItemsFragment not ready to open item");
                        Toast.makeText(this, "Opening item from notification...", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error opening item after delay", e);
                    Toast.makeText(this, "Could not open item from notification", Toast.LENGTH_SHORT).show();
                }
            }, 500); // 500ms delay to ensure fragment is ready
            
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in openItemFromNotification", e);
            Toast.makeText(this, "Error opening item from notification: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
