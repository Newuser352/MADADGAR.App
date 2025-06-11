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
        // TODO: Implement actual data fetching
        // For now, just keep showing empty state
        showEmptyState(true);
    }
}
