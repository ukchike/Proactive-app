package com.unifiedproductivity.app.ui.common

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.view.ContextThemeWrapper
import java.util.Calendar

/**
 * Native date/time picker dialogs. Both start from [initialMillis] and hand back a
 * full epoch-millis timestamp with the picked fields applied (date pickers keep the
 * original time-of-day; time pickers keep the original date).
 *
 * [isDark] must reflect the app's *currently rendered* theme (not just the system
 * setting) — these are plain framework dialogs that otherwise inherit the Activity's
 * static (light) theme and ignore the app's own dark-mode switch.
 */
fun pickDate(context: Context, initialMillis: Long, isDark: Boolean, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    DatePickerDialog(
        themedContext(context, isDark),
        { _, year, month, day ->
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            onPicked(cal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

fun pickTime(context: Context, initialMillis: Long, isDark: Boolean, onPicked: (Long) -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    TimePickerDialog(
        themedContext(context, isDark),
        { _, hour, minute ->
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            onPicked(cal.timeInMillis)
        },
        cal.get(Calendar.HOUR_OF_DAY),
        cal.get(Calendar.MINUTE),
        false
    ).show()
}

private fun themedContext(context: Context, isDark: Boolean): Context = ContextThemeWrapper(
    context,
    if (isDark) android.R.style.Theme_Material_Dialog_Alert
    else android.R.style.Theme_Material_Light_Dialog_Alert
)
