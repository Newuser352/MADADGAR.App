package com.example.madadgarapp.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.example.madadgarapp.R;
import com.example.madadgarapp.MainActivity;
import com.example.madadgarapp.ShareItemFragment;
import com.example.madadgarapp.repository.SupabaseItemBridge;
import com.example.madadgarapp.models.SupabaseItem;
import com.example.madadgarapp.models.Item;
import com.example.madadgarapp.adapters.ItemAdapter;

import java.util.ArrayList;
import java.util.List;

public class MyPostsFragment extends Fragment {
    private RecyclerView rvMyPosts;
    private ConstraintLayout layoutEmptyPosts;

    public static MyPostsFragment newInstance() {
        return new MyPostsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_my_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // Initialize views
        rvMyPosts = view.findViewById(R.id.recycler_my_posts);
        layoutEmptyPosts = view.findViewById(R.id.layout_empty_posts);

        // Set up RecyclerView
        rvMyPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Initially show empty state
        showEmptyState(true);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // Load user posts when fragment becomes visible
        loadUserPosts();
    }

    private void handleAddPost() {
        if (getActivity() != null) {
            // Navigate to share item screen
            getActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.share_item_container, ShareItemFragment.newInstance())
                .addToBackStack(null)
                .commit();
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvMyPosts.setVisibility(View.GONE);
            layoutEmptyPosts.setVisibility(View.VISIBLE);
        } else {
            rvMyPosts.setVisibility(View.VISIBLE);
            layoutEmptyPosts.setVisibility(View.GONE);
        }
    }

    /**
     * Call this method when a new post is added to refresh the list
     */
    public void refreshPosts() {
        loadUserPosts();
    }
    
    /**
     * Load the current user's posts from Supabase
     */
    private void loadUserPosts() {
        // Check if user is authenticated
        if (!com.example.madadgarapp.utils.SupabaseClient.AuthHelper.INSTANCE.isAuthenticated()) {
            Toast.makeText(getContext(), "Please sign in to view your posts", Toast.LENGTH_SHORT).show();
            showEmptyState(true);
            return;
        }
        
        // Get current user ID
        var currentUser = com.example.madadgarapp.utils.SupabaseClient.AuthHelper.INSTANCE.getCurrentUser();
        if (currentUser == null) {
            showEmptyState(true);
            return;
        }
        String userId = currentUser.getId();
        
        // Load user's posts from Supabase
        SupabaseItemBridge bridge = new SupabaseItemBridge();
        bridge.getUserItems(userId, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
            @Override
            public void onSuccess(List<SupabaseItem> supabaseItems) {
                // Convert SupabaseItems to Items for adapter
                List<Item> items = new ArrayList<>();
                for (SupabaseItem supabaseItem : supabaseItems) {
                    Item item = convertSupabaseItemToItem(supabaseItem);
                    items.add(item);
                }
                
                // Set up adapter if not already done
                if (rvMyPosts.getAdapter() == null) {
                    ItemAdapter adapter = new ItemAdapter(requireContext(), item -> {
                        // Handle item click - could navigate to edit/detail view
                        Toast.makeText(getContext(), "Item: " + item.getTitle(), Toast.LENGTH_SHORT).show();
                    });
                    rvMyPosts.setAdapter(adapter);
                }
                
                // Update adapter data
                ((ItemAdapter) rvMyPosts.getAdapter()).setItems(items);
                
                // Show appropriate state
                showEmptyState(items.isEmpty());
                
                if (items.isEmpty()) {
                    Toast.makeText(getContext(), "You haven't shared any items yet", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error loading your posts: " + error, Toast.LENGTH_SHORT).show();
                showEmptyState(true);
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
}
