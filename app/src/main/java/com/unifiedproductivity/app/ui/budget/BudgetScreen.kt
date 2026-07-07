package com.unifiedproductivity.app.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unifiedproductivity.app.data.entity.BudgetItem
import com.unifiedproductivity.app.data.entity.BudgetList
import com.unifiedproductivity.app.data.model.BudgetItemType
import com.unifiedproductivity.app.ui.common.SwipeToDelete
import com.unifiedproductivity.app.ui.theme.AccentGreen
import com.unifiedproductivity.app.ui.theme.PriorityHigh
import com.unifiedproductivity.app.util.BudgetCalculations
import com.unifiedproductivity.app.util.CurrencyFormatter
import com.unifiedproductivity.app.util.DateTimeUtils

/**
 * Budget module: named lists of income/expense items with paid/received checkboxes
 * and progress bars, plus outstanding income/expense totals across every list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showNewList by remember { mutableStateOf(false) }
    var addItemToList by remember { mutableStateOf<String?>(null) }
    var editingItem by remember { mutableStateOf<BudgetItem?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewList = true }) {
                Icon(Icons.Filled.Add, contentDescription = "New list")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    TotalCard(
                        title = "Outstanding Income",
                        amount = state.outstandingIncome,
                        icon = Icons.Filled.TrendingUp,
                        color = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                    TotalCard(
                        title = "Outstanding Expenses",
                        amount = state.outstandingExpenses,
                        icon = Icons.Filled.TrendingDown,
                        color = PriorityHigh,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (state.lists.isEmpty()) {
                item {
                    Text(
                        "No budget lists yet — tap + to create one",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 24.dp)
                    )
                }
            } else {
                items(state.lists, key = { it.id }) { list ->
                    BudgetListCard(
                        list = list,
                        items = state.itemsByList[list.id].orEmpty(),
                        onAddItem = { addItemToList = list.id },
                        onToggleItem = { viewModel.toggleComplete(it) },
                        onEditItem = { editingItem = it },
                        onDeleteItem = { viewModel.deleteItem(it) }
                    )
                }
            }
        }
    }

    if (showNewList) {
        NewBudgetListDialog(
            onDismiss = { showNewList = false },
            onCreate = { name -> viewModel.createList(name); showNewList = false }
        )
    }

    if (addItemToList != null || editingItem != null) {
        BudgetItemEditorDialog(
            initial = editingItem,
            listId = editingItem?.listId ?: addItemToList!!,
            onDismiss = { addItemToList = null; editingItem = null },
            onSave = { item -> viewModel.saveItem(item); addItemToList = null; editingItem = null }
        )
    }
}

@Composable
private fun TotalCard(
    title: String,
    amount: Long,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.padding(end = 6.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = color)
            }
            Text(
                CurrencyFormatter.format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun BudgetListCard(
    list: BudgetList,
    items: List<BudgetItem>,
    onAddItem: () -> Unit,
    onToggleItem: (BudgetItem) -> Unit,
    onEditItem: (BudgetItem) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    val progress = BudgetCalculations.listProgress(items)
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(list.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Badge { Text("${progress.completedItems}/${progress.totalItems}") }
                    IconButton(onClick = onAddItem) {
                        Icon(Icons.Filled.Add, contentDescription = "Add item to ${list.name}")
                    }
                }
            }

            if (items.isNotEmpty()) {
                Text(
                    "${CurrencyFormatter.format(progress.completedAmount)} / ${CurrencyFormatter.format(progress.totalAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                LinearProgressIndicator(
                    progress = { progress.amountProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)
                )
            }

            items.forEach { budgetItem ->
                SwipeToDelete(onDelete = { onDeleteItem(budgetItem.id) }) {
                    BudgetItemRow(
                        item = budgetItem,
                        onToggle = { onToggleItem(budgetItem) },
                        onClick = { onEditItem(budgetItem) }
                    )
                }
            }
            if (items.isEmpty()) {
                Text(
                    "No items yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun BudgetItemRow(item: BudgetItem, onToggle: () -> Unit, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isCompleted) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isCompleted, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                    color = if (item.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.onSurface
                )
                item.dueDate?.let {
                    Text(
                        "Due ${DateTimeUtils.formatDayMonth(it)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Text(
                (if (item.type == BudgetItemType.INCOME) "+" else "-") + CurrencyFormatter.format(item.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textDecoration = if (item.isCompleted) TextDecoration.LineThrough else null,
                color = if (item.type == BudgetItemType.INCOME) AccentGreen else PriorityHigh
            )
        }
    }
}

@Composable
private fun NewBudgetListDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Budget List") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("e.g. Groceries") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
