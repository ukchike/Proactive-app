package com.unifiedproductivity.app.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/** A single checkable line within a note's checklist (Apple Notes-style). */
@Serializable
data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val isChecked: Boolean = false
)
