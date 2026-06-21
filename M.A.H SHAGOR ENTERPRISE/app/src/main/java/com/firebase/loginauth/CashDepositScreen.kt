package com.firebase.loginauth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.firebase.loginauth.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashDepositScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val cashDepositViewModel: CashDepositViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(context.applicationContext as android.app.Application)
    )
    var cylinderCount by remember { mutableStateOf("") }
    var amountValue by remember { mutableStateOf("") }
    val saveStatus by cashDepositViewModel.saveStatus.collectAsState()
    val cashDeposits by cashDepositViewModel.cashDeposits.collectAsState()
    val isLoading by cashDepositViewModel.isLoading.collectAsState()
    
    // Real-time date and time
    var currentDateTime by remember { mutableStateOf(getCurrentDateTime()) }
    
    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentDateTime = getCurrentDateTime()
        }
    }
    
    // Clear save status when it's an error after some time
    LaunchedEffect(saveStatus) {
        if (saveStatus is CashDepositSaveStatus.Error) {
            delay(3000)
            cashDepositViewModel.clearSaveStatus()
        } else if (saveStatus is CashDepositSaveStatus.Saved) {
            // Clear form after successful save
            cylinderCount = ""
            amountValue = ""
        }
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
                    text = "ক্যাশ জমা",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                // Save status indicator
                when (saveStatus) {
                    is CashDepositSaveStatus.AutoSaving -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "সেভ হচ্ছে...",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    is CashDepositSaveStatus.AutoSaved -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Saved",
                                tint = Color.Green,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "সেভ হয়েছে",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                    else -> {
                        Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
                    }
                }
            }
            
            // Dashboard Section
            val totalCashAmount = remember(cashDeposits) {
                cashDeposits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
            }
            val totalCylinderCount = remember(cashDeposits) {
                cashDeposits.sumOf { it.cylinderCount.toIntOrNull() ?: 0 }
            }
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "📊 ড্যাশবোর্ড",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Total Cash Entry Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = LightTeal.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, LightTeal)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "💰",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "মোট ক্যাশ জমা",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "৳${String.format("%.0f", totalCashAmount)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTeal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        // Total Cylinder Count Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF4CAF50))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "🛢️",
                                    fontSize = 24.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                Text(
                                    text = "মোট বিক্রিত সিলিন্ডার",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "$totalCylinderCount টি",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF4CAF50),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Summary Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = DarkTeal.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "মোট এন্ট্রি: ${cashDeposits.size}টি",
                            fontSize = 14.sp,
                            color = DarkTeal,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "গড় বিক্রয়: ৳${if (totalCylinderCount > 0) String.format("%.0f", totalCashAmount / totalCylinderCount) else "0"}",
                            fontSize = 14.sp,
                            color = DarkTeal,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            // Real-time Date and Time Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = currentDateTime,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
            }
            
            // Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // New entry header (moved to top)
                Text(
                    text = "নতুন তথ্য যোগ করুন",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                // First Section: আজকের সিলিন্ডার বিক্রি
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "আজকের সিলিন্ডার বিক্রি",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                        
                        OutlinedTextField(
                            value = cylinderCount,
                            onValueChange = { newValue ->
                                // Only allow numeric input
                                if (newValue.all { it.isDigit() }) {
                                    cylinderCount = newValue
                                    Log.d("CashDepositInput", "Cylinder count changed: '$newValue'")
                                }
                            },
                            placeholder = { 
                                if (cylinderCount.isEmpty()) {
                                    Text("বোতল এর সংখ্যা")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                focusedLabelColor = LightTeal
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number
                            ),
                            singleLine = true
                        )
                    }
                }
                
                // Second Section: টাকার পরিমাণ
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "টাকার পরিমাণ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                        
                        OutlinedTextField(
                            value = amountValue,
                            onValueChange = { newValue ->
                                // Allow numeric input with decimal point
                                if (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1) {
                                    amountValue = newValue
                                    Log.d("CashDepositInput", "Amount changed: '$newValue'")
                                }
                            },
                            placeholder = { 
                                if (amountValue.isEmpty()) {
                                    Text("টাকার পরিমাণ লিখুন")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                focusedLabelColor = LightTeal
                            ),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Text(
                                    text = "৳",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTeal,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        )
                    }
                }
                
                // Summary Section (if both fields have data)
                if (cylinderCount.isNotBlank() && amountValue.isNotBlank()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = LightTeal.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "সারসংক্ষেপ",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "সিলিন্ডার:",
                                    fontSize = 14.sp,
                                    color = DarkTeal
                                )
                                Text(
                                    text = "$cylinderCount টি",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkTeal
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "মোট টাকা:",
                                    fontSize = 14.sp,
                                    color = DarkTeal
                                )
                                Text(
                                    text = "৳ $amountValue",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkTeal
                                )
                            }
                        }
                    }
                }
                
                // Save Button
                Button(
                    onClick = {
                        cashDepositViewModel.saveCashDeposit(cylinderCount, amountValue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = cylinderCount.isNotBlank() && amountValue.isNotBlank() && saveStatus !is CashDepositSaveStatus.Saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (cylinderCount.isNotBlank() && amountValue.isNotBlank()) DarkTeal else Color.Gray,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (saveStatus is CashDepositSaveStatus.Saving) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "সংরক্ষণ হচ্ছে...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else {
                        Text(
                            text = "সংরক্ষণ করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Save Status Messages
                when (saveStatus) {
                    is CashDepositSaveStatus.Saved -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "✓ সফলভাবে সংরক্ষিত হয়েছে",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Green.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is CashDepositSaveStatus.Error -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚠ ${(saveStatus as CashDepositSaveStatus.Error).message}",
                                modifier = Modifier.padding(16.dp),
                                color = Color.Red.copy(alpha = 0.8f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    else -> {}
                }
                
                // Saved cash deposits section (moved to bottom)
                if (isLoading) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = DarkTeal
                                )
                                Text(
                                    text = "তথ্য লোড হচ্ছে...",
                                    color = DarkTeal,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                } else if (cashDeposits.isNotEmpty()) {
                    // Saved entries header
                    Text(
                        text = "সংরক্ষিত তথ্য",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                    )
                    
                    // Display saved entries
                    cashDeposits.forEach { deposit ->
                        CashDepositEntryCard(
                            deposit = deposit,
                            onEdit = { editedDeposit ->
                                cashDepositViewModel.editCashDeposit(
                                    editedDeposit.id,
                                    editedDeposit.cylinderCount,
                                    editedDeposit.amount
                                )
                            },
                            onDelete = { depositId ->
                                cashDepositViewModel.deleteCashDeposit(depositId)
                            }
                        )
                    }
                }
                
                // Add some bottom padding
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun CashDepositEntryCard(
    deposit: CashDepositEntry,
    onEdit: (CashDepositEntry) -> Unit,
    onDelete: (String) -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Date and time row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = deposit.date,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Text(
                    text = deposit.time,
                    fontSize = 14.sp,
                    color = DarkTeal.copy(alpha = 0.7f)
                )
            }
            
            Divider(
                color = LightTeal.copy(alpha = 0.3f),
                thickness = 1.dp
            )
            
            // Cylinder count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "বোতল এর সংখ্যা:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Text(
                    text = if (deposit.cylinderCount.isNotEmpty()) deposit.cylinderCount else "নাই",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (deposit.cylinderCount.isNotEmpty()) DarkTeal else DarkTeal.copy(alpha = 0.5f)
                )
            }
            
            // Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "টাকার পরিমাণ:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Text(
                    text = if (deposit.amount.isNotEmpty()) "৳ ${deposit.amount}" else "নাই",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (deposit.amount.isNotEmpty()) DarkTeal else DarkTeal.copy(alpha = 0.5f)
                )
            }
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DarkTeal
                    ),
                    border = BorderStroke(1.dp, DarkTeal)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সম্পাদনা")
                }
                
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    ),
                    border = BorderStroke(1.dp, Color.Red)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মুছুন")
                }
            }
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        EditCashDepositDialog(
            deposit = deposit,
            onDismiss = { showEditDialog = false },
            onConfirm = { editedDeposit ->
                onEdit(editedDeposit)
                showEditDialog = false
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "নিশ্চিত করুন",
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
            },
            text = {
                Text(
                    text = "আপনি কি এই ক্যাশ ডিপোজিট এন্ট্রি মুছে ফেলতে চান?",
                    color = DarkTeal
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete(deposit.id)
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red
                    )
                ) {
                    Text("মুছুন", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false }
                ) {
                    Text("বাতিল", color = DarkTeal)
                }
            }
        )
    }
}

@Composable
fun EditCashDepositDialog(
    deposit: CashDepositEntry,
    onDismiss: () -> Unit,
    onConfirm: (CashDepositEntry) -> Unit
) {
    var editedCylinderCount by remember { mutableStateOf(deposit.cylinderCount) }
    var editedAmount by remember { mutableStateOf(deposit.amount) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ক্যাশ ডিপোজিট সম্পাদনা",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                // Cylinder Count Field
                OutlinedTextField(
                    value = editedCylinderCount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() }) {
                            editedCylinderCount = newValue
                        }
                    },
                    label = { Text("সিলিন্ডার সংখ্যা") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        focusedLabelColor = LightTeal
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )
                
                // Amount Field
                OutlinedTextField(
                    value = editedAmount,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() || it == '.' } && newValue.count { it == '.' } <= 1) {
                            editedAmount = newValue
                        }
                    },
                    label = { Text("টাকার পরিমাণ") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        focusedLabelColor = LightTeal
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal
                    ),
                    singleLine = true,
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                )
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল", color = DarkTeal)
                    }
                    
                    Button(
                        onClick = {
                            if (editedCylinderCount.isNotBlank() && editedAmount.isNotBlank()) {
                                val editedDeposit = deposit.copy(
                                    cylinderCount = editedCylinderCount,
                                    amount = editedAmount,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onConfirm(editedDeposit)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = editedCylinderCount.isNotBlank() && editedAmount.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal
                        )
                    ) {
                        Text("সংরক্ষণ", color = Color.White)
                    }
                }
            }
        }
    }
}

// Get current date and time in Bengali format
private fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy - hh:mm:ss a", Locale("bn", "BD"))
    return sdf.format(Date())
}
