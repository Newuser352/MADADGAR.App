package com.example.madadgarapp.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.madadgarapp.R
import com.example.madadgarapp.adapters.NotificationAdapter
import com.example.madadgarapp.models.UserNotification
import com.example.madadgarapp.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationsFragment : Fragment() {

    companion object {
        private const val TAG = "NotificationsFragment"
    }

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var textEmpty: TextView
    private lateinit var adapter: NotificationAdapter
    
    @Inject
    lateinit var repo: NotificationRepository
    
    private val notificationListener = object : NotificationAdapter.OnNotificationClickListener {
        override fun onNotificationClick(notification: UserNotification) {
            onNotificationClicked(notification)
        }
        
        override fun onDeleteNotificationClick(notification: UserNotification) {
            handleDeleteNotification(notification)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        swipeRefresh = view.findViewById(R.id.swipe_refresh)
        textEmpty = view.findViewById(R.id.text_empty)
        val recycler: RecyclerView = view.findViewById(R.id.recycler_notifications)

        adapter = NotificationAdapter(notificationListener)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        swipeRefresh.setOnRefreshListener { loadData() }


        loadData()
    }

    private fun loadData() {
        swipeRefresh.isRefreshing = true
        val userId = com.example.madadgarapp.utils.SupabaseClient.AuthHelper.getCurrentUser()?.id
        if (userId == null) {
            android.util.Log.e("NotificationsFragment", "loadData: No user ID found")
            showEmpty()
            return
        }
        android.util.Log.d("NotificationsFragment", "loadData: Loading notifications for user: $userId")
        
        lifecycleScope.launch {
            try {
                // First, let's try to get all notifications for debugging
                val allResult = repo.getAllNotifications(userId)
                if (allResult.isSuccess) {
                    val allList = allResult.getOrNull() ?: emptyList()
                    android.util.Log.d("NotificationsFragment", "loadData: Found ${allList.size} total notifications")
                    allList.forEach { notification ->
                        android.util.Log.d("NotificationsFragment", "loadData: Notification - ID: ${notification.id}, Title: ${notification.title}, IsRead: ${notification.isRead}, UserId: ${notification.userId}")
                    }
                } else {
                    android.util.Log.e("NotificationsFragment", "loadData: Failed to get all notifications", allResult.exceptionOrNull())
                }
                
                // Now get unread notifications
                val result = repo.getUnread(userId)
                swipeRefresh.isRefreshing = false
                if (result.isSuccess) {
                    val list = result.getOrNull() ?: emptyList()
                    android.util.Log.d("NotificationsFragment", "loadData: Found ${list.size} unread notifications")
                    list.forEach { notification ->
                        android.util.Log.d("NotificationsFragment", "loadData: Unread - ID: ${notification.id}, Title: ${notification.title}")
                    }
                    adapter.setData(list)
                    textEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    
                    if (list.isEmpty()) {
                        android.util.Log.w("NotificationsFragment", "loadData: No unread notifications found, showing empty state")
                    } else {
                        android.util.Log.d("NotificationsFragment", "loadData: Found ${list.size} unread notifications")
                    }
                } else {
                    android.util.Log.e("NotificationsFragment", "loadData: Failed to get unread notifications", result.exceptionOrNull())
                    textEmpty.visibility = View.VISIBLE
                    android.widget.Toast.makeText(requireContext(), "Failed to load notifications: ${result.exceptionOrNull()?.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationsFragment", "loadData: Exception in loadData", e)
                swipeRefresh.isRefreshing = false
                textEmpty.visibility = View.VISIBLE
                android.widget.Toast.makeText(requireContext(), "Error loading notifications: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun onNotificationClicked(n: UserNotification) {
        // Mark as read & maybe navigate to item
        lifecycleScope.launch {
            n.id?.let { repo.markRead(it) }
        }
        // For now just collapse & refresh list
        loadData()
    }
    
    private fun handleDeleteNotification(notification: UserNotification) {
        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                deleteNotification(notification)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteNotification(notification: UserNotification) {
        val notificationId = notification.id
        if (notificationId == null) {
            Toast.makeText(requireContext(), "Cannot delete notification: Invalid ID", Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(requireContext(), "Deleting notification...", Toast.LENGTH_SHORT).show()
        
        lifecycleScope.launch {
            try {
                val result = repo.deleteNotification(notificationId)
                if (result.isSuccess) {
                    // Remove from adapter UI
                    adapter.removeNotification(notification)
                    Toast.makeText(requireContext(), "Notification deleted successfully", Toast.LENGTH_SHORT).show()
                    
                    // Check if list is empty and show empty state
                    if (adapter.itemCount == 0) {
                        textEmpty.visibility = android.view.View.VISIBLE
                    }
                } else {
                    Log.e(TAG, "Failed to delete notification", result.exceptionOrNull())
                    Toast.makeText(requireContext(), "Failed to delete notification: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting notification", e)
                Toast.makeText(requireContext(), "Error deleting notification: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    

    private fun showEmpty() {
        swipeRefresh.isRefreshing = false
        textEmpty.visibility = View.VISIBLE
    }
}
