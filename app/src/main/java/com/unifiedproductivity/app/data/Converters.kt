package com.unifiedproductivity.app.data

import androidx.room.TypeConverter
import com.unifiedproductivity.app.data.model.ChecklistItem
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Room type converters. Lists of strings are stored as a single delimited column.
 * We use the unit-separator character () as the delimiter so ordinary text
 * (tags, emails) never collides with it.
 */
class Converters {

    private val separator = ""

    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        if (value.isNullOrEmpty()) return ""
        return value.joinToString(separator)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrEmpty()) return emptyList()
        return value.split(separator)
    }

    private val json = Json { ignoreUnknownKeys = true }

    @TypeConverter
    fun fromChecklistItems(value: List<ChecklistItem>?): String =
        json.encodeToString(value ?: emptyList())

    @TypeConverter
    fun toChecklistItems(value: String?): List<ChecklistItem> =
        if (value.isNullOrEmpty()) emptyList() else json.decodeFromString(value)
}
