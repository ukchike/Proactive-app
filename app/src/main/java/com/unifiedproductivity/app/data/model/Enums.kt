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

/** Eisenhower-matrix priority for notes (urgent/important quadrants). */
@Serializable
enum class Eisenhower(val label: String, val short: String) {
    NONE("No priority", ""),
    URGENT_IMPORTANT("Urgent & Important", "Do now"),
    IMPORTANT_NOT_URGENT("Important, Not Urgent", "Schedule"),
    URGENT_NOT_IMPORTANT("Urgent, Not Important", "Delegate"),
    NOT_URGENT_NOT_IMPORTANT("Not Urgent or Important", "Later")
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

/** Whether a budget line item is money coming in or going out. */
@Serializable
enum class BudgetItemType(val label: String) {
    INCOME("Income"),
    EXPENSE("Expense")
}
