package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@Composable
fun CustomerManagementScreen(
    onBackClick: () -> Unit,
    customerViewModel: CustomerViewModel = viewModel()
) {
    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var showCustomerDetails by remember { mutableStateOf(false) }
    
    val customers by customerViewModel.filteredCustomers.collectAsState()
    val customerStats by customerViewModel.customerStats.collectAsState()
    val isLoading by customerViewModel.isLoading.collectAsState()
    val errorMessage by customerViewModel.errorMessage.collectAsState()
    val successMessage by customerViewModel.successMessage.collectAsState()
    val searchQuery by customerViewModel.searchQuery.collectAsState()
    val selectedType by customerViewModel.selectedCustomerType.collectAsState()
    
    // Handle messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Error message will be shown in UI
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            customerViewModel.clearSuccess()
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
            CustomerTopAppBar(
                onBackClick = onBackClick,
                onAddClick = { showAddCustomerDialog = true },
                onSearchQueryChange = { customerViewModel.searchCustomers(it) },
                searchQuery = searchQuery
            )
            
            // Statistics Cards
            CustomerStatsSection(stats = customerStats)
            
            // Filter Chips
            CustomerFilterChips(
                selectedType = selectedType,
                onTypeSelected = { customerViewModel.filterByCustomerType(it) },
                onClearFilters = { customerViewModel.clearFilters() }
            )
            
            // Customer List
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            } else {
                CustomerList(
                    customers = customers,
                    onCustomerClick = { customer ->
                        selectedCustomer = customer
                        showCustomerDetails = true
                    },
                    onEditClick = { customer ->
                        selectedCustomer = customer
                        showAddCustomerDialog = true
                    },
                    onDeleteClick = { customer ->
                        customerViewModel.deleteCustomer(customer.id)
                    }
                )
            }
        }
        
        // Error/Success Messages
        errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = { customerViewModel.clearError() }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }
            }
        }
        
        successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Green.copy(alpha = 0.9f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Success",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
    
    // Add/Edit Customer Dialog
    if (showAddCustomerDialog) {
        AddEditCustomerDialog(
            customer = selectedCustomer,
            onDismiss = {
                showAddCustomerDialog = false
                selectedCustomer = null
            },
            onSave = { customer ->
                if (selectedCustomer != null) {
                    customerViewModel.updateCustomer(customer)
                } else {
                    customerViewModel.addCustomer(
                        name = customer.name,
                        phone = customer.phone,
                        address = customer.address,
                        area = customer.area,
                        customerType = customer.customerType,
                        creditLimit = customer.creditLimit,
                        email = customer.email,
                        notes = customer.notes
                    )
                }
                showAddCustomerDialog = false
                selectedCustomer = null
            }
        )
    }
    
    // Customer Details Dialog
    if (showCustomerDetails && selectedCustomer != null) {
        CustomerDetailsDialog(
            customer = selectedCustomer!!,
            onDismiss = {
                showCustomerDetails = false
                selectedCustomer = null
            },
            onEdit = {
                showCustomerDetails = false
                showAddCustomerDialog = true
            }
        )
    }
}

@Composable
private fun CustomerTopAppBar(
    onBackClick: () -> Unit,
    onAddClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    searchQuery: String
) {
    Column {
        // Header Row
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
                text = "গ্রাহক ব্যবস্থাপনা",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Customer",
                    tint = Color.White
                )
            }
        }
        
        // Search Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "নাম, ফোন বা এলাকা দিয়ে খুঁজুন...",
                        color = DarkTeal.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = DarkTeal
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "Clear",
                                tint = DarkTeal
                            )
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DarkTeal,
                    unfocusedBorderColor = DarkTeal.copy(alpha = 0.5f),
                    focusedTextColor = DarkTeal,
                    unfocusedTextColor = DarkTeal
                ),
                singleLine = true
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun CustomerStatsSection(stats: CustomerStats) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "মোট গ্রাহক",
                value = stats.totalCustomers.toString(),
                icon = Icons.Filled.Person,
                color = LightTeal
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                title = "সক্রিয় গ্রাহক",
                value = stats.activeCustomers.toString(),
                icon = Icons.Filled.CheckCircle,
                color = Color.Green
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "ভিআইপি গ্রাহক",
                value = stats.vipCustomers.toString(),
                icon = Icons.Filled.Star,
                color = Color(0xFFFFD700)
            )
            
            StatCard(
                modifier = Modifier.weight(1f),
                title = "মোট বাকি",
                value = "৳${stats.totalCredit.toInt()}",
                icon = Icons.Filled.Star,
                color = Color(0xFFFF9800)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Text(
                text = title,
                fontSize = 12.sp,
                color = DarkTeal.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomerFilterChips(
    selectedType: CustomerType?,
    onTypeSelected: (CustomerType?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
                // All Customers Chip
                FilterChip(
                    onClick = { onTypeSelected(null) },
                    label = { Text("সব গ্রাহক") },
                    selected = selectedType == null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = DarkTeal,
                        containerColor = Color.White.copy(alpha = 0.7f),
                        labelColor = Color.White
                    )
                )
                
                // Customer Type Chips
                CustomerType.values().forEach { type ->
                    FilterChip(
                        onClick = { onTypeSelected(type) },
                        label = { Text(type.displayNameBn) },
                        selected = selectedType == type,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = DarkTeal,
                            containerColor = Color.White.copy(alpha = 0.7f),
                            labelColor = Color.White
                        )
                    )
                }
                
                // Clear Filters
                if (selectedType != null) {
                    IconButton(onClick = onClearFilters) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = "Clear Filters",
                            tint = Color.White
                        )
                    }
                }
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun CustomerList(
    customers: List<Customer>,
    onCustomerClick: (Customer) -> Unit,
    onEditClick: (Customer) -> Unit,
    onDeleteClick: (Customer) -> Unit
) {
    if (customers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = "No Customers",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "কোন গ্রাহক পাওয়া যায়নি",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "নতুন গ্রাহক যোগ করুন বা অনুসন্ধান পরিবর্তন করুন",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(customers) { customer ->
                CustomerCard(
                    customer = customer,
                    onClick = { onCustomerClick(customer) },
                    onEditClick = { onEditClick(customer) },
                    onDeleteClick = { onDeleteClick(customer) }
                )
            }
            
            // Bottom padding
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CustomerCard(
    customer: Customer,
    onClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Customer Type Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(
                                when (customer.customerType) {
                                    CustomerType.VIP -> Color(0xFFFFD700)
                                    CustomerType.COMMERCIAL -> Color.Blue

                                    else -> LightTeal
                                }
                            )
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = customer.customerType.displayNameBn,
                        fontSize = 12.sp,
                        color = DarkTeal.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = DarkTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Customer Name
            Text(
                text = customer.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Phone Number
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Phone",
                    tint = DarkTeal.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = customer.phone,
                    fontSize = 14.sp,
                    color = DarkTeal.copy(alpha = 0.8f)
                )
            }
            
            // Area
            if (customer.area.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = "Area",
                        tint = DarkTeal.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customer.area,
                        fontSize = 14.sp,
                        color = DarkTeal.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Credit Info
            if (customer.currentCredit > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "বাকি:",
                        fontSize = 12.sp,
                        color = DarkTeal.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "৳${customer.currentCredit.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                }
            }
            
            // Order Count
            if (customer.totalOrders > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "মোট অর্ডার:",
                        fontSize = 12.sp,
                        color = DarkTeal.copy(alpha = 0.6f)
                    )
                    Text(
                        text = customer.totalOrders.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                }
            }
        }
    }
}
