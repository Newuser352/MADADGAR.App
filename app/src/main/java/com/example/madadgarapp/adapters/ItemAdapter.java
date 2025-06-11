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

import com.example.madadgarapp.R;
import com.example.madadgarapp.models.Item;

import java.util.ArrayList;
import java.util.List;

public class ItemAdapter extends ListAdapter<Item, ItemAdapter.ItemViewHolder> {

    private final Context context;
    private final OnItemClickListener listener;
    private List<Item> allItems = new ArrayList<>();
    private String currentQuery = "";
    private String currentCategory = "";

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

    public void filterItems(String query, String category) {
        currentQuery = query != null ? query.toLowerCase() : "";
        currentCategory = category;
        filterItems();
    }

    private void filterItems() {
        List<Item> filteredList = new ArrayList<>();
        
        for (Item item : allItems) {
            if (item.matchesFilters(currentQuery, currentCategory)) {
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

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageItem;
        private final TextView textItemTitle;
        private final TextView textItemCategory;
        private final TextView textItemLocation;
        private final TextView textItemDescription;

        public ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            imageItem = itemView.findViewById(R.id.image_item);
            textItemTitle = itemView.findViewById(R.id.text_item_title);
            textItemCategory = itemView.findViewById(R.id.text_item_category);
            textItemLocation = itemView.findViewById(R.id.text_item_location);
            textItemDescription = itemView.findViewById(R.id.text_item_description);
        }

        public void bind(final Item item, final OnItemClickListener listener) {
            textItemTitle.setText(item.getTitle());
            textItemCategory.setText(item.getFullCategory());
            textItemLocation.setText(item.getLocation());
            textItemDescription.setText(item.getDescription());
            
            // TODO: Load image using a library like Glide or Picasso
            // Example: Glide.with(imageItem).load(item.getImageUrl()).into(imageItem);
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
        }
    }
}

