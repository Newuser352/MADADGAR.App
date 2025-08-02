package com.example.madadgarapp.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.appbar.MaterialToolbar;

import com.example.madadgarapp.R;
import com.example.madadgarapp.activities.ItemDetailActivity;
import com.example.madadgarapp.adapters.ItemAdapter;
import com.example.madadgarapp.models.Item;
import com.example.madadgarapp.models.SupabaseItem;
import com.example.madadgarapp.repository.SupabaseItemBridge;
import com.example.madadgarapp.utils.FavoriteManager;
import com.example.madadgarapp.utils.TimeUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Fragment to display posts that the user has favourited (hearted).
 */
public class SavedPostsFragment extends Fragment {

    private RecyclerView rvSavedPosts;
    private MaterialToolbar toolbar;
    private ConstraintLayout layoutEmptyFavorites;
    private ItemAdapter itemAdapter;

    public static SavedPostsFragment newInstance() {
        return new SavedPostsFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        toolbar = view.findViewById(R.id.toolbar_saved_posts);
        rvSavedPosts = view.findViewById(R.id.recycler_saved_posts);
        layoutEmptyFavorites = view.findViewById(R.id.layout_empty_favorites);

        rvSavedPosts.setLayoutManager(new LinearLayoutManager(getContext()));
        itemAdapter = new ItemAdapter(requireContext(), item -> {
            Intent intent = new Intent(getContext(), ItemDetailActivity.class);
            intent.putExtra(ItemDetailActivity.EXTRA_ITEM, item);
            startActivity(intent);
        });
        rvSavedPosts.setAdapter(itemAdapter);

        // Set navigation click to go back
        toolbar.setNavigationOnClickListener(v -> requireActivity().getOnBackPressedDispatcher().onBackPressed());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadSavedPosts();
    }

    private void showEmptyState(boolean empty) {
        if (empty) {
            rvSavedPosts.setVisibility(View.GONE);
            layoutEmptyFavorites.setVisibility(View.VISIBLE);
        } else {
            rvSavedPosts.setVisibility(View.VISIBLE);
            layoutEmptyFavorites.setVisibility(View.GONE);
        }
    }

    private void loadSavedPosts() {
        // Get favourite IDs from shared prefs
        Set<String> favIds = FavoriteManager.getFavorites(requireContext());
        if (favIds == null) favIds = new HashSet<>();

        if (favIds.isEmpty()) {
            showEmptyState(true);
            return;
        }

        // Make favIds effectively final by creating a final reference
        final Set<String> finalFavIds = favIds;

        SupabaseItemBridge bridge = new SupabaseItemBridge();
        // Fetch a reasonable number of active items and filter locally
        bridge.getActiveItems(1000, 0, new SupabaseItemBridge.RepositoryCallback<List<SupabaseItem>>() {
            @Override
            public void onSuccess(List<SupabaseItem> supabaseItems) {
                List<Item> savedItems = new ArrayList<>();
                for (SupabaseItem supabaseItem : supabaseItems) {
                    if (finalFavIds.contains(supabaseItem.getId())) {
                        savedItems.add(convertSupabaseItemToItem(supabaseItem));
                    }
                }

                itemAdapter.setItems(savedItems);
                showEmptyState(savedItems.isEmpty());
                if (savedItems.isEmpty()) {
                    Toast.makeText(getContext(), "No saved posts found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to load saved posts: " + error, Toast.LENGTH_SHORT).show();
                showEmptyState(true);
            }
        });
    }

    private String determinePrimaryContact(SupabaseItem supabaseItem) {
        String primaryContact = supabaseItem.getContactNumber();
        if ((primaryContact == null || primaryContact.isEmpty()) && supabaseItem.getContact1() != null && !supabaseItem.getContact1().isEmpty()) {
            primaryContact = supabaseItem.getContact1();
        }
        if ((primaryContact == null || primaryContact.isEmpty()) && supabaseItem.getContact2() != null && !supabaseItem.getContact2().isEmpty()) {
            primaryContact = supabaseItem.getContact2();
        }
        return primaryContact;
    }

    private Item convertSupabaseItemToItem(SupabaseItem supabaseItem) {
        long createdAt = supabaseItem.getCreatedAt() != null ? TimeUtils.parseTimestamp(supabaseItem.getCreatedAt()) : System.currentTimeMillis();
        long expiresAt = supabaseItem.getExpiresAt() != null ? TimeUtils.parseTimestamp(supabaseItem.getExpiresAt()) : Long.MAX_VALUE;

        String coverImage = null;
        if (supabaseItem.getImageUrls() != null && !supabaseItem.getImageUrls().isEmpty()) {
            coverImage = supabaseItem.getImageUrls().get(0);
        }

        Item item = new Item(
                supabaseItem.getId(),
                supabaseItem.getTitle(),
                supabaseItem.getDescription(),
                supabaseItem.getMainCategory(),
                supabaseItem.getSubCategory(),
                supabaseItem.getLocation(),
                determinePrimaryContact(supabaseItem),
                supabaseItem.getOwnerEmail(),
                coverImage,
                supabaseItem.getOwnerId(),
                createdAt,
                expiresAt
        );
        item.setImageUrls(supabaseItem.getImageUrls());
        item.setVideoUrl(supabaseItem.getVideoUrl());
        return item;
    }
}
