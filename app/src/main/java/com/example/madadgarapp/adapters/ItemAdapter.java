package com.example.madadgarapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.madadgarapp.R;
import com.example.madadgarapp.models.Item;
import com.example.madadgarapp.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends ListAdapter<Item, ItemAdapter.ItemViewHolder> {

    private final Context context;
    private final OnItemClickListener listener;
    private List<Item> allItems = new ArrayList<>();
    private String currentQuery = "";
    private String currentCategory = "";
    private com.example.madadgarapp.utils.LocationUtils.Coordinates currentLocation;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public ItemAdapter(Context context, OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.context = context;
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
                    return oldItem.getTitle().equals(newItem.getTitle()) &&
                           oldItem.getDescription().equals(newItem.getDescription()) &&
                           oldItem.getMainCategory().equals(newItem.getMainCategory()) &&
                           oldItem.getSubCategory().equals(newItem.getSubCategory()) &&
                           oldItem.getLocation().equals(newItem.getLocation());
                }
            };

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_layout, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = getItem(position);
        holder.bind(item, listener);
    }

    public void setItems(List<Item> items) {
        allItems = new ArrayList<>(items);
        filterItems();
    }

    public void setCurrentLocation(com.example.madadgarapp.utils.LocationUtils.Coordinates location) {
        this.currentLocation = location;
        filterItems();
    }

    public void filterItems(String query, String category) {
        currentQuery = query != null ? query.toLowerCase() : "";
        currentCategory = category;
        filterItems();
    }

    private void filterItems() {
        List<Item> filteredList = new ArrayList<>();
        
        double radiusKm = 0.5; // 500 m
        for (Item item : allItems) {
            boolean matchesText = item.matchesFilters(currentQuery, currentCategory);
            boolean withinRadius = true;
            if (currentLocation != null && item.getLatitude() != null && item.getLongitude() != null) {
                withinRadius = com.example.madadgarapp.utils.LocationUtils.isWithinRadius(
                        currentLocation,
                        new com.example.madadgarapp.utils.LocationUtils.Coordinates(item.getLatitude(), item.getLongitude()),
                        radiusKm);
            }
            if (matchesText && withinRadius) {
                filteredList.add(item);
            }
        }
        
        submitList(filteredList);
    }

    public int getFilteredItemCount() {
        return getCurrentList().size();
    }

    public boolean hasActiveFilters() {
        return !currentQuery.isEmpty() || !currentCategory.isEmpty();
    }
    
    public List<Item> getAllItems() {
        return new ArrayList<>(allItems);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageItem;
        private final TextView textItemTitle;
        private final TextView textItemCategory;
        private final TextView textItemLocation;
        private final TextView textItemDescription;
        private final TextView textItemTime;
        private final ImageView imageFavorite;
        private final TextView textItemBadge;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageItem = itemView.findViewById(R.id.image_item);
            textItemTitle = itemView.findViewById(R.id.text_item_title);
            textItemCategory = itemView.findViewById(R.id.text_item_category);
            textItemLocation = itemView.findViewById(R.id.text_item_location);
            textItemDescription = itemView.findViewById(R.id.text_item_description);
            textItemTime = itemView.findViewById(R.id.text_item_time);
            imageFavorite = itemView.findViewById(R.id.image_favorite);
            textItemBadge = itemView.findViewById(R.id.text_item_badge);
        }

public void bind(final Item item, final OnItemClickListener listener) {
            long now = System.currentTimeMillis();
            final long ONE_HOUR = 60 * 60 * 1000L;
            textItemTitle.setText(item.getTitle());
            textItemCategory.setText(item.getFullCategory());
            textItemLocation.setText(item.getLocation());
            textItemDescription.setText(item.getDescription());
            // Set relative time using TimeUtils for better formatting
            CharSequence relativeTime = TimeUtils.getRelativeTimeString(item.getCreatedAt());
            textItemTime.setText(relativeTime);
            
            // Favourite icon state
            boolean isFav = com.example.madadgarapp.utils.FavoriteManager.isFavorite(itemView.getContext(), item.getId());
            imageFavorite.setImageResource(isFav ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            imageFavorite.setOnClickListener(v -> {
                boolean newState = com.example.madadgarapp.utils.FavoriteManager.toggleFavorite(v.getContext(), item.getId());
                imageFavorite.setImageResource(newState ? R.drawable.ic_favorite : R.drawable.ic_favorite_border);
            });

            // Badge logic
            String badgeText = null;
            if (item.getCreatedAt() >= now - 24 * ONE_HOUR) {
                badgeText = "NEW";
            } else if (item.getExpiration() > 0 && item.getExpiration() - now <= 12 * ONE_HOUR) {
                badgeText = "EXPIRING";
            } else if (item.getViewCount() >= 100) { // threshold for popularity
                badgeText = "POPULAR";
            }
            if (badgeText != null) {
                textItemBadge.setText(badgeText);
                textItemBadge.setVisibility(View.VISIBLE);
            } else {
                textItemBadge.setVisibility(View.GONE);
            }

            // Load image using Glide
            Glide.with(imageItem.getContext())
                    .load(item.getImageUrl())
                    .apply(new RequestOptions().transform(new RoundedCorners(16)))
                    .into(imageItem);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}

