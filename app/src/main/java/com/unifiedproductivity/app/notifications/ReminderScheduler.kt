package com.unifiedproductivity.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.unifiedproductivity.app.data.entity.Reminder

/**
 * Schedules/cancels exact alarms for reminders with a due date. On Android 12+ we
 * fall back to an inexact alarm when the user hasn't granted the exact-alarm right,
 * so scheduling never crashes.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        // A completed or dateless reminder should have no pending alarm.
        val due = reminder.dueDate
        if (due == null || reminder.isCompleted || reminder.deletedAt != null) {
            cancel(reminder.id)
            return
        }

        // Fire earlier if the user asked for a lead time (first entry wins for the MVP).
        val leadMinutes = reminder.notifyMinutesBefore.firstOrNull()?.toIntOrNull() ?: 0
        val triggerAt = due - leadMinutes * 60_000L
        if (triggerAt <= System.currentTimeMillis()) return // already past — nothing to fire

        val pending = pendingIntent(reminder)
        try {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            // Exact-alarm permission revoked between the check and the call — degrade gracefully.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(reminderId: String) {
        alarmManager.cancel(pendingIntent(reminderId, title = "", body = null))
    }

    private fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun pendingIntent(reminder: Reminder): PendingIntent =
        pendingIntent(reminder.id, reminder.title, reminder.description)

    private fun pendingIntent(reminderId: String, title: String, body: String?): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            // A stable action per id keeps PendingIntents matchable for cancellation.
            action = "com.unifiedproductivity.app.REMIND_$reminderId"
            putExtra(ReminderAlarmReceiver.EXTRA_ID, reminderId)
            putExtra(ReminderAlarmReceiver.EXTRA_TITLE, title)
            putExtra(ReminderAlarmReceiver.EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
