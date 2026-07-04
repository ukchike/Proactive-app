package com.unifiedproductivity.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Creates the notification channels and posts due-reminder / event-alert notifications. */
object NotificationHelper {

    const val REMINDER_CHANNEL_ID = "reminders_due"
    const val EVENT_CHANNEL_ID = "events_due"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL_ID, "Reminder due dates", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts when a reminder is due"
            }
        )
        manager.createNotificationChannel(
            NotificationChannel(EVENT_CHANNEL_ID, "Calendar event alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Alerts before a calendar event starts"
            }
        )
    }

    /** Post a notification for a due reminder. No-op if the user hasn't granted POST_NOTIFICATIONS. */
    fun showReminder(context: Context, reminderId: String, title: String, body: String?) {
        show(
            context,
            notificationId = reminderId.hashCode(),
            channelId = REMINDER_CHANNEL_ID,
            title = title.ifBlank { "Reminder due" },
            body = body
        )
    }

    /** Post a notification for an upcoming calendar event. */
    fun showEvent(context: Context, eventId: String, title: String, body: String?) {
        show(
            context,
            notificationId = eventId.hashCode(),
            channelId = EVENT_CHANNEL_ID,
            title = title.ifBlank { "Event starting" },
            body = body
        )
    }

    private fun show(context: Context, notificationId: Int, channelId: String, title: String, body: String?) {
        ensureChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .apply { if (!body.isNullOrBlank()) setContentText(body) }
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
}
