package com.simats.travelbuddy

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object AlertHelper {
    private const val CHANNEL_ID = "travelbuddy_alerts_channel"
    private const val CHANNEL_NAME = "TravelBuddy Notifications"
    private const val CHANNEL_DESC = "Notifications and Alerts for TravelBuddy updates"
    private var notificationIdCounter = 100

    fun showNotification(context: Context, title: String, message: String, type: String) {
        // 1. Save Alert locally in SQLite DB under the active user's email
        try {
            val sharedPreferences = context.getSharedPreferences("TravelBuddyPrefs", Context.MODE_PRIVATE)
            val activeEmail = sharedPreferences.getString("ACTIVE_EMAIL", "guest@travelbuddy.com") ?: "guest@travelbuddy.com"
            
            val dbHelper = AlertDbHelper(context)
            dbHelper.insertAlert(activeEmail, title, message, type)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Trigger System Bar Notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create Channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESC
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Target activity for when user clicks on notification
        val intent = Intent(context, AlertsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            pendingIntentFlags
        )

        // Build notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback standard system drawable icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        // Post notification
        try {
            notificationManager.notify(notificationIdCounter++, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
