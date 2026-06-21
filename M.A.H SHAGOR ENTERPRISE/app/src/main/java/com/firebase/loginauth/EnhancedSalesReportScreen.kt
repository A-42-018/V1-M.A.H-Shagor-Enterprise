package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedSalesReportScreen(
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val orderRepository = remember { OrderRepository.getInstance(context) }
    val deliveryRepository = remember { DeliveryRepository(context) }
    val orders by orderRepository.orders.collectAsState()
    val deliveries by deliveryRepository.deliveries.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabOptions = listOf("সারসংক্ষেপ", "চলমান অর্ডার", "সম্পূর্ণ অর্ডার")
    
    // Filter orders based on selected tab
    val filteredOrders = when (selectedTab) {
        1 -> orders.filter { 
            it.orderStatus == OrderStatus.PENDING || 
            it.orderStatus == OrderStatus.CONFIRMED ||
            it.orderStatus == OrderStatus.PROCESSING ||
            it.orderStatus == OrderStatus.READY_FOR_DELIVERY ||
            it.orderStatus == OrderStatus.OUT_FOR_DELIVERY
        }
        2 -> orders.filter { 
            it.orderStatus == OrderStatus.DELIVERED ||
            it.orderStatus == OrderStatus.CANCELLED ||
            it.orderStatus == OrderStatus.RETURNED
        }
        else -> orders
    }
    
    // Calculate summary data
    val totalSales = orders.sumOf { it.totalAmount }
    val totalOrders = orders.size
    val completedOrders = orders.count { it.orderStatus == OrderStatus.DELIVERED }
    val runningOrders = orders.count { 
        it.orderStatus == OrderStatus.PENDING || 
        it.orderStatus == OrderStatus.CONFIRMED ||
        it.orderStatus == OrderStatus.PROCESSING ||
        it.orderStatus == OrderStatus.READY_FOR_DELIVERY ||
        it.orderStatus == OrderStatus.OUT_FOR_DELIVERY
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(DarkTeal, MediumTeal)
                    )
                )
        ) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "বিক্রির হিসাব",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tab Navigation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        LazyRow(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tabOptions.size) { index ->
                                FilterChip(
                                    onClick = { selectedTab = index },
                                    label = {
                                        Text(
                                            text = tabOptions[index],
                                            fontSize = 12.sp,
                                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    selected = selectedTab == index,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color.White.copy(alpha = 0.3f),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color.Transparent,
                                        labelColor = Color.White.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Content based on selected tab
                when (selectedTab) {
                    0 -> {
                        // Summary Statistics
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SalesStatCard(
                                    title = "মোট আয়",
                                    value = "৳${formatCurrency(totalSales)}",
                                    icon = Icons.Filled.Star,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                SalesStatCard(
                                    title = "মোট অর্ডার",
                                    value = totalOrders.toString(),
                                    icon = Icons.Filled.ShoppingCart,
                                    color = Color(0xFF2196F3),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SalesStatCard(
                                    title = "সম্পন্ন অর্ডার",
                                    value = completedOrders.toString(),
                                    icon = Icons.Filled.CheckCircle,
                                    color = Color(0xFF4CAF50),
                                    modifier = Modifier.weight(1f)
                                )
                                SalesStatCard(
                                    title = "চলমান অর্ডার",
                                    value = runningOrders.toString(),
                                    icon = Icons.Filled.Schedule,
                                    color = Color(0xFFFF9800),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    1, 2 -> {
                        // Detailed Order List
                        items(filteredOrders) { order ->
                            DetailedOrderCard(
                                order = order,
                                deliveries = deliveries,
                                isRunningOrder = selectedTab == 1
                            )
                        }
                        
                        if (filteredOrders.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Inbox,
                                            contentDescription = null,
                                            modifier = Modifier.size(64.dp),
                                            tint = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = if (selectedTab == 1) "কোন চলমান অর্ডার নেই" else "কোন সম্পূর্ণ অর্ডার নেই",
                                            fontSize = 16.sp,
                                            color = Color.Gray,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailedOrderCard(
    order: Order,
    deliveries: List<DeliveryEntry>,
    isRunningOrder: Boolean
) {
    // Find related delivery entry
    val relatedDelivery = deliveries.find { it.orderId == order.id }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with Order Number and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "অর্ডার নং: ${order.orderNumber}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = formatOrderDate(order.orderDate),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Order Status Badge
                Surface(
                    color = getOrderStatusColor(order.orderStatus).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = order.orderStatus.displayNameBn,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = getOrderStatusColor(order.orderStatus)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Customer Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = DarkTeal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.customerName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                    Text(
                        text = order.customerPhone,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Delivery Address
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = DarkTeal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.deliveryAddress,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Order Items
            if (order.orderItems.isNotEmpty()) {
                Text(
                    text = "অর্ডার আইটেম:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                order.orderItems.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.cylinderType.displayName} x ${item.quantity}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "৳${formatCurrency(item.totalPrice)}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Financial Information
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "মোট পরিমাণ: ৳${formatCurrency(order.totalAmount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = "পরিশোধিত: ৳${formatCurrency(order.paidAmount)}",
                        fontSize = 12.sp,
                        color = Color(0xFF4CAF50)
                    )
                    if (order.remainingAmount > 0) {
                        Text(
                            text = "বকেয়া: ৳${formatCurrency(order.remainingAmount)}",
                            fontSize = 12.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                }
                
                // Payment Status Badge
                Surface(
                    color = getPaymentStatusColor(order.paymentStatus).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (order.paymentStatus) {
                            PaymentStatus.PENDING -> "পেমেন্ট পেন্ডিং"
                            PaymentStatus.PARTIAL -> "আংশিক পেইড"
                            PaymentStatus.PAID -> "পেইড"
                            PaymentStatus.OVERDUE -> "বকেয়া"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = getPaymentStatusColor(order.paymentStatus)
                    )
                }
            }
            
            // Delivery Status (if available)
            relatedDelivery?.let { delivery ->
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.Gray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = DarkTeal
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ডেলিভারি স্ট্যাটাস:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkTeal
                        )
                    }
                    
                    Surface(
                        color = getDeliveryStatusColor(delivery.deliveryStatus).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = delivery.deliveryStatus.displayNameBn,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = getDeliveryStatusColor(delivery.deliveryStatus)
                        )
                    }
                }
                
                if (delivery.deliveryPerson.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ডেলিভারি পার্সন: ${delivery.deliveryPerson}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                if (delivery.scheduledDate.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "নির্ধারিত তারিখ: ${delivery.scheduledDate}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            } ?: run {
                // No delivery entry found
                if (isRunningOrder) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ডেলিভারি এখনো শুরু হয়নি",
                            fontSize = 11.sp,
                            color = Color(0xFFFF9800),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }
            
            // Priority Badge (if not normal)
            if (order.priority != OrderPriority.NORMAL) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = getPriorityColor(order.priority).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "অগ্রাধিকার: ${order.priority.displayNameBn}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = getPriorityColor(order.priority)
                    )
                }
            }
        }
    }
}

@Composable
fun SalesStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Utility functions
fun formatCurrency(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale("bn", "BD")).format(amount.toInt())
}

fun formatOrderDate(dateString: String): String {
    return try {
        val parts = dateString.split(" ")
        if (parts.size >= 2) {
            parts[0]
        } else {
            dateString
        }
    } catch (e: Exception) {
        dateString
    }
}
