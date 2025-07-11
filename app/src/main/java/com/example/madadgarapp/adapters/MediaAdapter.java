package com.example.madadgarapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.example.madadgarapp.R;
import com.example.madadgarapp.activities.FullScreenImageActivity;
import com.example.madadgarapp.utils.MediaUtils;

import java.util.List;

/**
 * Adapter for displaying images and videos in ViewPager2
 */
public class MediaAdapter extends RecyclerView.Adapter<MediaAdapter.MediaViewHolder> {
    
    public static class MediaItem {
        public final String url;
        public final boolean isVideo;
        
        public MediaItem(String url, boolean isVideo) {
            this.url = url;
            this.isVideo = isVideo;
        }
    }
    
    private final Context context;
    private final List<MediaItem> mediaItems;
    
    public MediaAdapter(Context context, List<MediaItem> mediaItems) {
        this.context = context;
        this.mediaItems = mediaItems;
    }
    
    @NonNull
    @Override
    public MediaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_media, parent, false);
        return new MediaViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MediaViewHolder holder, int position) {
        MediaItem item = mediaItems.get(position);
        holder.bind(item);
    }
    
    @Override
    public int getItemCount() {
        return mediaItems.size();
    }
    
    class MediaViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageMedia;
        private final FrameLayout videoContainer;
        private final ImageView videoThumbnail;
        private final ImageView videoPlayButton;
        
        public MediaViewHolder(@NonNull View itemView) {
            super(itemView);
            imageMedia = itemView.findViewById(R.id.image_media);
            videoContainer = itemView.findViewById(R.id.video_container);
            videoThumbnail = itemView.findViewById(R.id.video_thumbnail);
            videoPlayButton = itemView.findViewById(R.id.video_play_button);
        }
        
        public void bind(MediaItem item) {
            String processedUrl = MediaUtils.processMediaUrl(item.url);
            String mediaType = MediaUtils.getMediaTypeDescription(processedUrl);
            
            Log.d("MediaAdapter", "Binding media item: " + processedUrl + ", type: " + mediaType + ", isVideo: " + item.isVideo);
            
            if (item.isVideo) {
                // Show video container, hide image
                imageMedia.setVisibility(View.GONE);
                videoContainer.setVisibility(View.VISIBLE);
                
                // Load video thumbnail - try to load from video URL or use placeholder
                if (MediaUtils.isValidMediaUrl(processedUrl)) {
                    Log.d("MediaAdapter", "Loading video thumbnail from valid URL: " + processedUrl);
                    Glide.with(context)
                        .load(processedUrl) // Glide can extract frame from video URL
                        .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .transform(new RoundedCorners(8)))
                        .into(videoThumbnail);
                } else {
                    Log.w("MediaAdapter", "Invalid video URL, loading placeholder: " + processedUrl);
                    Glide.with(context)
                        .load(R.drawable.placeholder_image)
                        .into(videoThumbnail);
                }
                
                // Set click listener for video play
                videoPlayButton.setOnClickListener(v -> playVideo(processedUrl));
                videoContainer.setOnClickListener(v -> playVideo(processedUrl));
                
            } else {
                // Show image, hide video container
                imageMedia.setVisibility(View.VISIBLE);
                videoContainer.setVisibility(View.GONE);
                
                // Load image with improved error handling
                if (MediaUtils.isValidMediaUrl(processedUrl)) {
                    Log.d("MediaAdapter", "Loading image from valid URL: " + processedUrl);
                    Glide.with(context)
                        .load(processedUrl)
                        .apply(new RequestOptions()
                            .placeholder(R.drawable.placeholder_image)
                            .error(R.drawable.placeholder_image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .transform(new RoundedCorners(8)))
                        .into(imageMedia);
                } else {
                    Log.w("MediaAdapter", "Invalid image URL, loading placeholder: " + processedUrl);
                    Glide.with(context)
                        .load(R.drawable.placeholder_image)
                        .into(imageMedia);
                }
                
                // Set click listener for full-screen image view
                imageMedia.setOnClickListener(v -> viewFullscreenImage(processedUrl));
            }
        }
        
        private void playVideo(String videoUrl) {
            if (videoUrl == null || videoUrl.isEmpty()) {
                Toast.makeText(context, "Video URL is not available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                Log.d("MediaAdapter", "Attempting to play video: " + videoUrl);
                
                // Try multiple approaches to play video
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(videoUrl), "video/*");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                
                // Check if there's an app that can handle video
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    // Fallback: try to open in browser
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl));
                    browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(browserIntent);
                }
            } catch (Exception e) {
                Log.e("MediaAdapter", "Error playing video", e);
                Toast.makeText(context, "Unable to play video. URL: " + videoUrl, Toast.LENGTH_LONG).show();
            }
        }
        
        private void viewFullscreenImage(String imageUrl) {
            if (imageUrl == null || imageUrl.isEmpty()) {
                Toast.makeText(context, "Image URL is not available", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                Log.d("MediaAdapter", "Opening full-screen image viewer: " + imageUrl);
                
                // Use our custom FullScreenImageActivity
                Intent intent = new Intent(context, FullScreenImageActivity.class);
                intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_URL, imageUrl);
                intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_TITLE, "Image");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                
            } catch (Exception e) {
                Log.e("MediaAdapter", "Error opening full-screen image", e);
                
                // Fallback: try to open in external app
                try {
                    Intent fallbackIntent = new Intent(Intent.ACTION_VIEW);
                    fallbackIntent.setDataAndType(Uri.parse(imageUrl), "image/*");
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    
                    if (fallbackIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(fallbackIntent);
                    } else {
                        // Last resort: open in browser
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl));
                        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(browserIntent);
                    }
                } catch (Exception fallbackException) {
                    Log.e("MediaAdapter", "All fallback methods failed", fallbackException);
                    Toast.makeText(context, "Unable to view image. URL: " + imageUrl, Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}
