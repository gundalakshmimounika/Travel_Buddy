package com.simats.travelbuddy

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlertsActivity : AppCompatActivity() {

    private lateinit var rvAlerts: RecyclerView
    private lateinit var llEmptyState: LinearLayout
    private lateinit var dbHelper: AlertDbHelper
    private val alertsList = mutableListOf<AlertItem>()
    private lateinit var adapter: AlertsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alerts)

        dbHelper = AlertDbHelper(this)

        // Adjust for Edge to Edge status bar margins
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rlToolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
            insets
        }

        findViewById<View>(R.id.ivBack).setOnClickListener { finish() }

        rvAlerts = findViewById(R.id.rvAlerts)
        llEmptyState = findViewById(R.id.llEmptyState)
        rvAlerts.layoutManager = LinearLayoutManager(this)

        adapter = AlertsAdapter(alertsList) { alert ->
            showDeleteAlertConfirmationDialog(alert)
        }
        rvAlerts.adapter = adapter

        findViewById<View>(R.id.tvClearAll).setOnClickListener {
            showClearAllConfirmationDialog()
        }

        loadAlerts()
    }

    private fun getActiveEmail(): String {
        val sharedPreferences = getSharedPreferences("TravelBuddyPrefs", MODE_PRIVATE)
        return sharedPreferences.getString("ACTIVE_EMAIL", "guest@travelbuddy.com") ?: "guest@travelbuddy.com"
    }

    private fun loadAlerts() {
        alertsList.clear()
        val savedAlerts = dbHelper.getAllAlerts(getActiveEmail())
        alertsList.addAll(savedAlerts)

        if (alertsList.isEmpty()) {
            rvAlerts.visibility = View.GONE
            llEmptyState.visibility = View.VISIBLE
        } else {
            rvAlerts.visibility = View.VISIBLE
            llEmptyState.visibility = View.GONE
            adapter.notifyDataSetChanged()
        }
    }

    private fun showDeleteAlertConfirmationDialog(alert: AlertItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Notification")
            .setMessage("Are you sure you want to delete this notification?")
            .setPositiveButton("Delete") { _, _ ->
                dbHelper.deleteAlert(alert.id)
                loadAlerts()
                Toast.makeText(this, "Notification deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showClearAllConfirmationDialog() {
        if (alertsList.isEmpty()) {
            Toast.makeText(this, "No alerts to clear", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear All Notifications")
            .setMessage("Are you sure you want to clear all notifications?")
            .setPositiveButton("Clear") { _, _ ->
                dbHelper.clearAllAlerts(getActiveEmail())
                loadAlerts()
                Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    class AlertsAdapter(
        private val items: List<AlertItem>,
        private val onDeleteClick: (AlertItem) -> Unit
    ) : RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val flIconBg: FrameLayout = view.findViewById(R.id.flIconBg)
            val ivAlertIcon: ImageView = view.findViewById(R.id.ivAlertIcon)
            val tvAlertTitle: TextView = view.findViewById(R.id.tvAlertTitle)
            val tvAlertTime: TextView = view.findViewById(R.id.tvAlertTime)
            val tvAlertMessage: TextView = view.findViewById(R.id.tvAlertMessage)
            val ivDeleteAlert: View = view.findViewById(R.id.ivDeleteAlert)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_alert, parent, false))

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.tvAlertTitle.text = item.title
            holder.tvAlertMessage.text = item.message
            holder.tvAlertTime.text = getFormattedTime(item.timestamp)
            holder.ivDeleteAlert.setOnClickListener { onDeleteClick(item) }

            // Setup dynamic style and colors based on alert type for premium visual experience
            val (iconRes, iconTintBg, iconTint) = when (item.type) {
                "trip" -> Triple(
                    R.drawable.ic_location,
                    "#10B981", // Green
                    "#FFFFFF"
                )
                "hotel_success" -> Triple(
                    android.R.drawable.checkbox_on_background, // Checkmark standard drawable
                    "#06B6D4", // Teal
                    "#FFFFFF"
                )
                "hotel_failure" -> Triple(
                    android.R.drawable.ic_delete, // Delete/Error icon standard drawable
                    "#EF4444", // Red
                    "#FFFFFF"
                )
                "weather" -> Triple(
                    R.drawable.ic_alerts, // Warning/Alert icon
                    "#F59E0B", // Yellow/Orange
                    "#FFFFFF"
                )
                "travel" -> Triple(
                    R.drawable.ic_search, // ticket/search icon
                    "#3B82F6", // Blue
                    "#FFFFFF"
                )
                "join" -> Triple(
                    R.drawable.ic_community, // Community icon
                    "#8B5CF6", // Purple
                    "#FFFFFF"
                )
                else -> Triple(
                    R.drawable.ic_alerts,
                    "#6B7280", // Gray
                    "#FFFFFF"
                )
            }

            try {
                holder.ivAlertIcon.setImageResource(iconRes)
                holder.flIconBg.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(iconTintBg)
                )
                holder.ivAlertIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor(iconTint)
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        override fun getItemCount() = items.size

        private fun getFormattedTime(epochMillis: Long): String {
            val diff = System.currentTimeMillis() - epochMillis
            if (diff < 0) return "Just now"
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                seconds < 60 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(epochMillis))
            }
        }
    }
}
