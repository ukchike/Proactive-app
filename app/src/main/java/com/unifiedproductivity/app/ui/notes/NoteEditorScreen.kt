package com.unifiedproductivity.app.ui.notes

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.model.ChecklistItem
import com.unifiedproductivity.app.data.model.Eisenhower

/**
 * Rich-text note editor backed by compose-rich-editor. Formatting (bold/italic/
 * underline/headings/lists) is applied through a toolbar, content is persisted as
 * Markdown to [Note.content], and an optional checklist (Apple Notes-style) lives
 * alongside it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NotesViewModel,
    noteId: String,
    onBack: () -> Unit
) {
    var note by remember { mutableStateOf<Note?>(null) }
    var title by remember { mutableStateOf("") }
    var tagsText by remember { mutableStateOf("") }
    var folderId by remember { mutableStateOf<String?>(null) }
    var eisenhower by remember { mutableStateOf(Eisenhower.NONE) }
    val checklist = remember { mutableStateListOf<ChecklistItem>() }
    var newChecklistText by remember { mutableStateOf("") }
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val richState = rememberRichTextState()

    LaunchedEffect(noteId) {
        val loaded = if (noteId == "new") Note() else viewModel.loadNote(noteId) ?: Note()
        note = loaded
        title = loaded.title
        tagsText = loaded.tags.joinToString(", ")
        folderId = loaded.folderId
        eisenhower = loaded.eisenhower
        checklist.clear()
        checklist.addAll(loaded.checklistItems)
        richState.setMarkdown(loaded.content)
    }

    fun persist() {
        val base = note ?: return
        val markdown = richState.toMarkdown()
        if (title.isBlank() && markdown.isBlank() && checklist.isEmpty()) return // nothing to save
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModel.saveNote(
            base.copy(
                title = title,
                content = markdown,
                tags = tags,
                folderId = folderId,
                eisenhower = eisenhower,
                checklistItems = checklist.toList()
            )
        )
    }

    fun addChecklistItem() {
        val text = newChecklistText.trim()
        if (text.isEmpty()) return
        checklist.add(ChecklistItem(text = text))
        newChecklistText = ""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { persist(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Notes")
                    }
                },
                actions = {
                    // iOS Notes uses a text "Done" action rather than an icon.
                    TextButton(onClick = { persist(); onBack() }) {
                        Text("Done", fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineMedium) },
                textStyle = MaterialTheme.typography.headlineMedium,
                colors = transparentFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            // Compact metadata row: tags + folder side by side to leave room to write.
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    placeholder = { Text("tags") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (folders.isNotEmpty()) {
                    var folderMenuOpen by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        AssistChip(
                            onClick = { folderMenuOpen = true },
                            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                            label = {
                                Text(folders.firstOrNull { it.id == folderId }?.name ?: "Folder")
                            }
                        )
                        DropdownMenu(
                            expanded = folderMenuOpen,
                            onDismissRequest = { folderMenuOpen = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No folder") },
                                onClick = { folderId = null; folderMenuOpen = false }
                            )
                            folders.forEach { folder ->
                                DropdownMenuItem(
                                    text = { Text(folder.name) },
                                    onClick = { folderId = folder.id; folderMenuOpen = false }
                                )
                            }
                        }
                    }
                }
            }

            // Eisenhower priority — urgent notes surface on the Home dashboard.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                Eisenhower.entries.forEach { quadrant ->
                    FilterChip(
                        selected = eisenhower == quadrant,
                        onClick = { eisenhower = quadrant },
                        label = {
                            Text(if (quadrant == Eisenhower.NONE) "No priority" else quadrant.short)
                        }
                    )
                }
            }

            ChecklistSection(
                items = checklist,
                newItemText = newChecklistText,
                onNewItemTextChange = { newChecklistText = it },
                onAddItem = ::addChecklistItem,
                onToggle = { id ->
                    val index = checklist.indexOfFirst { it.id == id }
                    if (index >= 0) checklist[index] = checklist[index].copy(isChecked = !checklist[index].isChecked)
                },
                onRemove = { id -> checklist.removeAll { it.id == id } }
            )

            FormattingToolbar(
                onBold = { richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                onItalic = { richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                onUnderline = { richState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                onH1 = { richState.toggleSpanStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) },
                onH2 = { richState.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)) },
                onBullet = { richState.toggleUnorderedList() },
                onNumbered = { richState.toggleOrderedList() }
            )

            // The writing box gets all remaining height, matches the surrounding
            // background (no boxed-in look), and never collapses.
            RichTextEditor(
                state = richState,
                colors = RichTextEditorDefaults.richTextEditorColors(
                    containerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(min = 300.dp)
                    .padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun ChecklistSection(
    items: List<ChecklistItem>,
    newItemText: String,
    onNewItemTextChange: (String) -> Unit,
    onAddItem: () -> Unit,
    onToggle: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        items.forEach { item ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(checked = item.isChecked, onCheckedChange = { onToggle(item.id) })
                Text(
                    item.text,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    color = if (item.isChecked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRemove(item.id) }) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onAddItem) {
                Icon(Icons.Filled.Add, contentDescription = "Add checklist item")
            }
            TextField(
                value = newItemText,
                onValueChange = onNewItemTextChange,
                placeholder = { Text("Add a checklist item") },
                singleLine = true,
                colors = transparentFieldColors(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onAddItem() }),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun FormattingToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onH1: () -> Unit,
    onH2: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item { ToolButton(onBold) { Icon(Icons.Filled.FormatBold, "Bold") } }
        item { ToolButton(onItalic) { Icon(Icons.Filled.FormatItalic, "Italic") } }
        item { ToolButton(onUnderline) { Icon(Icons.Filled.FormatUnderlined, "Underline") } }
        item { ToolButton(onH1) { Icon(Icons.Filled.Title, "Heading 1") } }
        item {
            ToolButton(onH2) {
                Row { Icon(Icons.Filled.Title, "Heading 2"); Text("2", fontSize = 12.sp) }
            }
        }
        item { ToolButton(onBullet) { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bulleted list") } }
        item { ToolButton(onNumbered) { Icon(Icons.Filled.FormatListNumbered, "Numbered list") } }
    }
}

@Composable
private fun ToolButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    FilledTonalIconButton(onClick = onClick) { content() }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent
)
