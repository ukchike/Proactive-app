package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** A named group of budget items (e.g. "Groceries", "Monthly Home Expenses"). */
@Serializable
@Entity(tableName = "budget_lists")
data class BudgetList(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null
)
