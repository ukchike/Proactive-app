package com.unifiedproductivity.app.data.repository

import com.unifiedproductivity.app.data.dao.CalendarDao
import com.unifiedproductivity.app.data.dao.EventDao
import com.unifiedproductivity.app.data.entity.CalendarEntity
import com.unifiedproductivity.app.data.entity.Event
import kotlinx.coroutines.flow.Flow

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

    // ----- Events -----

    fun observeEventsInRange(from: Long, to: Long): Flow<List<Event>> =
        eventDao.observeInRange(from, to)

    fun searchEvents(query: String): Flow<List<Event>> = eventDao.search(query.trim())

    fun observeEvent(id: String): Flow<Event?> = eventDao.observeById(id)

    suspend fun getEvent(id: String): Event? = eventDao.getById(id)

    suspend fun saveEvent(event: Event) =
        eventDao.upsert(event.copy(modifiedAt = System.currentTimeMillis()))

    suspend fun deleteEvent(id: String) = eventDao.softDelete(id)

    /** True if the given window overlaps any existing event (overbooking check). */
    fun observeOverlaps(from: Long, to: Long): Flow<List<Event>> =
        eventDao.observeInRange(from, to)
}
