package com.firebase.loginauth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val notesRepository = NotesRepository(application)
    
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    private val _textNotes = MutableStateFlow<List<Note>>(emptyList())
    val textNotes: StateFlow<List<Note>> = _textNotes.asStateFlow()
    
    private val _listNotes = MutableStateFlow<List<Note>>(emptyList())
    val listNotes: StateFlow<List<Note>> = _listNotes.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _saveStatus = MutableStateFlow<SaveStatus>(SaveStatus.Idle)
    val saveStatus: StateFlow<SaveStatus> = _saveStatus.asStateFlow()
    
    // Auto-save jobs
    private var autoSaveJob: Job? = null
    private val autoSaveDelay = 3000L // 3 seconds delay for auto-save (increased from 500ms)
    
    init {
        loadNotes()
    }
    
    private fun loadNotes() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Collect from local StateFlow for immediate updates on save
                notesRepository.notes.collect { notesList ->
                    _notes.value = notesList
                    _textNotes.value = notesList.filter { it.type == NoteType.TEXT }
                    _listNotes.value = notesList.filter { it.type == NoteType.LIST }
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error loading notes: ${e.message}")
                _isLoading.value = false
            }
        }

        // Background Firestore sync (one-shot; repository merges remote into local)
        viewModelScope.launch {
            try {
                notesRepository.syncWithFirestore()
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Error syncing with Firestore: ${e.message}")
            }
        }
    }
    
    // Save note immediately
    fun saveNote(note: Note) {
        Log.d("NotesViewModel", "saveNote called with: ID=${note.id}, Title=${note.title}, Type=${note.type}")
        viewModelScope.launch {
            _saveStatus.value = SaveStatus.Saving
            try {
                Log.d("NotesViewModel", "Calling repository.saveNote...")
                val isNewNote = _notes.value.none { it.id == note.id }
                val result = notesRepository.saveNote(note)
                if (result.isSuccess) {
                    Log.d("NotesViewModel", "Note saved successfully: ${result.getOrNull()}")
                    val noteTypeLabel = if (note.type == NoteType.TEXT) "টেক্সট নোট" else "লিস্ট নোট"
                    if (isNewNote) {
                        ActivityLogger.logNoteCreate(note.title, noteTypeLabel)
                    } else {
                        ActivityLogger.logNoteUpdate(note.title, noteTypeLabel)
                    }
                    _saveStatus.value = SaveStatus.Saved
                    // Reset status after showing success
                    delay(1000)
                    _saveStatus.value = SaveStatus.Idle
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to save note"
                    Log.e("NotesViewModel", "Failed to save note: $errorMsg")
                    _saveStatus.value = SaveStatus.Error(errorMsg)
                }
            } catch (e: Exception) {
                Log.e("NotesViewModel", "Exception in saveNote: ${e.message}", e)
                _saveStatus.value = SaveStatus.Error(e.message ?: "Failed to save note")
            }
        }
    }
    
    // Auto-save with debouncing
    fun autoSaveNote(note: Note) {
        // Only auto-save if there's meaningful content
        if (note.title.isBlank() && note.content.isBlank()) {
            return
        }
        
        // Cancel previous auto-save job
        autoSaveJob?.cancel()
        
        // Start new auto-save job with delay
        autoSaveJob = viewModelScope.launch {
            // Show "typing..." status briefly, then wait
            _saveStatus.value = SaveStatus.Typing
            delay(500L) // Short delay to show typing status
            
            // If still typing, show auto-saving status
            _saveStatus.value = SaveStatus.AutoSaving
            delay(autoSaveDelay - 500L) // Remaining delay
            
            try {
                val result = notesRepository.autoSaveNote(note)
                if (result.isSuccess) {
                    _saveStatus.value = SaveStatus.AutoSaved
                    // Reset status after showing success
                    delay(2000L) // Show success for 2 seconds
                    _saveStatus.value = SaveStatus.Idle
                } else {
                    _saveStatus.value = SaveStatus.Error(result.exceptionOrNull()?.message ?: "Auto-save failed")
                }
            } catch (e: Exception) {
                _saveStatus.value = SaveStatus.Error(e.message ?: "Auto-save failed")
            }
        }
    }
    
    // Delete note
    fun deleteNote(noteId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = notesRepository.deleteNote(noteId)
                if (result.isFailure) {
                    // Handle error if needed
                }
            } catch (e: Exception) {
                // Handle error if needed
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Create new note
    fun createNote(title: String, content: String, type: NoteType): Note {
        return Note(
            id = UUID.randomUUID().toString(), // Generate unique ID
            title = title,
            content = content,
            type = type,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
    }
    
    fun updateNote(existingNote: Note, newTitle: String, newContent: String): Note {
        return existingNote.copy(
            title = newTitle,
            content = newContent,
            updatedAt = System.currentTimeMillis()
        )
    }
    
    // Get notes by type for UI
    fun getNotesByType(type: NoteType): List<Note> {
        return when (type) {
            NoteType.TEXT -> _textNotes.value
            NoteType.LIST -> _listNotes.value
        }
    }
    
    // Clear save status
    fun clearSaveStatus() {
        _saveStatus.value = SaveStatus.Idle
    }
}

// Save status sealed class
sealed class SaveStatus {
    object Idle : SaveStatus()
    object Typing : SaveStatus()
    object Saving : SaveStatus()
    object Saved : SaveStatus()
    object AutoSaving : SaveStatus()
    object AutoSaved : SaveStatus()
    data class Error(val message: String) : SaveStatus()
}
