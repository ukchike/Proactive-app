package com.unifiedproductivity.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.model.BudgetItemType
import com.unifiedproductivity.app.ui.common.pickDate
import com.unifiedproductivity.app.util.DateTimeUtils

/** Create/edit dialog for a single income or expense line within a budget list. */
@Composable
fun BudgetItemEditorDialog(
    initial: BudgetItem?,
    listId: String,
    onDismiss: () -> Unit,
    onSave: (BudgetItem) -> Unit
) {
    val context = LocalContext.current
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var amountText by remember { mutableStateOf(initial?.amount?.takeIf { it != 0L }?.toString() ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: BudgetItemType.EXPENSE) }
    var dueDate by remember { mutableStateOf(initial?.dueDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New Budget Item" else "Edit Budget Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Name (e.g. Rent, Salary)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input -> if (input.all { it.isDigit() }) amountText = input },
                    placeholder = { Text("Amount") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BudgetItemType.entries.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t.label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(
                        onClick = {
                            pickDate(context, dueDate ?: DateTimeUtils.endOfToday(), isDark) { picked ->
                                dueDate = picked
                            }
                        },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null) },
                        label = { Text(dueDate?.let { DateTimeUtils.formatDayMonth(it) } ?: "Add due date") }
                    )
                    if (dueDate != null) {
                        AssistChip(
                            onClick = { dueDate = null },
                            leadingIcon = { Icon(Icons.Filled.Close, contentDescription = null) },
                            label = { Text("Clear") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = amountText.toLongOrNull() ?: return@TextButton
                    if (name.isBlank()) return@TextButton
                    val base = initial ?: BudgetItem(listId = listId)
                    onSave(
                        base.copy(
                            name = name.trim(),
                            amount = amount,
                            type = type,
                            dueDate = dueDate
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
