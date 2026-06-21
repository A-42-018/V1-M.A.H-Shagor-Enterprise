package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DueAccountScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: DueAccountViewModel = viewModel { DueAccountViewModel(context.applicationContext as android.app.Application) }
    val customerViewModel: CustomerViewModel = viewModel { CustomerViewModel(context.applicationContext as android.app.Application) }
    val salesViewModel: SalesViewModel = viewModel { SalesViewModel(context.applicationContext as android.app.Application) }
    
    val dueEntries by viewModel.filteredEntries.collectAsState()
    val dueSummary by viewModel.dueSummary.collectAsState()
    val customerBalances by viewModel.customerBalances.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()
    val customers by customerViewModel.customers.collectAsState()
    val salesWithDue by salesViewModel.sales.collectAsState()
    val pendingSales = remember(salesWithDue) { 
        salesWithDue.filter { order -> order.remainingAmount > 0 }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf<DueAccountEntry?>(null) }
    var showDeleteDialog by remember { mutableStateOf<DueAccountEntry?>(null) }
    var showCustomerDetailsDialog by remember { mutableStateOf<CustomerDueBalance?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    // Integrate sales due data for automated dashboard calculation
    LaunchedEffect(salesWithDue) {
        viewModel.integrateSalesDueData(salesWithDue)
    }
    
    // Handle save status
    LaunchedEffect(saveStatus) {
        when (saveStatus) {
            is DueSaveStatus.Success -> {
                // Auto-clear success message after 2 seconds
                kotlinx.coroutines.delay(2000)
                viewModel.clearSaveStatus()
            }
            is DueSaveStatus.Error -> {
                // Auto-clear error message after 3 seconds
                kotlinx.coroutines.delay(3000)
                viewModel.clearSaveStatus()
            }
            else -> {}
        }
    }

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
        DueAccountTopAppBar(
            onBackClick = onBackClick,
            onAddClick = { showAddDialog = true }
        )

        // Summary Cards
        DueSummarySection(dueSummary = dueSummary)

        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    Modifier,
                    color = Color.White
                )
            }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("লেনদেনের তালিকা", color = Color.White) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("কাস্টমার বাকী", color = Color.White) }
            )
        }

        // Content based on selected tab
        when (selectedTab) {
            0 -> {
                // Transactions List
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    // Filter Chips
                    Box(modifier = Modifier.padding(16.dp)) {
                        DueFilterChips(
                            selectedFilter = selectedFilter,
                            onFilterSelected = { viewModel.setFilter(it) }
                        )
                    }

                    // Single LazyColumn for all content
                    if (dueEntries.isEmpty() && pendingSales.isEmpty()) {
                        // Show empty state
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyDueState()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            // Show pending sales first
                            if (pendingSales.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "বিক্রয়ের বকেয়া",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(pendingSales) { sale ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { /* Handle sale click */ },
                                        colors = CardDefaults.cardColors(
                                            containerColor = LightTeal.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sale.customerName,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = DarkTeal
                                                )
                                                Text(
                                                    text = "৳${formatCurrencyBengali(sale.remainingAmount)}",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "ফোন: ${sale.customerPhone}",
                                                    fontSize = 12.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                            Column {
                                                Text(
                                                    text = "অর্ডার #${sale.orderNumber}",
                                                    fontSize = 10.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Show regular due entries
                            if (dueEntries.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "অন্যান্য বকেয়া",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                                items(dueEntries) { entry ->
                                    DueEntryCard(
                                        entry = entry,
                                        onClick = { /* Handle click */ },
                                        onEdit = { showEditDialog = entry },
                                        onDelete = { showDeleteDialog = entry }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Customer Due Balances
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                ) {
                    if (customerBalances.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyCustomerDueState()
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(16.dp)
                        ) {
                            items(customerBalances) { balance ->
                                CustomerDueCard(
                                    balance = balance,
                                    onClick = { 
                                        showCustomerDetailsDialog = balance
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Save Status Snackbar
    when (saveStatus) {
        is DueSaveStatus.Success -> {
            LaunchedEffect(saveStatus) {
                // Show success message
            }
        }
        is DueSaveStatus.Error -> {
            LaunchedEffect(saveStatus) {
                // Show error message
            }
        }
        else -> {}
    }

    // Dialogs
    if (showAddDialog) {
        AddDueEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { customerId, customerName, customerPhone, transactionType, amount, description, paymentMethod, notes ->
                viewModel.addDueEntry(
                    customerId = customerId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    transactionType = transactionType,
                    amount = amount,
                    description = description,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
                showAddDialog = false
            },
            customers = customers
        )
    }

    // Edit Dialog
    showEditDialog?.let { entry ->
        AddDueEntryDialog(
            entry = entry,
            onDismiss = { showEditDialog = null },
            onSave = { customerId: String, customerName: String, customerPhone: String, transactionType: DueTransactionType, amount: Double, description: String, paymentMethod: String, notes: String ->
                val updatedEntry = entry.copy(
                    customerId = customerId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    transactionType = transactionType,
                    amount = amount,
                    description = description,
                    paymentMethod = paymentMethod,
                    notes = notes
                )
                viewModel.updateDueEntry(updatedEntry)
                showEditDialog = null
            },
            customers = customers
        )
    }

    // Delete Confirmation Dialog
    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = {
                Text(
                    text = "এন্ট্রি মুছে ফেলুন",
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
            },
            text = {
                Text(
                    text = "আপনি কি নিশ্চিত যে এই এন্ট্রিটি মুছে ফেলতে চান?\n\nগ্রাহক: ${entry.customerName}\nপরিমাণ: ৳${formatCurrencyBengali(entry.amount)}\nবিবরণ: ${entry.description}",
                    color = DarkGray
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteDueEntry(entry.id)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFE53E3E)
                    )
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = DarkTeal
                    )
                ) {
                    Text("বাতিল")
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }

    showCustomerDetailsDialog?.let { customerBalance ->
        val customerEntries = dueEntries.filter { it.customerId == customerBalance.customerId }
        CustomerDueDetailsDialog(
            customerBalance = customerBalance,
            entries = customerEntries,
            onDismiss = { showCustomerDetailsDialog = null },
            onAddPayment = { customerId ->
                // TODO: Open payment dialog for specific customer
                showCustomerDetailsDialog = null
                showAddDialog = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DueAccountTopAppBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "বাকীর হিসেব",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        FloatingActionButton(
            onClick = onAddClick,
            containerColor = Color.White,
            contentColor = DarkTeal,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "Add Due Entry"
            )
        }
    }
}

@Composable
private fun DueSummarySection(dueSummary: DueSummary) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "বাকীর সারসংক্ষেপ",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "মোট বাকী",
                    value = "৳${formatCurrencyBengali(dueSummary.totalDueAmount)}",
                    icon = Icons.Filled.AccountBox,
                    color = Color(0xFFE53E3E),
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "বাকীদার কাস্টমার",
                    value = "${dueSummary.totalCustomersWithDue}",
                    icon = Icons.Filled.Person,
                    color = Color(0xFFED8936),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "আজকের পেমেন্ট",
                    value = "৳${formatCurrencyBengali(dueSummary.totalPaymentsToday)}",
                    icon = Icons.Filled.Star,
                    color = Color(0xFF38A169),
                    modifier = Modifier.weight(1f)
                )

                SummaryCard(
                    title = "আজকের বাকী",
                    value = "৳${formatCurrencyBengali(dueSummary.totalDueGivenToday)}",
                    icon = Icons.Filled.KeyboardArrowUp,
                    color = Color(0xFF3182CE),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = DarkGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun DueFilterChips(
    selectedFilter: DueAccountFilter,
    onFilterSelected: (DueAccountFilter) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DueAccountFilter.values().forEach { filter ->
            FilterChip(
                selected = selectedFilter == filter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = LightTeal,
                    selectedLabelColor = Color.White,
                    containerColor = Color.White,
                    labelColor = DarkTeal
                )
            )
        }
    }
}

@Composable
private fun DueEntryCard(
    entry: DueAccountEntry,
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = entry.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = entry.customerPhone,
                        fontSize = 12.sp,
                        color = DarkGray
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "৳${formatCurrencyBengali(entry.amount)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (entry.transactionType) {
                            DueTransactionType.CREDIT -> Color(0xFFE53E3E)
                            DueTransactionType.PAYMENT -> Color(0xFF38A169)
                            DueTransactionType.ADJUSTMENT -> Color(0xFF3182CE)
                        }
                    )
                    Text(
                        text = entry.transactionType.displayNameBengali,
                        fontSize = 12.sp,
                        color = when (entry.transactionType) {
                            DueTransactionType.CREDIT -> Color(0xFFE53E3E)
                            DueTransactionType.PAYMENT -> Color(0xFF38A169)
                            DueTransactionType.ADJUSTMENT -> Color(0xFF3182CE)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = entry.description,
                fontSize = 14.sp,
                color = DarkGray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatDateBengali(entry.date),
                        fontSize = 12.sp,
                        color = DarkGray.copy(alpha = 0.7f)
                    )
                    Text(
                        text = entry.paymentMethod,
                        fontSize = 12.sp,
                        color = DarkGray.copy(alpha = 0.7f)
                    )
                }
                
                // Edit and Delete buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "সম্পাদনা",
                            tint = Color(0xFF3182CE),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "মুছে ফেলুন",
                            tint = Color(0xFFE53E3E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerDueCard(
    balance: CustomerDueBalance,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    text = balance.customerName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                Text(
                    text = balance.customerPhone,
                    fontSize = 12.sp,
                    color = DarkGray
                )
                Text(
                    text = "${balance.transactionCount} টি লেনদেন",
                    fontSize = 12.sp,
                    color = DarkGray.copy(alpha = 0.7f)
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "৳${formatCurrencyBengali(balance.totalDueAmount)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (balance.totalDueAmount > 0) Color(0xFFE53E3E) else Color(0xFF38A169)
                )
                Text(
                    text = "বাকী",
                    fontSize = 12.sp,
                    color = DarkGray
                )
            }
        }
    }
}

@Composable
private fun EmptyDueState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.AccountBox,
            contentDescription = null,
            tint = DarkGray.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "কোনো বাকীর এন্ট্রি নেই",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = DarkGray,
            textAlign = TextAlign.Center
        )
        Text(
            text = "নতুন বাকীর এন্ট্রি যোগ করতে + বাটনে ক্লিক করুন",
            fontSize = 14.sp,
            color = DarkGray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun EmptyCustomerDueState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Person,
            contentDescription = null,
            tint = DarkGray.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "কোনো কাস্টমারের বাকী নেই",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = DarkGray,
            textAlign = TextAlign.Center
        )
        Text(
            text = "সব কাস্টমারের পেমেন্ট সম্পন্ন হয়েছে",
            fontSize = 14.sp,
            color = DarkGray.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Utility function to format date in Bengali
private fun formatDateBengali(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale("bn", "BD"))
    return sdf.format(Date(timestamp))
}
