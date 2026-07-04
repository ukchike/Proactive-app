package com.unifiedproductivity.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.model.Eisenhower
import com.unifiedproductivity.app.ui.common.SwipeToDelete
import com.unifiedproductivity.app.ui.theme.AccentGray
import com.unifiedproductivity.app.ui.theme.NotesAccent
import com.unifiedproductivity.app.ui.theme.PriorityHigh
import com.unifiedproductivity.app.ui.theme.PriorityLow
import com.unifiedproductivity.app.ui.theme.PriorityMedium
import com.unifiedproductivity.app.util.DateTimeUtils

/** iOS system colors per Eisenhower quadrant. */
fun eisenhowerColor(quadrant: Eisenhower): Color = when (quadrant) {
    Eisenhower.URGENT_IMPORTANT -> PriorityHigh
    Eisenhower.IMPORTANT_NOT_URGENT -> PriorityMedium
    Eisenhower.URGENT_NOT_IMPORTANT -> PriorityLow
    else -> AccentGray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: NotesViewModel,
    onOpenNote: (String) -> Unit,
    onBack: () -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFolder by viewModel.selectedFolderId.collectAsStateWithLifecycle()
    var showFolderDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes", fontWeight = FontWeight.Bold, color = NotesAccent) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Home")
                    }
                },
                actions = {
                    IconButton(onClick = { showFolderDialog = true }) {
                        Icon(Icons.Filled.CreateNewFolder, contentDescription = "New folder")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onOpenNote("new") },
                containerColor = NotesAccent
            ) {
                Icon(Icons.Filled.Add, contentDescription = "New note")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setSearchQuery,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Search notes") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (folders.isNotEmpty()) {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedFolder == null,
                            onClick = { viewModel.selectFolder(null) },
                            label = { Text("All") }
                        )
                    }
                    items(folders, key = { it.id }) { folder ->
                        FilterChip(
                            selected = selectedFolder == folder.id,
                            onClick = {
                                viewModel.selectFolder(if (selectedFolder == folder.id) null else folder.id)
                            },
                            label = { Text(folder.name) }
                        )
                    }
                }
            }

            if (notes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        if (query.isNotBlank()) "No matching notes" else "No notes yet. Tap + to start.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notes, key = { it.id }) { note ->
                        SwipeToDelete(onDelete = { viewModel.deleteNote(note.id) }) {
                            NoteCard(
                                note = note,
                                onClick = { onOpenNote(note.id) },
                                onPin = { viewModel.togglePin(note) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showFolderDialog) {
        NewFolderDialog(
            onDismiss = { showFolderDialog = false },
            onCreate = { name ->
                viewModel.createFolder(name)
                showFolderDialog = false
            }
        )
    }
}

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onPin: () -> Unit) {
    Card(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifBlank { note.content.lineSequence().firstOrNull()?.take(40) ?: "New Note" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${DateTimeUtils.formatDate(note.modifiedAt)}  ·  ${note.content.take(60).replace('\n', ' ')}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.eisenhower != Eisenhower.NONE) {
                        Text(
                            note.eisenhower.short,
                            style = MaterialTheme.typography.labelSmall,
                            color = eisenhowerColor(note.eisenhower),
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    if (note.tags.isNotEmpty()) {
                        Text(
                            note.tags.joinToString(" ") { "#$it" },
                            style = MaterialTheme.typography.labelSmall,
                            color = NotesAccent
                        )
                    }
                }
            }
            IconButton(onClick = onPin) {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = "Pin",
                    tint = if (note.isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
        }
    }
}

@Composable
private fun NewFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Folder") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Folder name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
