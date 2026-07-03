package com.unifiedproductivity.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.unifiedproductivity.app.UnifiedProductivityApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Re-arms alarms for upcoming reminders after a device reboot (alarms don't survive it). */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as? UnifiedProductivityApp ?: return
        val scheduler = ReminderScheduler(context.applicationContext)
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.container.remindersRepository.getUpcoming().forEach { scheduler.schedule(it) }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
