package com.unifiedproductivity.app.util

import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Date/time helpers shared across modules. All timestamps are epoch millis. */
object DateTimeUtils {

    private val dayMonth = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val dayMonthYear = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFmt = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun startOfToday(): Long = startOfDay(System.currentTimeMillis())

    fun endOfToday(): Long = endOfDay(System.currentTimeMillis())

    fun startOfDay(millis: Long): Long = calendarAt(millis).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun endOfDay(millis: Long): Long = calendarAt(millis).apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis

    fun isSameDay(a: Long, b: Long): Boolean = startOfDay(a) == startOfDay(b)

    fun isOverdue(dueDate: Long?): Boolean = dueDate != null && dueDate < startOfToday()

    fun formatDate(millis: Long): String = dayMonthYear.format(Date(millis))

    fun formatDayMonth(millis: Long): String = dayMonth.format(Date(millis))

    fun formatTime(millis: Long): String = timeFmt.format(Date(millis))

    fun formatMonthYear(millis: Long): String = monthYear.format(Date(millis))

    /** Human friendly due-date label used in reminder rows. */
    fun formatDueLabel(dueDate: Long, hasTime: Boolean): String {
        val today = startOfToday()
        val dayStart = startOfDay(dueDate)
        val dayLabel = when (dayStart) {
            today -> "Today"
            today + DAY_MS -> "Tomorrow"
            today - DAY_MS -> "Yesterday"
            else -> formatDayMonth(dueDate)
        }
        return if (hasTime) "$dayLabel, ${formatTime(dueDate)}" else dayLabel
    }

    /** Next occurrence for a recurring reminder/event. */
    fun nextOccurrence(from: Long?, frequency: RecurrenceFrequency, interval: Int): Long? {
        if (from == null || frequency == RecurrenceFrequency.NONE) return from
        val cal = calendarAt(from)
        val step = if (interval <= 0) 1 else interval
        when (frequency) {
            RecurrenceFrequency.DAILY -> cal.add(Calendar.DAY_OF_YEAR, step)
            RecurrenceFrequency.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, step)
            RecurrenceFrequency.MONTHLY -> cal.add(Calendar.MONTH, step)
            RecurrenceFrequency.YEARLY -> cal.add(Calendar.YEAR, step)
            RecurrenceFrequency.NONE -> Unit
        }
        return cal.timeInMillis
    }

    fun startOfMonth(millis: Long): Long = calendarAt(millis).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    fun addMonths(millis: Long, months: Int): Long =
        calendarAt(millis).apply { add(Calendar.MONTH, months) }.timeInMillis

    /** Day-of-month (1..31) for the given timestamp. */
    fun dayOfMonth(millis: Long): Int = calendarAt(millis).get(Calendar.DAY_OF_MONTH)

    /** Days in the month containing [millis]. */
    fun daysInMonth(millis: Long): Int =
        calendarAt(millis).getActualMaximum(Calendar.DAY_OF_MONTH)

    /** Weekday index of the first day of the month, 0 = Sunday. */
    fun firstWeekdayOfMonth(millis: Long): Int = calendarAt(startOfMonth(millis)).let {
        it.get(Calendar.DAY_OF_WEEK) - 1
    }

    private fun calendarAt(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }

    const val DAY_MS: Long = 24L * 60 * 60 * 1000
}
