package com.unifiedproductivity.app.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.unifiedproductivity.app.data.model.BudgetItemType
import com.unifiedproductivity.app.sync.ConflictResolver
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * A single income or expense line within a [BudgetList] (e.g. "Rent — ₦36,000").
 * [isCompleted] marks it paid (expense) or received (income). Amounts are stored as
 * whole currency units (no decimals), matching how the reference prototype and
 * everyday Naira budgeting works.
 */
@Serializable
@Entity(
    tableName = "budget_items",
    indices = [Index("listId"), Index("dueDate")]
)
data class BudgetItem(
    @PrimaryKey override val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val name: String = "",
    val amount: Long = 0,
    val type: BudgetItemType = BudgetItemType.EXPENSE,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    override val modifiedAt: Long = System.currentTimeMillis(),
    override val deletedAt: Long? = null
) : ConflictResolver.Versioned
