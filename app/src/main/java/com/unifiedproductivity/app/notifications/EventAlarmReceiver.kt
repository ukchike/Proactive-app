package com.unifiedproductivity.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unifiedproductivity.app.UnifiedProductivityApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Fired by AlarmManager for a calendar event alert. Posts the notification, then —
 * for recurring events — reschedules the alarm for the *next* occurrence, since only
 * the first occurrence is a real database row (later ones are computed on the fly).
 */
class EventAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: "Event starting"
        val body = intent.getStringExtra(EXTRA_BODY)
        NotificationHelper.showEvent(context, id, title, body)

        val app = context.applicationContext as? UnifiedProductivityApp ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val event = app.container.calendarRepository.getEvent(id)
                if (event != null) EventScheduler(context.applicationContext).schedule(event)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ID = "event_id"
        const val EXTRA_TITLE = "event_title"
        const val EXTRA_BODY = "event_body"
    }
}
