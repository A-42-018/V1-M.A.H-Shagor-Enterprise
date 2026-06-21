package com.firebase.loginauth

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    
    // Initialize BackupViewModel for full functionality
    val backupViewModel = remember { BackupViewModel(context) }
    val uiState by backupViewModel.uiState.collectAsState()
    val isLoading by backupViewModel.isLoading.collectAsState()
    val backupEntries by backupViewModel.backupEntries.collectAsState()
    
    // Initialize DataSyncViewModel for manual sync functionality
    val dataSyncViewModel = remember { DataSyncViewModel(context) }
    val syncStatus by dataSyncViewModel.syncStatus.collectAsState()
    
    // Dialog states - only for functional features
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        DarkTeal,
                        MediumTeal,
                        LightTeal
                    )
                )
            )
    ) {
        // Top App Bar
        SettingsTopAppBar(onBackClick = onBackClick)
        
        // Manual Sync Section
        ManualSyncSection(
            syncStatus = syncStatus,
            onSyncClick = { dataSyncViewModel.startFullSync() },
            lastSyncTime = dataSyncViewModel.getLastSyncTime(),
            isSyncNeeded = dataSyncViewModel.isSyncNeeded()
        )

        // Settings Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Data Backup & Restore Section
            item {
                SettingsSection(
                    title = "Data Management",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Filled.Add,
                            title = "Create Backup",
                            subtitle = "Save all business data to cloud storage",
                            onClick = { showBackupDialog = true },
                            iconColor = Color(0xFF38A169)
                        ),
                        SettingsItem(
                            icon = Icons.Filled.Refresh,
                            title = "Restore Backup",
                            subtitle = "Restore data from previous backups",
                            onClick = { showRestoreDialog = true },
                            iconColor = Color(0xFF9F7AEA)
                        )
                    )
                )
            }

            // Account Section
            item {
                SettingsSection(
                    title = "Account",
                    items = listOf(
                        SettingsItem(
                            icon = Icons.Filled.ExitToApp,
                            title = "Sign Out",
                            subtitle = "Log out from your account",
                            onClick = onSignOut,
                            iconColor = Color(0xFFE53E3E)
                        )
                    )
                )
            }
        }
    }

    // Dialog handling - only functional
    // Show success/error messages
    uiState.successMessage?.let { message ->
        LaunchedEffect(message) {
            // You can show a snackbar or toast here
            backupViewModel.clearMessages()
        }
    }
    
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            // You can show a snackbar or toast here
            backupViewModel.clearMessages()
        }
    }
    
    // Dialogs
    if (showBackupDialog) {
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            backupViewModel = backupViewModel,
            context = context
        )
    }
    
    if (showRestoreDialog) {
        FunctionalRestoreDialog(
            onDismiss = { showRestoreDialog = false },
            context = context
        )
    }
}

@Composable
fun SettingsTopAppBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .background(
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = Icons.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }
        
        Text(
            text = "Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    items: List<SettingsItem>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            items.forEachIndexed { index, item ->
                SettingsItemRow(item = item)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = DarkGray.copy(alpha = 0.1f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsItemRow(item: SettingsItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = item.iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Text(
                    text = item.subtitle,
                    fontSize = 14.sp,
                    color = DarkGray.copy(alpha = 0.7f)
                )
            }
        }
        
        Icon(
            imageVector = Icons.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = DarkGray.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

// Data classes
data class SettingsItem(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
    val iconColor: Color = DarkTeal
)



// Dialog Components

@Composable
fun BackupDialog(
    onDismiss: () -> Unit,
    backupViewModel: BackupViewModel,
    context: Context
) {
    val backupViewModel = remember { BackupViewModel(context) }
    val uiState by backupViewModel.uiState.collectAsState()
    val isLoading by backupViewModel.isLoading.collectAsState()
    
    var backupDescription by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Create Backup",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Save all your business data securely to Firebase cloud storage.",
                    fontSize = 14.sp,
                    color = DarkGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Backup description input
                OutlinedTextField(
                    value = backupDescription,
                    onValueChange = { backupDescription = it },
                    label = { Text("Backup Description (Optional)") },
                    placeholder = { Text("e.g: Monthly backup") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    singleLine = true
                )
                
                // Show error or success message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                uiState.successMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFF38A169),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DarkGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val description = backupDescription.ifBlank { "Manual backup" }
                            backupViewModel.createBackup(description)
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38A169))
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Create Backup", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun RestoreBackupDialog(
    onDismiss: () -> Unit,
    context: Context
) {
    val backupViewModel = remember { BackupViewModel(context) }
    val uiState by backupViewModel.uiState.collectAsState()
    val isLoading by backupViewModel.isLoading.collectAsState()
    val backupEntries by backupViewModel.backupEntries.collectAsState()
    
    var selectedBackupId by remember { mutableStateOf<String?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
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
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Restore Backup",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "Restore data from previous backups. This will replace current data.",
                    fontSize = 14.sp,
                    color = DarkGray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Show error or success message
                uiState.error?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                uiState.successMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFF38A169),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = DarkTeal)
                    }
                } else if (backupEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No backups found",
                                fontSize = 16.sp,
                                color = DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Create a backup first",
                                fontSize = 14.sp,
                                color = DarkGray
                            )
                        }
                    }
                } else {
                    // Backup list
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(backupEntries.size) { index ->
                            val backup = backupEntries[index]
                            BackupListItem(
                                backup = backup,
                                isSelected = selectedBackupId == backup.id,
                                onSelect = { selectedBackupId = backup.id },
                                onDelete = { backupViewModel.deleteBackup(backup.id) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = DarkGray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            selectedBackupId?.let {
                                showConfirmDialog = true
                            }
                        },
                        enabled = selectedBackupId != null && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F7AEA))
                    ) {
                        Text("Restore Backup", color = Color.White)
                    }
                }
            }
        }
    }
    
    // Confirmation dialog
    if (showConfirmDialog) {
        RestoreConfirmationDialog(
            onConfirm = {
                selectedBackupId?.let { backupId ->
                    backupViewModel.restoreBackup(backupId)
                }
                showConfirmDialog = false
            },
            onDismiss = { showConfirmDialog = false }
        )
    }
}

@Composable
fun BackupListItem(
    backup: BackupEntry,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE6FFFA) else Color(0xFFF7FAFC)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF38A169))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = backup.description,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatBackupDate(backup.timestamp),
                    fontSize = 12.sp,
                    color = DarkGray
                )
                Text(
                    text = "Size: ${formatDataSize(backup.dataSize)}",
                    fontSize = 12.sp,
                    color = DarkGray
                )
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    tint = Color(0xFFE53E3E),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun RestoreConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirm Restore",
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
        },
        text = {
            Text(
                text = "Are you sure you want to restore this backup? This will replace all your current data.",
                color = DarkGray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9F7AEA))
            ) {
                Text("Yes, Restore", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = DarkGray)
            }
        }
    )
}

@Composable
fun ManualSyncSection(
    syncStatus: SyncStatus,
    onSyncClick: () -> Unit,
    lastSyncTime: String,
    isSyncNeeded: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header with sync icon
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF38A169)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "🔄 Data Synchronization",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2D3748)
                    )
                }
                
                // Sync status indicator
                if (syncStatus.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF38A169),
                        strokeWidth = 2.dp
                    )
                } else if (isSyncNeeded) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Sync needed",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFFED8936)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Synced",
                        modifier = Modifier.size(20.dp),
                        tint = Color(0xFF38A169)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sync information
            Text(
                text = "Last sync: $lastSyncTime",
                fontSize = 14.sp,
                color = Color(0xFF718096)
            )
            
            if (syncStatus.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "❌ ${syncStatus.error}",
                    fontSize = 12.sp,
                    color = Color(0xFFE53E3E)
                )
            }
            
            if (syncStatus.successMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "✅ ${syncStatus.successMessage}",
                    fontSize = 12.sp,
                    color = Color(0xFF38A169)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sync button
            Button(
                onClick = onSyncClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !syncStatus.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF38A169),
                    disabledContainerColor = Color(0xFFE2E8F0)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (syncStatus.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Syncing...", color = Color.White)
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSyncNeeded) "Sync Now (Updates Available)" else "Force Sync",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Sync details
            if (syncStatus.syncedCollections.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Synced: ${syncStatus.syncedCollections.joinToString(", ")}",
                    fontSize = 12.sp,
                    color = Color(0xFF718096),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

// Helper functions
private fun formatBackupDate(timestamp: com.google.firebase.Timestamp): String {
    val date = timestamp.toDate()
    val formatter = java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault())
    return formatter.format(date)
}

private fun formatDataSize(sizeInBytes: Long): String {
    return when {
        sizeInBytes < 1024 -> "${sizeInBytes} B"
        sizeInBytes < 1024 * 1024 -> "${sizeInBytes / 1024} KB"
        else -> "${sizeInBytes / (1024 * 1024)} MB"
    }
}
