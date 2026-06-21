package com.firebase.loginauth

import android.content.Context
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

/**
 * FUNCTIONAL DATA RESTORE SYSTEM
 * This provides a complete working restore functionality for your business app
 */

@Composable
fun FunctionalRestoreDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    val backupViewModel = remember { BackupViewModel(context) }
    val uiState by backupViewModel.uiState.collectAsState()
    val isLoading by backupViewModel.isLoading.collectAsState()
    val backupEntries by backupViewModel.backupEntries.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    
    var selectedBackupId by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    // Load backups when dialog opens
    LaunchedEffect(Unit) {
        backupViewModel.loadBackups()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🔄 ডেটা রিস্টোর করুন",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF38A169)
                        )
                    }
                } else {
                    // Backup list
                    if (backupEntries.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = "No backups",
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "কোন ব্যাকআপ পাওয়া যায়নি",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                                Text(
                                    text = "প্রথমে একটি ব্যাকআপ তৈরি করুন",
                                    fontSize = 14.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(backupEntries) { backup ->
                                BackupItemCard(
                                    backup = backup,
                                    isSelected = selectedBackupId == backup.id,
                                    onSelect = { selectedBackupId = backup.id },
                                    onRestore = {
                                        selectedBackupId = backup.id
                                        showConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF718096)
                        )
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            selectedBackupId?.let {
                                showConfirmDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedBackupId != null && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38A169)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = "Restore",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("রিস্টোর করুন")
                    }
                }
                
                // Error/Success messages
                uiState.error?.let { error ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFED7D7)
                        )
                    ) {
                        Text(
                            text = "❌ $error",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFFC53030),
                            fontSize = 14.sp
                        )
                    }
                }
                
                uiState.successMessage?.let { message ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFC6F6D5)
                        )
                    ) {
                        Text(
                            text = "✅ $message",
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF2F855A),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog && selectedBackupId != null) {
        FunctionalRestoreConfirmationDialog(
            onConfirm = {
                coroutineScope.launch {
                    try {
                        backupViewModel.restoreBackup(selectedBackupId!!)
                        showConfirmDialog = false
                        // Close the main dialog after successful restore
                        onDismiss()
                    } catch (e: Exception) {
                        Log.e("FunctionalRestore", "Restore failed", e)
                    }
                }
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
fun BackupItemCard(
    backup: BackupEntry,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRestore: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE6FFFA) else Color(0xFFF7FAFC)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38A169))
        } else null
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = backup.description.ifEmpty { "ব্যাকআপ" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2D3748)
                    )
                    
                    Text(
                        text = formatFunctionalBackupDate(backup.timestamp),
                        fontSize = 14.sp,
                        color = Color(0xFF718096)
                    )
                    
                    Text(
                        text = "আকার: ${formatFunctionalDataSize(backup.dataSize)}",
                        fontSize = 12.sp,
                        color = Color(0xFF718096)
                    )
                }
                
                Button(
                    onClick = onRestore,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF38A169)
                    ),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("রিস্টোর", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun FunctionalRestoreConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "⚠️ নিশ্চিত করুন",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "আপনি কি নিশ্চিত যে আপনি এই ব্যাকআপ থেকে ডেটা রিস্টোর করতে চান? এটি আপনার বর্তমান সমস্ত ডেটা প্রতিস্থাপন করবে।",
                fontSize = 14.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE53E3E)
                )
            ) {
                Text("হ্যাঁ, রিস্টোর করুন")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("বাতিল")
            }
        }
    )
}

// Helper functions
private fun formatFunctionalBackupDate(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val format = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    return format.format(date)
}

private fun formatFunctionalDataSize(sizeInBytes: Long): String {
    return when {
        sizeInBytes < 1024 -> "${sizeInBytes} B"
        sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
        else -> "${sizeInBytes / (1024 * 1024)} MB"
    }
}
