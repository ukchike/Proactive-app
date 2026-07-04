package com.unifiedproductivity.app.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.unifiedproductivity.app.data.model.RecurrenceFrequency

private val frequencyLabels = mapOf(
    RecurrenceFrequency.NONE to "Never",
    RecurrenceFrequency.DAILY to "Daily",
    RecurrenceFrequency.WEEKLY to "Weekly",
    RecurrenceFrequency.MONTHLY to "Monthly",
    RecurrenceFrequency.YEARLY to "Yearly"
)

/** "Repeat" picker: frequency chips plus an interval stepper ("every N weeks", etc). */
@Composable
fun RecurrencePicker(
    frequency: RecurrenceFrequency,
    interval: Int,
    onFrequencyChange: (RecurrenceFrequency) -> Unit,
    onIntervalChange: (Int) -> Unit
) {
    Column {
        Text("Repeat", style = MaterialTheme.typography.bodyMedium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            RecurrenceFrequency.entries.forEach { freq ->
                FilterChip(
                    selected = frequency == freq,
                    onClick = { onFrequencyChange(freq) },
                    label = { Text(frequencyLabels.getValue(freq)) }
                )
            }
        }
        if (frequency != RecurrenceFrequency.NONE) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Every", style = MaterialTheme.typography.bodyMedium)
                IconButton(
                    onClick = { onIntervalChange((interval - 1).coerceAtLeast(1)) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Filled.Remove, contentDescription = "Fewer") }
                Text("$interval", style = MaterialTheme.typography.bodyLarge)
                IconButton(
                    onClick = { onIntervalChange((interval + 1).coerceAtMost(30)) },
                    modifier = Modifier.size(32.dp)
                ) { Icon(Icons.Filled.Add, contentDescription = "More") }
                Text(unitLabel(frequency, interval), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun unitLabel(frequency: RecurrenceFrequency, interval: Int): String {
    val plural = interval != 1
    return when (frequency) {
        RecurrenceFrequency.DAILY -> if (plural) "days" else "day"
        RecurrenceFrequency.WEEKLY -> if (plural) "weeks" else "week"
        RecurrenceFrequency.MONTHLY -> if (plural) "months" else "month"
        RecurrenceFrequency.YEARLY -> if (plural) "years" else "year"
        RecurrenceFrequency.NONE -> ""
    }
}
