package com.unifiedproductivity.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest

/** Creates the reminders notification channel and posts due-reminder notifications. */
object NotificationHelper {

    const val CHANNEL_ID = "reminders_due"
    private const val CHANNEL_NAME = "Reminder due dates"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a reminder is due"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    /** Post a notification for a due reminder. No-op if the user hasn't granted POST_NOTIFICATIONS. */
    fun showReminder(context: Context, reminderId: String, title: String, body: String?) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title.ifBlank { "Reminder due" })
            .apply { if (!body.isNullOrBlank()) setContentText(body) }
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(reminderId.hashCode(), notification)
    }
}
