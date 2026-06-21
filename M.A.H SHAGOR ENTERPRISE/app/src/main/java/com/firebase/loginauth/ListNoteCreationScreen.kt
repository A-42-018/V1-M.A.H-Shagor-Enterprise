package com.firebase.loginauth

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.firebase.loginauth.ui.theme.*

@Composable
fun ListNoteCreationScreen(
    onBackClick: () -> Unit,
    onSaveNote: (String, List<String>) -> Unit,
    notesViewModel: NotesViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var title by remember { mutableStateOf("") }
    var listItems by remember { mutableStateOf(listOf("")) }
    val saveStatus by notesViewModel.saveStatus.collectAsState()
    
    // Save when back button is pressed
    val handleBackPress = {
        val nonEmptyItems = listItems.filter { it.isNotBlank() }
        if (title.isNotBlank() && nonEmptyItems.isNotEmpty()) {
            onSaveNote(title, nonEmptyItems)
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
                    text = "নতুন লিস্ট নোট",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Manual save button
                TextButton(
                    onClick = {
                        val nonEmptyItems = listItems.filter { it.isNotBlank() }
                        if (title.isNotBlank() && nonEmptyItems.isNotEmpty()) {
                            onSaveNote(title, nonEmptyItems)
                        }
                    },
                    enabled = title.isNotBlank() && listItems.any { it.isNotBlank() }
                ) {
                    Text(
                        text = "সেভ",
                        color = if (title.isNotBlank() && listItems.any { it.isNotBlank() }) Color.White else Color.White.copy(alpha = 0.5f),
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
                        label = { Text("লিস্টের শিরোনাম") },
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
                
                // List Items
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(listItems) { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Number
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LightTeal,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                
                                // Text Field
                                OutlinedTextField(
                                    value = item,
                                    onValueChange = { newValue ->
                                        Log.d("ListNoteInput", "Input changed at index $index: '$newValue' (length: ${newValue.length})")
                                        listItems = listItems.toMutableList().apply {
                                            set(index, newValue)
                                            // Add new item if this is the last one and it's not empty
                                            if (index == size - 1 && newValue.isNotBlank()) {
                                                add("")
                                            }
                                        }
                                    },
                                    placeholder = { Text("আইটেম ${index + 1}") },
                                    modifier = Modifier.weight(1f),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = LightTeal,
                                        focusedLabelColor = LightTeal
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text
                                    ),
                                    singleLine = true
                                )
                                
                                // Delete button (only show if more than one item)
                                if (listItems.size > 1) {
                                    IconButton(
                                        onClick = {
                                            if (listItems.size > 1) {
                                                listItems = listItems.toMutableList().apply {
                                                    removeAt(index)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                        
                        // Add new item button
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                OutlinedButton(
                                    onClick = { 
                                        listItems = listItems.toMutableList().apply {
                                            add("")
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = LightTeal
                                    ),
                                    border = BorderStroke(1.dp, LightTeal)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Add,
                                        contentDescription = "Add Item",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("নতুন আইটেম যোগ করুন")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
