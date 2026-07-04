package com.unifiedproductivity.app.util

import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.model.RecurrenceFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurrenceExpanderTest {

    private val hour = 60 * 60 * 1000L
    private val day = 24 * hour

    private fun event(
        start: Long,
        durationMs: Long = hour,
        recurrence: RecurrenceFrequency = RecurrenceFrequency.NONE,
        interval: Int = 1
    ) = Event(
        calendarId = "cal",
        startDateTime = start,
        endDateTime = start + durationMs,
        recurrence = recurrence,
        recurrenceInterval = interval
    )

    @Test
    fun `non-recurring event returns itself when overlapping range`() {
        val e = event(start = 1000L)
        val result = RecurrenceExpander.expand(e, rangeStart = 0L, rangeEnd = 2000L)
        assertEquals(1, result.size)
        assertEquals(1000L, result[0].startDateTime)
    }

    @Test
    fun `non-recurring event outside range returns nothing`() {
        val e = event(start = 1000L)
        val result = RecurrenceExpander.expand(e, rangeStart = 5000L, rangeEnd = 6000L)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `daily recurrence produces one occurrence per day in range`() {
        val start = 0L
        val e = event(start = start, recurrence = RecurrenceFrequency.DAILY, interval = 1)
        // A 5-day window starting at the series' own start.
        val result = RecurrenceExpander.expand(e, rangeStart = 0L, rangeEnd = 5 * day)
        assertEquals(5, result.size)
        assertEquals(listOf(0L, day, 2 * day, 3 * day, 4 * day), result.map { it.startDateTime })
    }

    @Test
    fun `weekly recurrence with interval 2 skips alternate weeks`() {
        val week = 7 * day
        val e = event(start = 0L, recurrence = RecurrenceFrequency.WEEKLY, interval = 2)
        val result = RecurrenceExpander.expand(e, rangeStart = 0L, rangeEnd = 5 * week)
        // Occurrences at 0, 2 weeks, 4 weeks within a 5-week window.
        assertEquals(listOf(0L, 2 * week, 4 * week), result.map { it.startDateTime })
    }

    @Test
    fun `recurring event that started before the range still appears inside it`() {
        // Started 10 days ago, daily — should still produce an occurrence today.
        val start = -10 * day
        val e = event(start = start, recurrence = RecurrenceFrequency.DAILY)
        val result = RecurrenceExpander.expand(e, rangeStart = 0L, rangeEnd = day)
        assertEquals(1, result.size)
        assertEquals(0L, result[0].startDateTime)
    }

    @Test
    fun `expanded occurrences preserve the original event id and duration`() {
        val e = event(start = 0L, durationMs = 2 * hour, recurrence = RecurrenceFrequency.DAILY)
        val result = RecurrenceExpander.expand(e, rangeStart = 0L, rangeEnd = 3 * day)
        result.forEach {
            assertEquals(e.id, it.id)
            assertEquals(2 * hour, it.endDateTime - it.startDateTime)
        }
    }

    @Test
    fun `none recurrence never loops past the single occurrence`() {
        val e = event(start = 0L, recurrence = RecurrenceFrequency.NONE)
        val result = RecurrenceExpander.expand(e, rangeStart = 0L, rangeEnd = 100 * day)
        assertEquals(1, result.size)
    }
}
