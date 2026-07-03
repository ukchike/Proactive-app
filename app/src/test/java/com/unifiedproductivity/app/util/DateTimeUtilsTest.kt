package com.unifiedproductivity.app.util

import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class DateTimeUtilsTest {

    private fun at(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    @Test
    fun `start and end of day bound the same day`() {
        val noon = at(2026, Calendar.JULY, 3)
        assertTrue(DateTimeUtils.startOfDay(noon) <= noon)
        assertTrue(DateTimeUtils.endOfDay(noon) >= noon)
        assertTrue(DateTimeUtils.isSameDay(DateTimeUtils.startOfDay(noon), DateTimeUtils.endOfDay(noon)))
    }

    @Test
    fun `daily recurrence advances by interval`() {
        val start = at(2026, Calendar.JULY, 3)
        val next = DateTimeUtils.nextOccurrence(start, RecurrenceFrequency.DAILY, 2)!!
        assertEquals(2, ((next - start) / DateTimeUtils.DAY_MS).toInt())
    }

    @Test
    fun `weekly recurrence advances seven days`() {
        val start = at(2026, Calendar.JULY, 3)
        val next = DateTimeUtils.nextOccurrence(start, RecurrenceFrequency.WEEKLY, 1)!!
        assertEquals(7, ((next - start) / DateTimeUtils.DAY_MS).toInt())
    }

    @Test
    fun `none recurrence returns same instant`() {
        val start = at(2026, Calendar.JULY, 3)
        assertEquals(start, DateTimeUtils.nextOccurrence(start, RecurrenceFrequency.NONE, 1))
    }

    @Test
    fun `july 2026 has 31 days and starts on wednesday`() {
        val july = at(2026, Calendar.JULY, 15)
        assertEquals(31, DateTimeUtils.daysInMonth(july))
        // 2026-07-01 is a Wednesday -> index 3 (0 = Sunday)
        assertEquals(3, DateTimeUtils.firstWeekdayOfMonth(july))
    }

    @Test
    fun `overdue detection`() {
        val yesterday = System.currentTimeMillis() - 2 * DateTimeUtils.DAY_MS
        val tomorrow = System.currentTimeMillis() + 2 * DateTimeUtils.DAY_MS
        assertTrue(DateTimeUtils.isOverdue(yesterday))
        assertFalse(DateTimeUtils.isOverdue(tomorrow))
        assertFalse(DateTimeUtils.isOverdue(null))
    }
}
