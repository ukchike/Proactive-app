package com.unifiedproductivity.app.data.model

import kotlinx.serialization.Serializable

/** Priority levels for reminders, with an ordinal used for sorting. */
@Serializable
enum class Priority(val sortRank: Int, val label: String) {
    HIGH(0, "High"),
    MEDIUM(1, "Medium"),
    LOW(2, "Low"),
    NONE(3, "None")
}

/** Specialized note templates (see spec: Notes module, "Note types"). */
@Serializable
enum class NoteType(val label: String) {
    FREE_FORM("Note"),
    MEETING("Meeting Notes"),
    RESEARCH("Research"),
    JOURNAL("Journal"),
    FINANCIAL("Financial Record")
}

/** How often a reminder or event repeats. Kept lightweight for the MVP. */
@Serializable
enum class RecurrenceFrequency {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

/** RSVP status for calendar event attendees. */
enum class AttendeeStatus {
    ACCEPTED, DECLINED, TENTATIVE, AWAITING
}

/** The smart lists shown at the top of the Reminders module. */
enum class SmartList(val label: String) {
    TODAY("Today"),
    SCHEDULED("Scheduled"),
    ALL("All"),
    FLAGGED("Flagged"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}
