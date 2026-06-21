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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@Composable
fun OrderManagementScreen(
    onBackClick: () -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    var showMultiCylinderOrderDialog by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var showOrderDetails by remember { mutableStateOf(false) }
    var showStatusUpdateDialog by remember { mutableStateOf(false) }
    
    val orders by orderViewModel.filteredOrders.collectAsState()
    val orderStats by orderViewModel.orderStats.collectAsState()
    val isLoading by orderViewModel.isLoading.collectAsState()
    val errorMessage by orderViewModel.errorMessage.collectAsState()
    val successMessage by orderViewModel.successMessage.collectAsState()
    val orderFilter by orderViewModel.orderFilter.collectAsState()
    
    // Handle messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Error message will be shown in UI
        }
    }
    
    LaunchedEffect(successMessage) {
        successMessage?.let {
            orderViewModel.clearSuccess()
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            // Top App Bar
            item {
                OrderTopAppBar(
                    onBackClick = onBackClick,
                    onAddClick = { showMultiCylinderOrderDialog = true },
                    onSearchQueryChange = { orderViewModel.searchOrders(it) },
                    searchQuery = orderFilter.searchQuery
                )
            }
            
            // Statistics Cards
            item {
                OrderStatsSection(stats = orderStats)
            }
            
            // Filter Chips
            item {
                OrderFilterChips(
                    selectedStatus = orderFilter.orderStatus,
                    selectedPriority = orderFilter.priority,
                    onStatusSelected = { orderViewModel.filterByStatus(it) },
                    onPrioritySelected = { orderViewModel.filterByPriority(it) },
                    onClearFilters = { orderViewModel.clearFilters() }
                )
            }
            
            // Order List or Loading
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else {
                items(orders) { order ->
                    OrderCard(
                        order = order,
                        onClick = {
                            selectedOrder = order
                            showOrderDetails = true
                        },
                        onStatusClick = {
                            selectedOrder = order
                            showStatusUpdateDialog = true
                        },
                        onEditClick = {
                            selectedOrder = order
                            showMultiCylinderOrderDialog = true
                        },
                        onDeleteClick = {
                            orderViewModel.deleteOrder(order.id)
                        }
                    )
                }
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
                    IconButton(onClick = { orderViewModel.clearError() }) {
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
    

    
    // Order Details Dialog
    if (showOrderDetails && selectedOrder != null) {
        OrderDetailsDialog(
            order = selectedOrder!!,
            onDismiss = {
                showOrderDetails = false
                selectedOrder = null
            },
            onEdit = {
                showOrderDetails = false
                showMultiCylinderOrderDialog = true
            },
            onStatusUpdate = {
                showOrderDetails = false
                showStatusUpdateDialog = true
            }
        )
    }
    
    // Status Update Dialog
    if (showStatusUpdateDialog && selectedOrder != null) {
        OrderStatusUpdateDialog(
            order = selectedOrder!!,
            onDismiss = {
                showStatusUpdateDialog = false
                selectedOrder = null
            },
            onStatusUpdate = { orderId, orderStatus, paymentStatus, deliveryStatus, paidAmount ->
                orderViewModel.updateOrderStatus(orderId, orderStatus)
                if (paymentStatus != null) {
                    orderViewModel.updatePaymentStatus(orderId, paymentStatus, paidAmount)
                }
                if (deliveryStatus != null) {
                    orderViewModel.updateDeliveryStatus(orderId, deliveryStatus)
                }
                showStatusUpdateDialog = false
                selectedOrder = null
            }
        )
    }
    
    // Multi-Cylinder Order Dialog with Customer Suggestions
    if (showMultiCylinderOrderDialog) {
        EnhancedMultiCylinderOrderDialog(
            existingOrder = selectedOrder,
            onDismiss = {
                showMultiCylinderOrderDialog = false
                selectedOrder = null
            },
            onOrderCreated = {
                showMultiCylinderOrderDialog = false
                selectedOrder = null
                // Refresh orders list
                orderViewModel.refreshOrders()
            },
            orderViewModel = orderViewModel
        )
    }
}

@Composable
private fun OrderTopAppBar(
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
                text = "অর্ডার ব্যবস্থাপনা",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            IconButton(onClick = onAddClick) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Add Order",
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
                        text = "অর্ডার নম্বর, গ্রাহকের নাম বা ফোন দিয়ে খুঁজুন...",
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
private fun OrderStatsSection(stats: OrderStats) {
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
            OrderStatCard(
                modifier = Modifier.weight(1f),
                title = "মোট অর্ডার",
                value = stats.totalOrders.toString(),
                icon = Icons.Filled.ShoppingCart,
                color = LightTeal
            )
            
            OrderStatCard(
                modifier = Modifier.weight(1f),
                title = "অপেক্ষমান",
                value = stats.pendingOrders.toString(),
                icon = Icons.Filled.Info,
                color = Color(0xFFFF9800)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderStatCard(
                modifier = Modifier.weight(1f),
                title = "সম্পন্ন",
                value = stats.deliveredOrders.toString(),
                icon = Icons.Filled.CheckCircle,
                color = Color.Green
            )
            
            OrderStatCard(
                modifier = Modifier.weight(1f),
                title = "আজকের অর্ডার",
                value = stats.todaysOrders.toString(),
                icon = Icons.Filled.DateRange,
                color = Color(0xFF2196F3)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderStatCard(
                modifier = Modifier.weight(1f),
                title = "মোট আয়",
                value = "৳${stats.totalRevenue.toInt()}",
                icon = Icons.Filled.Star,
                color = Color(0xFF4CAF50)
            )
            
            OrderStatCard(
                modifier = Modifier.weight(1f),
                title = "বকেয়া",
                value = "৳${stats.pendingPayments.toInt()}",
                icon = Icons.Filled.Warning,
                color = Color(0xFFF44336)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun OrderStatCard(
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
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Text(
                text = title,
                fontSize = 11.sp,
                color = DarkTeal.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OrderFilterChips(
    selectedStatus: OrderStatus?,
    selectedPriority: OrderPriority?,
    onStatusSelected: (OrderStatus?) -> Unit,
    onPrioritySelected: (OrderPriority?) -> Unit,
    onClearFilters: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Status Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                onClick = { onStatusSelected(null) },
                label = { Text("সব অর্ডার", fontSize = 12.sp) },
                selected = selectedStatus == null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = DarkTeal,
                    containerColor = Color.White.copy(alpha = 0.7f),
                    labelColor = Color.White
                )
            )
            
            FilterChip(
                onClick = { onStatusSelected(OrderStatus.PENDING) },
                label = { Text("অপেক্ষমান", fontSize = 12.sp) },
                selected = selectedStatus == OrderStatus.PENDING,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = DarkTeal,
                    containerColor = Color.White.copy(alpha = 0.7f),
                    labelColor = Color.White
                )
            )
            
            FilterChip(
                onClick = { onStatusSelected(OrderStatus.CONFIRMED) },
                label = { Text("নিশ্চিত", fontSize = 12.sp) },
                selected = selectedStatus == OrderStatus.CONFIRMED,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = DarkTeal,
                    containerColor = Color.White.copy(alpha = 0.7f),
                    labelColor = Color.White
                )
            )
            
            FilterChip(
                onClick = { onStatusSelected(OrderStatus.DELIVERED) },
                label = { Text("সম্পন্ন", fontSize = 12.sp) },
                selected = selectedStatus == OrderStatus.DELIVERED,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.White,
                    selectedLabelColor = DarkTeal,
                    containerColor = Color.White.copy(alpha = 0.7f),
                    labelColor = Color.White
                )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Priority Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OrderPriority.values().forEach { priority ->
                FilterChip(
                    onClick = { onPrioritySelected(if (selectedPriority == priority) null else priority) },
                    label = { Text(priority.displayNameBn, fontSize = 12.sp) },
                    selected = selectedPriority == priority,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = DarkTeal,
                        containerColor = Color.White.copy(alpha = 0.7f),
                        labelColor = Color.White
                    )
                )
            }
            
            // Clear Filters
            if (selectedStatus != null || selectedPriority != null) {
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
private fun OrderList(
    orders: List<Order>,
    onOrderClick: (Order) -> Unit,
    onStatusClick: (Order) -> Unit,
    onEditClick: (Order) -> Unit,
    onDeleteClick: (Order) -> Unit
) {
    if (orders.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.ShoppingCart,
                    contentDescription = "No Orders",
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "কোন অর্ডার পাওয়া যায়নি",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "নতুন অর্ডার যোগ করুন বা অনুসন্ধান পরিবর্তন করুন",
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
            items(orders) { order ->
                OrderCard(
                    order = order,
                    onClick = { onOrderClick(order) },
                    onStatusClick = { onStatusClick(order) },
                    onEditClick = { onEditClick(order) },
                    onDeleteClick = { onDeleteClick(order) }
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
private fun OrderCard(
    order: Order,
    onClick: () -> Unit,
    onStatusClick: () -> Unit,
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
                Column {
                    Text(
                        text = order.orderNumber,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = order.orderDate,
                        fontSize = 12.sp,
                        color = DarkTeal.copy(alpha = 0.6f)
                    )
                }
                
                Row {
                    // Priority Indicator
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(getPriorityColor(order.priority))
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Action Buttons
                    IconButton(
                        onClick = onStatusClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Update Status",
                            tint = DarkTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
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
            
            // Customer Info
            Text(
                text = order.customerName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = DarkTeal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Text(
                text = order.customerPhone,
                fontSize = 14.sp,
                color = DarkTeal.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Order Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getOrderStatusColor(order.orderStatus).copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = order.orderStatus.displayNameBn,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = getOrderStatusColor(order.orderStatus)
                    )
                }
                
                Text(
                    text = "৳${order.totalAmount.toInt()}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
            }
            
            // Items Summary
            if (order.orderItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${order.orderItems.size} টি পণ্য - ${order.orderItems.sumOf { it.quantity }} টি সিলিন্ডার",
                    fontSize = 12.sp,
                    color = DarkTeal.copy(alpha = 0.7f)
                )
            }
            
            // Payment Status
            if (order.remainingAmount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "বাকি: ৳${order.remainingAmount.toInt()}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Red
                )
            }
        }
    }
}
