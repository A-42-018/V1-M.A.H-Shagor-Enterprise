package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(
    expenseViewModel: ExpenseViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val expenses by expenseViewModel.expenses.collectAsState()
    val saveStatus by expenseViewModel.saveStatus.collectAsState()
    val isLoading by expenseViewModel.isLoading.collectAsState()
    
    // Form state variables
    var selectedExpenseType by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var selectedPaymentMethod by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    // Dropdown states
    var expenseTypeExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    
    // Edit dialog state
    var showEditDialog by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<ExpenseEntry?>(null) }
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expenseToDelete by remember { mutableStateOf<ExpenseEntry?>(null) }
    
    // Clear form after successful save
    LaunchedEffect(saveStatus) {
        if (saveStatus is ExpenseSaveStatus.Saved) {
            selectedExpenseType = ""
            description = ""
            amount = ""
            selectedPaymentMethod = ""
            vendor = ""
            location = ""
            notes = ""
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "খরচ ট্র্যাকিং",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "পেছনে যান",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF006064)
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF006064),
                            Color(0xFF004D40)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        item {
            // Expense Dashboard
            ExpenseDashboard(expenses = expenses)
        }
        
        item {
            // Form Card
            ExpenseFormCard(
                selectedExpenseType = selectedExpenseType,
                onExpenseTypeChange = { selectedExpenseType = it },
                description = description,
                onDescriptionChange = { description = it },
                amount = amount,
                onAmountChange = { amount = it },
                selectedPaymentMethod = selectedPaymentMethod,
                onPaymentMethodChange = { selectedPaymentMethod = it },
                vendor = vendor,
                onVendorChange = { vendor = it },
                location = location,
                onLocationChange = { location = it },
                notes = notes,
                onNotesChange = { notes = it },
                expenseTypeExpanded = expenseTypeExpanded,
                onExpenseTypeExpandedChange = { expenseTypeExpanded = it },
                paymentMethodExpanded = paymentMethodExpanded,
                onPaymentMethodExpandedChange = { paymentMethodExpanded = it },
                isLoading = isLoading,
                saveStatus = saveStatus,
                onSave = {
                    expenseViewModel.saveExpense(
                        expenseType = selectedExpenseType,
                        description = description,
                        amount = amount,
                        paymentMethod = selectedPaymentMethod,
                        vendor = vendor,
                        location = location,
                        notes = notes
                    )
                }
            )
        }
        
        item {
            // Expense History Card
            ExpenseHistoryCard(
                expenses = expenses,
                onEdit = { expense ->
                    editingExpense = expense
                    showEditDialog = true
                },
                onDelete = { expense ->
                    expenseToDelete = expense
                    showDeleteDialog = true
                }
            )
        }
    }
    }
    
    // Edit Dialog
    if (showEditDialog && editingExpense != null) {
        EditExpenseDialog(
            expense = editingExpense!!,
            onDismiss = { 
                showEditDialog = false
                editingExpense = null
            },
            onSave = { updatedExpense ->
                expenseViewModel.editExpense(
                    expenseId = updatedExpense.id,
                    expenseType = updatedExpense.expenseType,
                    description = updatedExpense.description,
                    amount = updatedExpense.amount,
                    paymentMethod = updatedExpense.paymentMethod,
                    vendor = updatedExpense.vendor,
                    location = updatedExpense.location,
                    notes = updatedExpense.notes
                )
                showEditDialog = false
                editingExpense = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                expenseToDelete = null
            },
            title = { Text("নিশ্চিত করুন") },
            text = { Text("আপনি কি এই খরচের তথ্যটি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        expenseViewModel.deleteExpense(expenseToDelete!!.id)
                        showDeleteDialog = false
                        expenseToDelete = null
                    }
                ) {
                    Text("হ্যাঁ", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        expenseToDelete = null
                    }
                ) {
                    Text("না")
                }
            }
        )
    }
}

@Composable
fun ExpenseDashboard(expenses: List<ExpenseEntry>) {
    val totalExpense = expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val monthlyExpense = expenses.filter {
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)
        
        calendar.timeInMillis = it.createdAt
        calendar.get(Calendar.MONTH) == currentMonth && calendar.get(Calendar.YEAR) == currentYear
    }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "খরচের সারসংক্ষেপ",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006064),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Total Expense Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "মোট খরচ",
                            fontSize = 12.sp,
                            color = Color(0xFFE91E63),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${String.format("%.0f", totalExpense)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFAD1457)
                        )
                    }
                }
                
                // Monthly Expense Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "এই মাসে",
                            fontSize = 12.sp,
                            color = Color(0xFFFF9800),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${String.format("%.0f", monthlyExpense)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quick Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "মোট এন্ট্রি: ${expenses.size}টি",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
                Text(
                    text = "গড় খরচ: ৳${if (expenses.isNotEmpty()) String.format("%.0f", totalExpense / expenses.size) else "0"}",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormCard(
    selectedExpenseType: String,
    onExpenseTypeChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    selectedPaymentMethod: String,
    onPaymentMethodChange: (String) -> Unit,
    vendor: String,
    onVendorChange: (String) -> Unit,
    location: String,
    onLocationChange: (String) -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    expenseTypeExpanded: Boolean,
    onExpenseTypeExpandedChange: (Boolean) -> Unit,
    paymentMethodExpanded: Boolean,
    onPaymentMethodExpandedChange: (Boolean) -> Unit,
    isLoading: Boolean,
    saveStatus: ExpenseSaveStatus,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "নতুন খরচ যোগ করুন",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006064),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Expense Type Dropdown (Mandatory)
            ExposedDropdownMenuBox(
                expanded = expenseTypeExpanded,
                onExpandedChange = onExpenseTypeExpandedChange
            ) {
                OutlinedTextField(
                    value = selectedExpenseType,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("খরচের ধরন *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseTypeExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = expenseTypeExpanded,
                    onDismissRequest = { onExpenseTypeExpandedChange(false) }
                ) {
                    ExpenseType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type.displayNameBn) },
                            onClick = {
                                onExpenseTypeChange(type.displayNameBn)
                                onExpenseTypeExpandedChange(false)
                            }
                        )
                    }
                }
            }
            
            // Description (Mandatory)
            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                label = { Text("বিবরণ *") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1)
                )
            )
            
            // Amount (Mandatory)
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("পরিমাণ (টাকা) *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1)
                )
            )
            
            // Payment Method Dropdown (Mandatory)
            ExposedDropdownMenuBox(
                expanded = paymentMethodExpanded,
                onExpandedChange = onPaymentMethodExpandedChange
            ) {
                OutlinedTextField(
                    value = selectedPaymentMethod,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("পেমেন্ট পদ্ধতি *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                ExposedDropdownMenu(
                    expanded = paymentMethodExpanded,
                    onDismissRequest = { onPaymentMethodExpandedChange(false) }
                ) {
                    PaymentMethod.values().forEach { method ->
                        DropdownMenuItem(
                            text = { Text(method.displayNameBn) },
                            onClick = {
                                onPaymentMethodChange(method.displayNameBn)
                                onPaymentMethodExpandedChange(false)
                            }
                        )
                    }
                }
            }
            
            // Vendor (Optional)
            OutlinedTextField(
                value = vendor,
                onValueChange = onVendorChange,
                label = { Text("বিক্রেতা/সরবরাহকারী") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1)
                )
            )
            
            // Location (Optional)
            OutlinedTextField(
                value = location,
                onValueChange = onLocationChange,
                label = { Text("স্থান") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1)
                )
            )
            
            // Notes (Optional)
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("নোট") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                minLines = 2,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00ACC1),
                    focusedLabelColor = Color(0xFF00ACC1)
                )
            )
            
            // Mandatory fields notice
            Text(
                text = "* প্রথম ৪টি ক্ষেত্র বাধ্যতামূলক",
                fontSize = 12.sp,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Save Button
            Button(
                onClick = onSave,
                enabled = !isLoading && selectedExpenseType.isNotBlank() && 
                         description.isNotBlank() && amount.isNotBlank() && 
                         selectedPaymentMethod.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00ACC1),
                    disabledContainerColor = Color.Gray
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text(
                        text = "সংরক্ষণ করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Status Message
            when (saveStatus) {
                is ExpenseSaveStatus.Saving -> {
                    Text(
                        text = "সংরক্ষণ করা হচ্ছে...",
                        color = Color(0xFF00ACC1),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is ExpenseSaveStatus.Saved -> {
                    Text(
                        text = "সফলভাবে সংরক্ষিত হয়েছে!",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is ExpenseSaveStatus.Error -> {
                    val errorMessage = (saveStatus as ExpenseSaveStatus.Error).message
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun ExpenseHistoryCard(
    expenses: List<ExpenseEntry>,
    onEdit: (ExpenseEntry) -> Unit,
    onDelete: (ExpenseEntry) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "খরচের ইতিহাস (${expenses.size}টি এন্ট্রি)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006064),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (expenses.isEmpty()) {
                Text(
                    text = "কোনো খরচের তথ্য নেই",
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses) { expense ->
                        ExpenseCard(
                            expense = expense,
                            onEdit = { onEdit(expense) },
                            onDelete = { onDelete(expense) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseEntry,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.expenseType,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006064)
                    )
                    Text(
                        text = expense.description,
                        fontSize = 14.sp,
                        color = Color(0xFF666666),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Text(
                    text = "৳${expense.amount}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE91E63)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "পেমেন্ট: ${expense.paymentMethod}",
                        fontSize = 12.sp,
                        color = Color(0xFF666666)
                    )
                    if (expense.vendor.isNotBlank() && expense.vendor != "অজানা") {
                        Text(
                            text = "বিক্রেতা: ${expense.vendor}",
                            fontSize = 12.sp,
                            color = Color(0xFF666666)
                        )
                    }
                }
                
                Row {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "সম্পাদনা",
                            tint = Color(0xFF00ACC1),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "মুছুন",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            // Date
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("bn", "BD"))
            Text(
                text = dateFormat.format(Date(expense.createdAt)),
                fontSize = 11.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
