package com.example.madadgarapp.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying user's posts in My Posts fragment with delete functionality
 */
public class MyPostsAdapter extends ListAdapter<Item, MyPostsAdapter.MyPostViewHolder> {

    private final Context context;
    private final OnItemActionListener listener;
    private List<Item> allItems = new ArrayList<>();

    public interface OnItemActionListener {
        void onItemClick(Item item);
        void onItemDelete(Item item);
        void onItemEdit(Item item);
    }

    public MyPostsAdapter(Context context, OnItemActionListener listener) {
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
    public MyPostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_post_layout, parent, false);
        return new MyPostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyPostViewHolder holder, int position) {
        Item item = getItem(position);
        holder.bind(item, listener);
    }

    public void setItems(List<Item> items) {
        allItems = new ArrayList<>(items);
        submitList(new ArrayList<>(items));
    }

    public void removeItem(Item item) {
        List<Item> currentList = new ArrayList<>(getCurrentList());
        currentList.remove(item);
        allItems.remove(item);
        submitList(currentList);
    }

    static class MyPostViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageItem;
        private final TextView textItemTitle;
        private final TextView textItemCategory;
        private final TextView textItemLocation;
        private final TextView textItemDescription;
        private final TextView textItemTime;
        private final MaterialButton buttonDelete;
        private final MaterialButton buttonEdit;

        public MyPostViewHolder(@NonNull View itemView) {
            super(itemView);
            imageItem = itemView.findViewById(R.id.image_item);
            textItemTitle = itemView.findViewById(R.id.text_item_title);
            textItemCategory = itemView.findViewById(R.id.text_item_category);
            textItemLocation = itemView.findViewById(R.id.text_item_location);
            textItemDescription = itemView.findViewById(R.id.text_item_description);
            textItemTime = itemView.findViewById(R.id.text_item_time);
            buttonDelete = itemView.findViewById(R.id.button_delete);
            buttonEdit = itemView.findViewById(R.id.button_edit);
        }

        public void bind(final Item item, final OnItemActionListener listener) {
            textItemTitle.setText(item.getTitle());
            textItemCategory.setText(item.getFullCategory());
            textItemLocation.setText(item.getLocation());
            textItemDescription.setText(item.getDescription());
            
            // Set relative time using TimeUtils for better formatting
            CharSequence relativeTime = TimeUtils.getRelativeTimeString(item.getCreatedAt());
            textItemTime.setText(relativeTime);
            
            // Load image using Glide
            Glide.with(imageItem.getContext())
                    .load(item.getImageUrl())
                    .apply(new RequestOptions()
                            .transform(new RoundedCorners(16))
                            .placeholder(R.drawable.ic_launcher_foreground)
                            .error(R.drawable.ic_launcher_foreground))
                    .into(imageItem);
            
            // Set click listeners
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });

            // Delete button click listener with confirmation dialog
            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    showDeleteConfirmationDialog(v.getContext(), item, listener);
                }
            });

            // Edit button click listener
            buttonEdit.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemEdit(item);
                }
            });
        }

        private void showDeleteConfirmationDialog(Context context, Item item, OnItemActionListener listener) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Delete Item")
                    .setMessage("Are you sure you want to delete \"" + item.getTitle() + "\"? This action cannot be undone.")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        listener.onItemDelete(item);
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .setIcon(R.drawable.ic_delete_24)
                    .show();
        }
    }
}
