package com.example.madadgarapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.madadgarapp.R;
import com.example.madadgarapp.models.UserNotification;
import com.example.madadgarapp.utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(UserNotification notification);
        void onDeleteNotificationClick(UserNotification notification);
    }

    private final List<UserNotification> notifications = new ArrayList<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<UserNotification> list) {
        notifications.clear();
        if (list != null) notifications.addAll(list);
        notifyDataSetChanged();
    }

    public void removeNotification(UserNotification notification) {
        int position = notifications.indexOf(notification);
        if (position != -1) {
            notifications.remove(position);
            notifyItemRemoved(position);
        }
    }
    

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserNotification n = notifications.get(position);
        holder.bind(n, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textBody, textTime;
        MaterialButton btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textBody = itemView.findViewById(R.id.text_body);
            textTime = itemView.findViewById(R.id.text_time);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }

        void bind(UserNotification n, OnNotificationClickListener listener) {
            // Simple mapping: type as title, body as body
            textTitle.setText(n.getTitle());
            textBody.setText(n.getBody());
            textTime.setText(TimeUtils.getRelativeTimeString(TimeUtils.parseTimestamp(n.getCreatedAt())));

            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(n);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteNotificationClick(n);
            });
        }
    }
}
