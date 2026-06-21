package com.firebase.loginauth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryStatusScreen(
    onBackClick: () -> Unit = {},
    deliveryViewModel: DeliveryViewModel = viewModel(),
    orderViewModel: OrderViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var showAddDeliveryDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showUpdateStatusDialog by remember { mutableStateOf(false) }
    var showAssignPersonDialog by remember { mutableStateOf(false) }
    var selectedDelivery by remember { mutableStateOf<DeliveryEntry?>(null) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var showOrderActionDialog by remember { mutableStateOf(false) }
    
    val deliveries by deliveryViewModel.filteredDeliveries.collectAsState()
    val deliveryStats by deliveryViewModel.deliveryStats.collectAsState()
    val saveStatus by deliveryViewModel.saveStatus.collectAsState()
    val runningOrders by deliveryViewModel.runningOrders.collectAsState()
    val allOrders by deliveryViewModel.allOrders.collectAsState()
    
    // Filter deliveries based on selected tab
    val filteredDeliveries = when (selectedTab) {
        0 -> deliveries // All Deliveries
        1 -> deliveries.filter { it.deliveryStatus == DeliveryStatusType.PENDING }
        2 -> deliveries.filter { 
            it.deliveryStatus == DeliveryStatusType.IN_TRANSIT || 
            it.deliveryStatus == DeliveryStatusType.OUT_FOR_DELIVERY 
        }
        3 -> deliveries.filter { it.deliveryStatus == DeliveryStatusType.DELIVERED }
        else -> deliveries
    }
    
    // Load orders when component initializes
    LaunchedEffect(Unit) {
        deliveryViewModel.loadRunningOrders()
        deliveryViewModel.loadAllOrders()
    }
    
    // Show save status messages
    LaunchedEffect(saveStatus) {
        if (saveStatus is DeliverySaveStatus.Success) {
            kotlinx.coroutines.delay(2000)
            deliveryViewModel.resetSaveStatus()
        } else if (saveStatus is DeliverySaveStatus.Error) {
            kotlinx.coroutines.delay(3000)
            deliveryViewModel.resetSaveStatus()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal, LightTeal)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Text(
                    text = "ডেলিভারি স্ট্যাটাস",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                // Filter button
                IconButton(
                    onClick = { showFilterDialog = true },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Filter",
                        tint = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Add delivery button
                IconButton(
                    onClick = { showAddDeliveryDialog = true },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Add Delivery",
                        tint = Color.White
                    )
                }
            }
            
            // Stats Cards
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White,
                        RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Stats Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeliveryStatCard(
                            title = "মোট",
                            value = deliveryStats.totalDeliveries.toString(),
                            color = DarkTeal,
                            modifier = Modifier.weight(1f)
                        )
                        DeliveryStatCard(
                            title = "অপেক্ষমান",
                            value = deliveryStats.pendingDeliveries.toString(),
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                        DeliveryStatCard(
                            title = "পথে",
                            value = deliveryStats.inTransitDeliveries.toString(),
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )
                        DeliveryStatCard(
                            title = "সম্পন্ন",
                            value = deliveryStats.completedDeliveries.toString(),
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                // Tab Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val tabTitles = listOf("সব ডেলিভারি", "পেন্ডিং", "চলমান", "সম্পন্ন", "রানিং অর্ডার", "সব অর্ডার")
                        
                        tabTitles.forEachIndexed { index, title ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedTab = index },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedTab == index) DarkTeal else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = title,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    color = if (selectedTab == index) Color.White else DarkTeal,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
                
                // Content based on selected tab
                if (selectedTab == 4) {
                    // Running Orders Tab
                    items(runningOrders) { order ->
                        RunningOrderCard(
                            order = order,
                            onConvertToDelivery = {
                                deliveryViewModel.convertOrderToDelivery(order)
                            },
                            onEdit = {
                                selectedOrder = order
                                showOrderActionDialog = true
                            },
                            onDelete = {
                                deliveryViewModel.deleteOrder(order.id)
                            }
                        )
                    }
                } else if (selectedTab == 5) {
                    // All Orders Tab
                    if (allOrders.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = LightGray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "কোন অর্ডার পাওয়া যায়নি",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "অর্ডার ম্যানেজমেন্ট থেকে নতুন অর্ডার তৈরি করুন",
                                        fontSize = 14.sp,
                                        color = LightGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(allOrders) { order ->
                            AllOrderCard(
                                order = order,
                                onConvertToDelivery = {
                                    deliveryViewModel.convertOrderToDelivery(order)
                                },
                                onUpdateStatus = { newStatus ->
                                    deliveryViewModel.updateOrderStatus(order.id, newStatus)
                                },
                                onUpdatePaymentStatus = { newPaymentStatus ->
                                    deliveryViewModel.updateOrderPaymentStatus(order.id, newPaymentStatus)
                                },
                                onEdit = {
                                    selectedOrder = order
                                    showOrderActionDialog = true
                                },
                                onDelete = {
                                    deliveryViewModel.deleteOrder(order.id)
                                }
                            )
                        }
                    }
                } else {
                    // Delivery Tabs
                    if (filteredDeliveries.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.ShoppingCart,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = LightGray
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "কোন ডেলিভারি পাওয়া যায়নি",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkGray,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = "নতুন ডেলিভারি যোগ করতে + বাটনে ক্লিক করুন",
                                        fontSize = 14.sp,
                                        color = LightGray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredDeliveries) { delivery ->
                            DeliveryCard(
                                delivery = delivery,
                                onUpdateStatus = {
                                    selectedDelivery = delivery
                                    showUpdateStatusDialog = true
                                },
                                onAssignPerson = {
                                    selectedDelivery = delivery
                                    showAssignPersonDialog = true
                                },
                                onEdit = {
                                    selectedDelivery = delivery
                                    showAddDeliveryDialog = true
                                },
                                onDelete = {
                                    deliveryViewModel.deleteDelivery(delivery.id)
                                }
                            )
                        }
                    }
                }
                
                // Add some bottom padding
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
        
        // Save Status Message
        when (saveStatus) {
            is DeliverySaveStatus.Saving -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkTeal),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "সংরক্ষণ হচ্ছে...",
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            is DeliverySaveStatus.Success -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = (saveStatus as DeliverySaveStatus.Success).message,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            is DeliverySaveStatus.Error -> {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = (saveStatus as DeliverySaveStatus.Error).message,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            else -> {}
        }
    }
    
    // Dialogs
    if (showAddDeliveryDialog) {
        AddDeliveryDialog(
            delivery = selectedDelivery,
            deliveryViewModel = deliveryViewModel,
            onDismiss = { 
                showAddDeliveryDialog = false
                selectedDelivery = null
            },
            onSave = { delivery ->
                if (selectedDelivery != null) {
                    deliveryViewModel.editDelivery(delivery)
                } else {
                    deliveryViewModel.addDelivery(
                        orderId = delivery.orderId,
                        orderNumber = delivery.orderNumber,
                        customerId = delivery.customerId,
                        customerName = delivery.customerName,
                        customerPhone = delivery.customerPhone,
                        deliveryAddress = delivery.deliveryAddress,
                        cylinderDetails = delivery.cylinderDetails,
                        totalAmount = delivery.totalAmount,
                        scheduledDate = delivery.scheduledDate,
                        scheduledTime = delivery.scheduledTime,
                        deliveryArea = delivery.deliveryArea,
                        priority = delivery.priority,
                        notes = delivery.notes
                    )
                }
                showAddDeliveryDialog = false
                selectedDelivery = null
            }
        )
    }
    
    if (showUpdateStatusDialog && selectedDelivery != null) {
        // Get the related order for payment status
        val relatedOrder = allOrders.find { it.id == selectedDelivery!!.orderId }
        
        // Debug logging
        Log.d("DeliveryStatusScreen", "Looking for order with ID: ${selectedDelivery!!.orderId}")
        Log.d("DeliveryStatusScreen", "Available orders: ${allOrders.map { "${it.id} - ${it.orderNumber}" }}")
        Log.d("DeliveryStatusScreen", "Related order found: ${relatedOrder?.orderNumber ?: "NOT FOUND"}")
        
        // Use the EXACT working dialog from order management that we know works!
        relatedOrder?.let { order ->
            OrderStatusUpdateDialog(
                order = order,
                onDismiss = { 
                    showUpdateStatusDialog = false
                    selectedDelivery = null
                },
                onStatusUpdate = { orderId, orderStatus, paymentStatus, deliveryStatus, paidAmount ->
                    Log.d("DeliveryStatusScreen", "[WORKING_DIALOG] Using proven order dialog - OrderID: $orderId, PaymentStatus: $paymentStatus, Amount: $paidAmount")
                    
                    // Use the EXACT same payment update logic as order management
                    if (paymentStatus != null) {
                        Log.d("DeliveryStatusScreen", "[WORKING_DIALOG] Calling orderViewModel.updatePaymentStatus")
                        orderViewModel.updatePaymentStatus(
                            orderId = orderId,
                            paymentStatus = paymentStatus,
                            paidAmount = paidAmount
                        )
                        Log.d("DeliveryStatusScreen", "[WORKING_DIALOG] Payment update called successfully")
                    }
                    
                    // Update delivery status if needed
                    if (deliveryStatus != null) {
                        Log.d("DeliveryStatusScreen", "[WORKING_DIALOG] Updating delivery status to: $deliveryStatus")
                        // Convert DeliveryStatus to DeliveryStatusType
                        val deliveryStatusType = when (deliveryStatus) {
                            DeliveryStatus.PENDING -> DeliveryStatusType.PENDING
                            DeliveryStatus.IN_TRANSIT -> DeliveryStatusType.IN_TRANSIT
                            DeliveryStatus.DELIVERED -> DeliveryStatusType.DELIVERED
                            DeliveryStatus.CANCELLED -> DeliveryStatusType.CANCELLED
                        }
                        deliveryViewModel.updateDeliveryStatus(selectedDelivery!!.id, deliveryStatusType)
                    }
                    
                    showUpdateStatusDialog = false
                    selectedDelivery = null
                }
            )
        } ?: run {
            Log.e("DeliveryStatusScreen", "[ERROR] No related order found for delivery ${selectedDelivery!!.id}")
            showUpdateStatusDialog = false
            selectedDelivery = null
        }
    }
    
    if (showAssignPersonDialog && selectedDelivery != null) {
        AssignDeliveryPersonDialog(
            delivery = selectedDelivery!!,
            availablePersons = deliveryViewModel.getAvailableDeliveryPersons(),
            onDismiss = { 
                showAssignPersonDialog = false
                selectedDelivery = null
            },
            onAssign = { personName, personPhone ->
                deliveryViewModel.assignDeliveryPerson(
                    deliveryId = selectedDelivery!!.id,
                    personName = personName,
                    personPhone = personPhone
                )
                showAssignPersonDialog = false
                selectedDelivery = null
            }
        )
    }
    
    if (showFilterDialog) {
        DeliveryFilterDialog(
            onDismiss = { showFilterDialog = false },
            onApplyFilter = { filter ->
                deliveryViewModel.filterDeliveries(filter)
                showFilterDialog = false
            },
            onClearFilter = {
                deliveryViewModel.clearFilters()
                showFilterDialog = false
            }
        )
    }
    
    // Order Action Dialog (for editing orders from Running Orders tab)
    if (showOrderActionDialog && selectedOrder != null) {
        AlertDialog(
            onDismissRequest = { 
                showOrderActionDialog = false
                selectedOrder = null
            },
            title = { Text("অর্ডার একশন") },
            text = { 
                Column {
                    Text("অর্ডার নম্বর: ${selectedOrder!!.orderNumber}")
                    Text("কাস্টমার: ${selectedOrder!!.customerName}")
                    Text("মোট টাকা: ৳${selectedOrder!!.totalAmount.toInt()}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("আপনি কি করতে চান?", fontWeight = FontWeight.Medium)
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            selectedOrder?.let { deliveryViewModel.convertOrderToDelivery(it) }
                            showOrderActionDialog = false
                            selectedOrder = null
                        }
                    ) {
                        Text("ডেলিভারিতে পাঠান")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            selectedOrder?.let { deliveryViewModel.deleteOrder(it.id) }
                            showOrderActionDialog = false
                            selectedOrder = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                    ) {
                        Text("মুছে ফেলুন")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showOrderActionDialog = false
                        selectedOrder = null
                    }
                ) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
fun DeliveryStatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = color
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
fun AllOrderCard(
    order: Order,
    onConvertToDelivery: () -> Unit,
    onUpdateStatus: (OrderStatus) -> Unit,
    onUpdatePaymentStatus: (PaymentStatus) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showStatusMenu by remember { mutableStateOf(false) }
    var showPaymentMenu by remember { mutableStateOf(false) }
    var showActionMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
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
                        text = "অর্ডার #${order.orderNumber}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = order.customerName,
                        fontSize = 14.sp,
                        color = DarkGray
                    )
                }
                
                // Priority Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getPriorityColor(order.priority).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = when (order.priority) {
                            OrderPriority.HIGH -> "উচ্চ"
                            OrderPriority.URGENT -> "জরুরি"
                            else -> "সাধারণ"
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        color = getPriorityColor(order.priority),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Order Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "মোট টাকা: ৳${order.totalAmount.toInt()}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                    Text(
                        text = "পেইড: ৳${order.paidAmount.toInt()}",
                        fontSize = 12.sp,
                        color = if (order.paidAmount >= order.totalAmount) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                    if (order.remainingAmount > 0) {
                        Text(
                            text = "বাকি: ৳${order.remainingAmount.toInt()}",
                            fontSize = 12.sp,
                            color = Color(0xFFF44336)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "তারিখ: ${order.orderDate}",
                        fontSize = 12.sp,
                        color = DarkGray
                    )
                    if (order.deliveryArea.isNotEmpty()) {
                        Text(
                            text = "এলাকা: ${order.deliveryArea}",
                            fontSize = 12.sp,
                            color = DarkGray
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Order Items
            if (order.orderItems.isNotEmpty()) {
                Text(
                    text = "পণ্য:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGray
                )
                order.orderItems.forEach { item ->
                    Text(
                        text = "• ${item.cylinderType} (${item.quantity}টি) - ৳${item.totalPrice.toInt()}",
                        fontSize = 11.sp,
                        color = DarkGray,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Status Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Order Status
                Box {
                    Card(
                        modifier = Modifier.clickable { showStatusMenu = true },
                        colors = CardDefaults.cardColors(
                            containerColor = getOrderStatusColor(order.orderStatus).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (order.orderStatus) {
                                    OrderStatus.PENDING -> "পেন্ডিং"
                                    OrderStatus.CONFIRMED -> "নিশ্চিত"
                                    OrderStatus.PROCESSING -> "প্রক্রিয়াধীন"
                                    OrderStatus.READY_FOR_DELIVERY -> "প্রস্তুত"
                                    OrderStatus.OUT_FOR_DELIVERY -> "ডেলিভারিতে"
                                    OrderStatus.DELIVERED -> "সম্পন্ন"
                                    OrderStatus.CANCELLED -> "বাতিল"
                                    OrderStatus.RETURNED -> "ফেরত"
                                },
                                fontSize = 10.sp,
                                color = getOrderStatusColor(order.orderStatus),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = getOrderStatusColor(order.orderStatus)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showStatusMenu,
                        onDismissRequest = { showStatusMenu = false }
                    ) {
                        OrderStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (status) {
                                            OrderStatus.PENDING -> "পেন্ডিং"
                                            OrderStatus.CONFIRMED -> "নিশ্চিত"
                                            OrderStatus.PROCESSING -> "প্রক্রিয়াধীন"
                                            OrderStatus.READY_FOR_DELIVERY -> "প্রস্তুত"
                                            OrderStatus.OUT_FOR_DELIVERY -> "ডেলিভারিতে"
                                            OrderStatus.DELIVERED -> "সম্পন্ন"
                                            OrderStatus.CANCELLED -> "বাতিল"
                                            OrderStatus.RETURNED -> "ফেরত"
                                        }
                                    )
                                },
                                onClick = {
                                    onUpdateStatus(status)
                                    showStatusMenu = false
                                }
                            )
                        }
                    }
                }
                
                // Payment Status
                Box {
                    Card(
                        modifier = Modifier.clickable { showPaymentMenu = true },
                        colors = CardDefaults.cardColors(
                            containerColor = getPaymentStatusColor(order.paymentStatus).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (order.paymentStatus) {
                                    PaymentStatus.PENDING -> "পেমেন্ট পেন্ডিং"
                                    PaymentStatus.PARTIAL -> "আংশিক পেইড"
                                    PaymentStatus.PAID -> "পেইড"
                                    PaymentStatus.OVERDUE -> "বকেয়া"
                                },
                                fontSize = 10.sp,
                                color = getPaymentStatusColor(order.paymentStatus),
                                fontWeight = FontWeight.Medium
                            )
                            Icon(
                                Icons.Filled.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = getPaymentStatusColor(order.paymentStatus)
                            )
                        }
                    }
                    
                    DropdownMenu(
                        expanded = showPaymentMenu,
                        onDismissRequest = { showPaymentMenu = false }
                    ) {
                        PaymentStatus.values().forEach { status ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (status) {
                                            PaymentStatus.PENDING -> "পেমেন্ট পেন্ডিং"
                                            PaymentStatus.PARTIAL -> "আংশিক পেইড"
                                            PaymentStatus.PAID -> "পেইড"
                                            PaymentStatus.OVERDUE -> "বকেয়া"
                                        }
                                    )
                                },
                                onClick = {
                                    onUpdatePaymentStatus(status)
                                    showPaymentMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Convert to Delivery Button
                if (order.orderStatus == OrderStatus.PENDING || order.orderStatus == OrderStatus.CONFIRMED) {
                    Button(
                        onClick = onConvertToDelivery,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ডেলিভারিতে পাঠান",
                            fontSize = 12.sp
                        )
                    }
                }
                
                // More Actions Button
                Box {
                    OutlinedButton(
                        onClick = { showActionMenu = true },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Filled.MoreVert,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "আরো",
                            fontSize = 12.sp
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showActionMenu,
                        onDismissRequest = { showActionMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("এডিট করুন") },
                            onClick = {
                                onEdit()
                                showActionMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Edit, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("মুছে ফেলুন", color = Color.Red) },
                            onClick = {
                                onDelete()
                                showActionMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = Color.Red)
                            }
                        )
                    }
                }
            }
        }
    }
}
