package com.unifiedproductivity.app.data.repository

import com.unifiedproductivity.app.data.dao.CalendarDao
import com.unifiedproductivity.app.data.dao.EventDao
import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.Event
import com.unifiedproductivity.app.util.RecurrenceExpander
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Data-access layer for the Calendar module. */
class CalendarRepository(
    private val calendarDao: CalendarDao,
    private val eventDao: EventDao
) {
    fun observeCalendars(): Flow<List<CalendarEntity>> = calendarDao.observeAll()

    suspend fun saveCalendar(calendar: CalendarEntity) = calendarDao.upsert(calendar)

    suspend fun deleteCalendar(id: String) = calendarDao.softDelete(id)

    suspend fun calendarCount(): Int = calendarDao.count()

    suspend fun findCalendarByName(name: String): CalendarEntity? = calendarDao.findByName(name)

    /** First calendar, created on demand — guarantees event saves always have a home. */
    suspend fun ensureDefaultCalendar(): CalendarEntity =
        calendarDao.getFirst() ?: CalendarEntity(name = "Personal", color = "#FF3B30")
            .also { calendarDao.upsert(it) }

    // ----- Events -----

    /** Events in [from, to), with recurring events expanded into concrete occurrences. */
    fun observeEventsInRange(from: Long, to: Long): Flow<List<Event>> =
        eventDao.observeInRange(from, to).map { candidates ->
            candidates.flatMap { RecurrenceExpander.expand(it, from, to) }
                .sortedBy { it.startDateTime }
        }

    fun searchEvents(query: String): Flow<List<Event>> = eventDao.search(query.trim())

    fun observeEvent(id: String): Flow<Event?> = eventDao.observeById(id)

    suspend fun getEvent(id: String): Event? = eventDao.getById(id)

    suspend fun saveEvent(event: Event) =
        eventDao.upsert(event.copy(modifiedAt = System.currentTimeMillis()))

    suspend fun deleteEvent(id: String) = eventDao.softDelete(id)

    /** Events that could still fire an alarm — used to re-arm alerts after a reboot. */
    suspend fun getUpcomingEvents(): List<Event> = eventDao.getUpcoming(System.currentTimeMillis())

    /** True if the given window overlaps any existing event (overbooking check). */
    fun observeOverlaps(from: Long, to: Long): Flow<List<Event>> = observeEventsInRange(from, to)
}
