package com.unifiedproductivity.app.ui.common

import android.content.Context
import android.location.Geocoder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Location input backed by the device's geocoding service (the same lookup Maps
 * uses, no API key required): type a place, tap search, pick from resolved
 * addresses. Free text still works if nothing matches.
 */
@Composable
fun LocationField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }

    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                suggestions = emptyList()
            },
            placeholder = { Text("Location / address (optional)") },
            leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
            trailingIcon = {
                if (searching) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    IconButton(
                        onClick = {
                            if (value.isBlank()) return@IconButton
                            scope.launch {
                                searching = true
                                suggestions = geocode(context, value)
                                searching = false
                            }
                        }
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Find address")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        suggestions.forEach { address ->
            TextButton(onClick = {
                onValueChange(address)
                suggestions = emptyList()
            }) {
                Text(address, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
    }
}

private suspend fun geocode(context: Context, query: String): List<String> =
    withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext emptyList()
        try {
            @Suppress("DEPRECATION")
            Geocoder(context).getFromLocationName(query, 5)
                ?.map { address ->
                    (0..address.maxAddressLineIndex).joinToString(", ") { address.getAddressLine(it) }
                }
                ?.filter { it.isNotBlank() }
                ?.distinct()
                ?: emptyList()
        } catch (e: Exception) {
            emptyList() // no geocoder backend / no network — free text still works
        }
    }
