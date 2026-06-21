package com.firebase.loginauth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.outlined.Star
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

// Data classes for notes
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val type: NoteType,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NoteType {
    TEXT, LIST
}

data class ListItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isCompleted: Boolean = false
)

@Composable
fun NotesScreen(
    onBackClick: () -> Unit,
    notesViewModel: NotesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showTextNoteCreation by remember { mutableStateOf(false) }
    var showListNoteCreation by remember { mutableStateOf(false) }
    var editingNote by remember { mutableStateOf<Note?>(null) }
    
    // Collect notes from ViewModel
    val allNotes by notesViewModel.notes.collectAsState()
    val textNotes by notesViewModel.textNotes.collectAsState()
    val listNotes by notesViewModel.listNotes.collectAsState()
    val isLoading by notesViewModel.isLoading.collectAsState()
    val saveStatus by notesViewModel.saveStatus.collectAsState()

    // Get current notes based on selected tab
    val currentNotes = when (selectedTab) {
        0 -> textNotes
        1 -> listNotes
        else -> allNotes
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "নোটপ্যাড",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
            }

            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Note Type Tabs
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        NoteTypeTab(
                            icon = Icons.Filled.Edit,
                            label = "টেক্সট নোট",
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        NoteTypeTab(
                            icon = Icons.Filled.List,
                            label = "লিস্ট নোট",
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes List
                Card(
                    modifier = Modifier.fillMaxSize(),
                    colors = CardDefaults.cardColors(containerColor = CardWhite),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (currentNotes.isEmpty()) {
                                item {
                                    EmptyNotesState(
                                        noteType = if (selectedTab == 0) "টেক্সট নোট" else "লিস্ট নোট"
                                    )
                                }
                            } else {
                                itemsIndexed(currentNotes) { index, note ->
                                    NoteItem(
                                        note = note,
                                        onEdit = { editNote ->
                                            editingNote = editNote
                                        },
                                        onDelete = { notesViewModel.deleteNote(note.id) },
                                        index = index
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }


        
        // Floating Action Button for adding notes
        FloatingActionButton(
            onClick = {
                when (selectedTab) {
                    0 -> showTextNoteCreation = true // Text Note
                    1 -> showListNoteCreation = true // List Note
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-40).dp, y = (-160).dp) // Position to match red circle
                .size(64.dp), // Larger size like in screenshot
            containerColor = LightTeal,
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 20.dp, // Increased shadow
                pressedElevation = 24.dp,
                focusedElevation = 20.dp,
                hoveredElevation = 22.dp
            )
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Note",
                modifier = Modifier.size(32.dp) // Larger icon
            )
        }
    }

    // Navigate to Text Note Creation
    if (showTextNoteCreation) {
        TextNoteCreationScreen(
            onBackClick = { showTextNoteCreation = false },
            onSaveNote = { title, content ->
                val newNote = notesViewModel.createNote(title, content, NoteType.TEXT)
                notesViewModel.saveNote(newNote)
                showTextNoteCreation = false
            },
            notesViewModel = notesViewModel
        )
    }
    
    // Navigate to List Note Creation
    if (showListNoteCreation) {
        ListNoteCreationScreen(
            onBackClick = { showListNoteCreation = false },
            onSaveNote = { title, items ->
                val content = items.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n")
                val newNote = notesViewModel.createNote(title, content, NoteType.LIST)
                notesViewModel.saveNote(newNote)
                showListNoteCreation = false
            },
            notesViewModel = notesViewModel
        )
    }
    
    // Navigate to Edit Note Screen
    editingNote?.let { note ->
        if (note.type == NoteType.TEXT) {
            TextNoteEditScreen(
                note = note,
                onBackClick = { editingNote = null },
                onSaveNote = { updatedTitle, updatedContent ->
                    val updatedNote = notesViewModel.updateNote(note, updatedTitle, updatedContent)
                    notesViewModel.saveNote(updatedNote)
                    editingNote = null
                },
                notesViewModel = notesViewModel
            )
        } else {
            // Handle list note editing if needed
            ListNoteEditScreen(
                note = note,
                onBackClick = { editingNote = null },
                onSaveNote = { updatedTitle, updatedItems ->
                    val updatedContent = updatedItems.mapIndexed { index, item -> "${index + 1}. $item" }.joinToString("\n")
                    val updatedNote = notesViewModel.updateNote(note, updatedTitle, updatedContent)
                    notesViewModel.saveNote(updatedNote)
                    editingNote = null
                },
                notesViewModel = notesViewModel
            )
        }
    }
}

@Composable
fun NoteTypeTab(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) LightTeal else Color.Transparent
    val textColor = if (isSelected) Color.White else TextDark

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                color = textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
@Composable
fun NoteItem(
    note: Note,
    onEdit: (Note) -> Unit,
    onDelete: (Note) -> Unit,
    index: Int = 0
) {
    // Different gradient colors for variety
    val gradientColors = listOf(
        listOf(Color(0xFFFF9A9E), Color(0xFFFECFEF)), // Pink gradient
        listOf(Color(0xFFA18CD1), Color(0xFFFBC2EB)), // Purple gradient  
        listOf(Color(0xFF667eea), Color(0xFF764ba2)), // Blue gradient
        listOf(Color(0xFF89f7fe), Color(0xFF66a6ff)), // Light blue gradient
        listOf(Color(0xFFffecd2), Color(0xFFfcb69f))  // Orange gradient
    )
    
    val selectedGradient = gradientColors[index % gradientColors.size]
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = selectedGradient,
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        // Date and time
                        Text(
                            text = formatNoteDate(note.updatedAt),
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Title
                        Text(
                            text = note.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Content preview
                        Text(
                            text = note.content,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                    
                    // Action buttons
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        // Star/Favorite button (for future use)
                        IconButton(
                            onClick = { /* TODO: Implement favorite */ },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = "Favorite",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Edit button
                        IconButton(
                            onClick = { onEdit(note) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        
                        // Delete button
                        IconButton(
                            onClick = { onDelete(note) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesState(noteType: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Create,
            contentDescription = null,
            tint = SlateBluGray,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "কোন $noteType নোট নেই",
            fontSize = 16.sp,
            color = SlateBluGray,
            textAlign = TextAlign.Center
        )
        Text(
            text = "নতুন নোট যোগ করতে + বাটনে ক্লিক করুন",
            fontSize = 14.sp,
            color = DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onAddNote: (String, String, NoteType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(NoteType.TEXT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "নতুন নোট যোগ করুন",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Note Type Selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { selectedType = NoteType.TEXT },
                        label = { Text("টেক্সট") },
                        selected = selectedType == NoteType.TEXT
                    )
                    FilterChip(
                        onClick = { selectedType = NoteType.LIST },
                        label = { Text("লিস্ট") },
                        selected = selectedType == NoteType.LIST
                    )
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("শিরোনাম") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("বিষয়বস্তু") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onAddNote(title, content, selectedType)
                    }
                }
            ) {
                Text("যোগ করুন", color = LightTeal)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বাতিল", color = DarkGray)
            }
        }
    )
}

fun formatNoteDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@Composable
fun TextNoteEditScreen(
    note: Note,
    onBackClick: () -> Unit,
    onSaveNote: (String, String) -> Unit,
    notesViewModel: NotesViewModel
) {
    var title by remember { mutableStateOf(note.title) }
    var content by remember { mutableStateOf(note.content) }
    
    // Save when back button is pressed
    val handleBackPress = {
        if (title.isNotBlank() || content.isNotBlank()) {
            onSaveNote(title, content)
        }
        onBackClick()
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal)
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = handleBackPress) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = Color.White
                )
            }
            
            Text(
                text = "নোট সম্পাদনা করুন",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // No save indicators - seamless saving like modern notes apps
            Spacer(modifier = Modifier.width(48.dp)) // Maintain layout balance
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title Input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = DarkTeal,
                        fontWeight = FontWeight.Bold
                    ),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "নোটের শিরোনাম লিখুন...",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            // Content Input
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = DarkTeal
                    ),
                    decorationBox = { innerTextField ->
                        if (content.isEmpty()) {
                            Text(
                                text = "আপনার নোট এখানে লিখুন...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }
    }
}

@Composable
fun ListNoteEditScreen(
    note: Note,
    onBackClick: () -> Unit,
    onSaveNote: (String, List<String>) -> Unit,
    notesViewModel: NotesViewModel
) {
    var title by remember { mutableStateOf(note.title) }
    // Parse existing list items from content
    val existingItems = note.content.split("\n").mapNotNull { line ->
        val trimmed = line.trim()
        if (trimmed.matches(Regex("^\\d+\\. .+"))) {
            trimmed.substringAfter(". ")
        } else null
    }
    var items by remember { mutableStateOf(existingItems.toMutableList()) }
    val saveStatus by notesViewModel.saveStatus.collectAsState()
    
    // Auto-save effect
    LaunchedEffect(title, items) {
        if (title.isNotBlank() || items.any { it.isNotBlank() }) {
            // Auto-save will be handled by the ViewModel's existing logic
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal)
                )
            )
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "ফিরে যান",
                    tint = Color.White
                )
            }
            
            Text(
                text = "তালিকা নোট সম্পাদনা",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            // Save status indicator
            when (saveStatus) {
                is SaveStatus.Saving -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "সংরক্ষণ হচ্ছে...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
                is SaveStatus.Saved -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "সংরক্ষিত",
                            tint = Color.Green,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "সংরক্ষিত",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Green
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = { onSaveNote(title, items.filter { it.isNotBlank() }) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightTeal
                        )
                    ) {
                        Text("সংরক্ষণ করুন", color = Color.White)
                    }
                }
            }
        }
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Title Input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        color = DarkTeal,
                        fontWeight = FontWeight.Bold
                    ),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "তালিকার শিরোনাম লিখুন...",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Gray
                            )
                        }
                        innerTextField()
                    }
                )
            }
            
            // List Items
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(items) { index, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = DarkTeal,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.width(32.dp)
                            )
                            
                            BasicTextField(
                                value = item,
                                onValueChange = { newValue ->
                                    items = items.toMutableList().apply {
                                        this[index] = newValue
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = DarkTeal
                                ),
                                decorationBox = { innerTextField ->
                                    if (item.isEmpty()) {
                                        Text(
                                            text = "আইটেম ${index + 1} লিখুন...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = Color.Gray
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            
                            IconButton(
                                onClick = {
                                    items = items.toMutableList().apply {
                                        removeAt(index)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "মুছে ফেলুন",
                                    tint = Color.Red
                                )
                            }
                        }
                    }
                    
                    item {
                        Button(
                            onClick = {
                                items = items.toMutableList().apply {
                                    add("")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightTeal
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "নতুন আইটেম",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("নতুন আইটেম যোগ করুন", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TextNoteCreationScreen(
    onBackClick: () -> Unit,
    onSaveNote: (String, String) -> Unit,
    notesViewModel: NotesViewModel
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    
    // Save when back button is pressed
    val handleBackPress = {
        if (title.isNotBlank() || content.isNotBlank()) {
            onSaveNote(title, content)
        }
        onBackClick()
    }
    
    // Handle system back gesture
    BackHandler {
        handleBackPress()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = handleBackPress) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Text(
                    text = "নতুন টেক্সট নোট",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Save button
                TextButton(
                    onClick = {
                        if (title.isNotBlank() || content.isNotBlank()) {
                            onSaveNote(title, content)
                        }
                        onBackClick()
                    }
                ) {
                    Text(
                        text = "সেভ",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Title Input
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("নোটের শিরোনাম") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightTeal,
                            focusedLabelColor = LightTeal
                        ),
                        singleLine = true
                    )
                }
                
                // Content Input
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("নোটের বিষয়বস্তু") },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightTeal,
                            focusedLabelColor = LightTeal
                        ),
                        maxLines = Int.MAX_VALUE
                    )
                }
            }
        }
    }
}
