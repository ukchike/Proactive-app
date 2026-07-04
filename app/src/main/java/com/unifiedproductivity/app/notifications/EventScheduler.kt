package com.unifiedproductivity.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import com.unifiedproductivity.app.util.DateTimeUtils

/**
 * Schedules/cancels exact alarms for calendar events with an alert lead time set
 * ([Event.reminderMinutesBefore]). Recurring events only persist their first
 * occurrence, so this walks forward through occurrences to find the next one whose
 * alert hasn't already passed; [EventAlarmReceiver] re-arms the following occurrence
 * each time one fires, chaining the series indefinitely.
 */
class EventScheduler(private val context: Context) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(event: Event) {
        if (event.deletedAt != null) {
            cancel(event.id)
            return
        }
        val leadMinutes = event.reminderMinutesBefore.firstOrNull()?.toIntOrNull()
        if (leadMinutes == null) {
            cancel(event.id) // no alert configured for this event
            return
        }
        val triggerAt = nextAlarmTrigger(event, leadMinutes)
        if (triggerAt == null) {
            cancel(event.id)
            return
        }

        val pending = pendingIntent(event)
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

    fun cancel(eventId: String) {
        alarmManager.cancel(pendingIntent(eventId, title = "", body = null))
    }

    /**
     * Finds the next occurrence (starting from the event's stored start time) whose
     * alert time is still in the future, stepping forward through the recurrence
     * rule as needed. Bounded the same way as [com.unifiedproductivity.app.util.RecurrenceExpander].
     */
    private fun nextAlarmTrigger(event: Event, leadMinutes: Int): Long? {
        var occurrenceStart = event.startDateTime
        var iterations = 0
        val maxIterations = if (event.recurrence == RecurrenceFrequency.NONE) 1 else 500

        while (iterations < maxIterations) {
            val trigger = occurrenceStart - leadMinutes * 60_000L
            if (trigger > System.currentTimeMillis()) return trigger
            if (event.recurrence == RecurrenceFrequency.NONE) return null
            val next = DateTimeUtils.nextOccurrence(occurrenceStart, event.recurrence, event.recurrenceInterval)
            if (next == null || next <= occurrenceStart) return null
            occurrenceStart = next
            iterations++
        }
        return null
    }

    private fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) alarmManager.canScheduleExactAlarms() else true

    private fun pendingIntent(event: Event): PendingIntent =
        pendingIntent(event.id, event.title, event.location)

    private fun pendingIntent(eventId: String, title: String, body: String?): PendingIntent {
        val intent = Intent(context, EventAlarmReceiver::class.java).apply {
            // A stable action per id keeps PendingIntents matchable for cancellation.
            action = "com.unifiedproductivity.app.EVENT_ALERT_$eventId"
            putExtra(EventAlarmReceiver.EXTRA_ID, eventId)
            putExtra(EventAlarmReceiver.EXTRA_TITLE, title)
            putExtra(EventAlarmReceiver.EXTRA_BODY, body)
        }
        return PendingIntent.getBroadcast(
            context,
            eventId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
