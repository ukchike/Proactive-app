package com.unifiedproductivity.app.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unifiedproductivity.app.data.entity.Folder
import com.unifiedproductivity.app.data.entity.Note
import com.unifiedproductivity.app.data.repository.NotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(private val repository: NotesRepository) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val folders: StateFlow<List<Folder>> =
        repository.observeFolders()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Notes filtered by the current search query (query wins over folder filter). */
    val notes: StateFlow<List<Note>> =
        combine(_searchQuery, _selectedFolderId) { query, folderId -> query to folderId }
            .flatMapLatest { (query, folderId) ->
                when {
                    query.isNotBlank() -> repository.searchNotes(query)
                    folderId != null -> repository.observeNotesInFolder(folderId)
                    else -> repository.observeNotes()
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) { _searchQuery.value = query }

    fun selectFolder(folderId: String?) { _selectedFolderId.value = folderId }

    fun togglePin(note: Note) = viewModelScope.launch { repository.togglePin(note) }

    fun deleteNote(id: String) = viewModelScope.launch { repository.deleteNote(id) }

    fun createFolder(name: String) = viewModelScope.launch {
        repository.saveFolder(Folder(name = name))
    }

    // --- Single note editing ---

    suspend fun loadNote(id: String): Note? = repository.getNote(id)

    fun saveNote(note: Note) = viewModelScope.launch { repository.saveNote(note) }
}
