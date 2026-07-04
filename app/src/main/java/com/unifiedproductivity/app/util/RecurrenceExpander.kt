package com.unifiedproductivity.app.util

import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.data.model.RecurrenceFrequency

/**
 * Expands a (possibly recurring) [Event] into the concrete occurrences that fall
 * within a date range. Recurring events store only their first occurrence in Room —
 * this walks [DateTimeUtils.nextOccurrence] forward to materialize the rest,
 * entirely in memory, so nothing needs to be persisted per-instance.
 *
 * Expanded instances keep the original event's [Event.id] (they aren't separate
 * database rows), so editing or deleting any occurrence in the UI acts on the
 * whole series — the editor calls this out explicitly when recurrence is set.
 */
object RecurrenceExpander {

    /** Upper bound on generated occurrences per event, so a bad rule can't loop forever. */
    private const val MAX_OCCURRENCES = 500

    fun expand(event: Event, rangeStart: Long, rangeEnd: Long): List<Event> {
        if (event.recurrence == RecurrenceFrequency.NONE) {
            return if (overlaps(event.startDateTime, event.endDateTime, rangeStart, rangeEnd)) {
                listOf(event)
            } else {
                emptyList()
            }
        }

        val duration = (event.endDateTime - event.startDateTime).coerceAtLeast(0L)
        val result = mutableListOf<Event>()
        var occurrenceStart = event.startDateTime
        var iterations = 0

        while (occurrenceStart < rangeEnd && iterations < MAX_OCCURRENCES) {
            val occurrenceEnd = occurrenceStart + duration
            if (overlaps(occurrenceStart, occurrenceEnd, rangeStart, rangeEnd)) {
                result.add(event.copy(startDateTime = occurrenceStart, endDateTime = occurrenceEnd))
            }
            val next = DateTimeUtils.nextOccurrence(occurrenceStart, event.recurrence, event.recurrenceInterval)
            if (next == null || next <= occurrenceStart) break // guard against a non-advancing rule
            occurrenceStart = next
            iterations++
        }
        return result
    }

    private fun overlaps(startA: Long, endA: Long, startB: Long, endB: Long): Boolean =
        startA < endB && endA >= startB
}
