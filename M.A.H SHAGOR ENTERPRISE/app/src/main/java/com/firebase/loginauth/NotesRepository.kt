package com.firebase.loginauth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class NotesRepository(context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("notes_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // StateFlow for reactive UI updates
    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> = _notes.asStateFlow()
    
    companion object {
        private const val NOTES_KEY = "notes_list"
        private const val TAG = "NotesRepository"
    }
    
    init {
        loadLocalData()
        // Auto-sync with Firestore if user is authenticated
        if (auth.currentUser != null) {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    syncWithFirestore()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during auto-sync", e)
            }
        }
    }
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    private fun getUserNotesCollection() = getCurrentUserId()?.let { userId ->
        firestore.collection("users").document(userId).collection("notes")
    }
    
    // Local Storage Methods
    private fun loadLocalData() {
        try {
            val notesJson = sharedPreferences.getString(NOTES_KEY, "[]")
            val type = object : TypeToken<List<Note>>() {}.type
            val notesList: List<Note> = gson.fromJson(notesJson, type) ?: emptyList()
            
            _notes.value = notesList
            Log.d(TAG, "Loaded ${notesList.size} notes from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading notes from local storage", e)
            _notes.value = emptyList()
        }
    }
    
    private fun saveLocalData() {
        try {
            val notesJson = gson.toJson(_notes.value)
            sharedPreferences.edit()
                .putString(NOTES_KEY, notesJson)
                .apply()
            
            Log.d(TAG, "Saved ${_notes.value.size} notes to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notes to local storage", e)
        }
    }
    
    // Firestore Sync Methods
    suspend fun syncWithFirestore() {
        try {
            val userId = getCurrentUserId() ?: return
            val notesCollection = getUserNotesCollection() ?: return
            
            val snapshot = notesCollection.get().await()
            val firestoreNotes = snapshot.documents.mapNotNull { doc ->
                try {
                    Note(
                        id = doc.getString("id") ?: "",
                        title = doc.getString("title") ?: "",
                        content = doc.getString("content") ?: "",
                        type = NoteType.valueOf(doc.getString("type") ?: "TEXT"),
                        createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                        updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing note document", e)
                    null
                }
            }
            
            // Merge with local data (prioritize local changes)
            val mergedNotes = mergeNotes(_notes.value, firestoreNotes)
            _notes.value = mergedNotes.sortedByDescending { it.updatedAt }
            saveLocalData()
            
            Log.d(TAG, "Synced ${mergedNotes.size} notes with Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore", e)
        }
    }
    
    private fun mergeNotes(localNotes: List<Note>, firestoreNotes: List<Note>): List<Note> {
        val mergedMap = mutableMapOf<String, Note>()
        
        // Add Firestore notes first
        firestoreNotes.forEach { note ->
            mergedMap[note.id] = note
        }
        
        // Override with local notes (prioritize local changes)
        localNotes.forEach { note ->
            mergedMap[note.id] = note
        }
        
        return mergedMap.values.toList()
    }
    
    suspend fun uploadAllToFirestore() {
        try {
            _notes.value.forEach { note ->
                saveToFirestore(note)
            }
            Log.d(TAG, "Uploaded all notes to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading notes to Firestore", e)
        }
    }
    
    private suspend fun saveToFirestore(note: Note) {
        try {
            val userId = getCurrentUserId() ?: return
            val notesCollection = getUserNotesCollection() ?: return
            
            val noteData = hashMapOf(
                "id" to note.id,
                "title" to note.title,
                "content" to note.content,
                "type" to note.type.name,
                "createdAt" to note.createdAt,
                "updatedAt" to note.updatedAt,
                "userId" to userId
            )
            
            notesCollection.document(note.id).set(noteData).await()
            Log.d(TAG, "Saved note ${note.id} to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note to Firestore", e)
        }
    }
    
    private suspend fun deleteFromFirestore(noteId: String) {
        try {
            val notesCollection = getUserNotesCollection() ?: return
            notesCollection.document(noteId).delete().await()
            Log.d(TAG, "Deleted note $noteId from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note from Firestore", e)
        }
    }
    
    // Save or update a note
    suspend fun saveNote(note: Note): Result<String> {
        return try {
            // Update local storage first
            val updatedNotes = _notes.value.toMutableList()
            val existingIndex = updatedNotes.indexOfFirst { it.id == note.id }
            
            val updatedNote = note.copy(updatedAt = System.currentTimeMillis())
            
            if (existingIndex != -1) {
                updatedNotes[existingIndex] = updatedNote
            } else {
                updatedNotes.add(updatedNote)
            }
            
            _notes.value = updatedNotes.sortedByDescending { it.updatedAt }
            saveLocalData()
            
            // Save to Firestore in background
            CoroutineScope(Dispatchers.IO).launch {
                saveToFirestore(updatedNote)
            }
            
            Log.d(TAG, "Note saved successfully: ${note.id}")
            Result.success(note.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Get all notes for current user
    fun getAllNotes(): Flow<List<Note>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val notesCollection = getUserNotesCollection()
        if (notesCollection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = notesCollection
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore getAllNotes error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notes = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Note(
                            id = document.getString("id") ?: "",
                            title = document.getString("title") ?: "",
                            content = document.getString("content") ?: "",
                            type = NoteType.valueOf(document.getString("type") ?: "TEXT"),
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()
                
                trySend(notes)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Delete a note
    suspend fun deleteNote(noteId: String): Result<Unit> {
        return try {
            // Delete from local storage first
            val updatedNotes = _notes.value.toMutableList()
            updatedNotes.removeAll { it.id == noteId }
            _notes.value = updatedNotes
            saveLocalData()
            
            // Delete from Firestore in background
            CoroutineScope(Dispatchers.IO).launch {
                deleteFromFirestore(noteId)
            }
            
            Log.d(TAG, "Note deleted successfully: $noteId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Get notes by type (simplified query to avoid composite index)
    fun getNotesByType(type: NoteType): Flow<List<Note>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val notesCollection = getUserNotesCollection()
        if (notesCollection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        // Use simple query without ordering to avoid composite index requirement
        val listener = notesCollection
            .whereEqualTo("type", type.name)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Firestore getNotesByType error: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                val notes = snapshot?.documents?.mapNotNull { document ->
                    try {
                        Note(
                            id = document.getString("id") ?: "",
                            title = document.getString("title") ?: "",
                            content = document.getString("content") ?: "",
                            type = NoteType.valueOf(document.getString("type") ?: "TEXT"),
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.updatedAt } ?: emptyList() // Sort in memory instead
                
                trySend(notes)
            }
        
        awaitClose { listener.remove() }
    }
    
    // Auto-save functionality - saves note after a delay
    suspend fun autoSaveNote(note: Note): Result<String> {
        return saveNote(note)
    }
}
