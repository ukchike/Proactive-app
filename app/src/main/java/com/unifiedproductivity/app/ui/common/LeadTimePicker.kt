package com.unifiedproductivity.app.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Minutes-before options for "remind me" pickers; null = no notification at all. */
private val leadTimeOptions: List<Pair<Int?, String>> = listOf(
    null to "None",
    0 to "At time",
    5 to "5 min before",
    15 to "15 min before",
    30 to "30 min before",
    60 to "1 hour before",
    1440 to "1 day before"
)

/**
 * "Remind me" picker: choose how long before the due date/event start a
 * notification fires, or turn notifications off entirely for this item.
 */
@Composable
fun LeadTimePicker(
    label: String,
    selectedMinutes: Int?,
    onSelect: (Int?) -> Unit
) {
    Column {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            leadTimeOptions.forEach { (minutes, text) ->
                FilterChip(
                    selected = selectedMinutes == minutes,
                    onClick = { onSelect(minutes) },
                    label = { Text(text) }
                )
            }
        }
    }
}
