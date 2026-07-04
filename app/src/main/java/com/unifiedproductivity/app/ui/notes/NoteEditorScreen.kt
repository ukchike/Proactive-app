package com.unifiedproductivity.app.ui.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
import com.unifiedproductivity.app.data.entity.Note

/**
 * Rich-text note editor backed by compose-rich-editor. Formatting (bold/italic/
 * underline/headings/lists) is applied through a toolbar, and content is persisted
 * as Markdown to [Note.content] so it stays portable and diff-friendly.
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
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val richState = rememberRichTextState()

    LaunchedEffect(noteId) {
        val loaded = if (noteId == "new") Note() else viewModel.loadNote(noteId) ?: Note()
        note = loaded
        title = loaded.title
        tagsText = loaded.tags.joinToString(", ")
        folderId = loaded.folderId
        richState.setMarkdown(loaded.content)
    }

    fun persist() {
        val base = note ?: return
        val markdown = richState.toMarkdown()
        if (title.isBlank() && markdown.isBlank()) return // don't save empty notes
        val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        viewModel.saveNote(
            base.copy(title = title, content = markdown, tags = tags, folderId = folderId)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("") },
                navigationIcon = {
                    IconButton(onClick = { persist(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { persist(); onBack() }) {
                        Icon(Icons.Filled.Check, contentDescription = "Save")
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
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                placeholder = { Text("tags, comma separated") },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            if (folders.isNotEmpty()) {
                var folderMenuOpen by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { folderMenuOpen = true },
                        leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                        label = {
                            Text(folders.firstOrNull { it.id == folderId }?.name ?: "No folder")
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

            FormattingToolbar(
                onBold = { richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold)) },
                onItalic = { richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic)) },
                onUnderline = { richState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline)) },
                onH1 = { richState.toggleSpanStyle(SpanStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold)) },
                onH2 = { richState.toggleSpanStyle(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold)) },
                onBullet = { richState.toggleUnorderedList() },
                onNumbered = { richState.toggleOrderedList() }
            )

            RichTextEditor(
                state = richState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp)
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
