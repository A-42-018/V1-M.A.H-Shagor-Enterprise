package com.firebase.loginauth

import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAwareOrderDialog(
    existingOrder: Order? = null,
    onDismiss: () -> Unit,
    onOrderCreated: () -> Unit,
    orderViewModel: OrderViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel()
) {
    val context = LocalContext.current
    val stockRepository = remember { CylinderStockRepository(context) }
    
    // State variables
    var customerName by remember { mutableStateOf(existingOrder?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(existingOrder?.customerPhone ?: "") }
    var deliveryAddress by remember { mutableStateOf(existingOrder?.deliveryAddress ?: "") }
    var notes by remember { mutableStateOf(existingOrder?.notes ?: "") }
    var selectedPriority by remember { mutableStateOf(existingOrder?.priority ?: OrderPriority.NORMAL) }
    var showCustomerSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showStockAlert by remember { mutableStateOf(false) }
    var stockAlertMessage by remember { mutableStateOf("") }
    
    // Stock data
    val stockItems by stockRepository.stockItems.collectAsState()
    val customers by customerViewModel.customers.collectAsState()
    
    // Customer suggestions
    val filteredCustomers = remember(customerName, customers) {
        if (customerName.isBlank()) emptyList()
        else customers.filter { 
            it.name.contains(customerName, ignoreCase = true) || 
            it.phone.contains(customerName, ignoreCase = true)
        }.take(5)
    }
    
    // Create stock map for quick lookup by cylinder type
    val stockMap = remember(stockItems) {
        stockItems.groupBy { "${it.cylinderType}_${it.brand}" }
            .mapValues { entry -> entry.value.sumOf { it.availableQuantity } }
    }
    
    // Initialize cylinder items with stock information
    var cylinderItems by remember {
        mutableStateOf(
            if (existingOrder != null && existingOrder.orderItems.isNotEmpty()) {
                existingOrder.orderItems.map { orderItem ->
                    CylinderOrderItem(
                        cylinderType = orderItem.cylinderType,
                        quantity = orderItem.quantity,
                        unitPrice = orderItem.unitPrice
                    )
                }
            } else {
                // Initialize with available cylinder types
                CylinderType.values().filter { !it.name.startsWith("KG_") }.map { type ->
                    CylinderOrderItem(
                        cylinderType = type,
                        quantity = 0,
                        unitPrice = getDefaultPrice(type)
                    )
                }
            }
        )
    }
    
    val totalAmount = cylinderItems.sumOf { it.totalPrice }
    val totalQuantity = cylinderItems.sumOf { it.quantity }
    
    // Function to get available stock for a cylinder type
    fun getAvailableStock(cylinderType: CylinderType): Int {
        val key = "${cylinderType.name}_${cylinderType.brand}"
        return stockMap[key] ?: 0
    }
    
    // Function to check if order exceeds available stock
    fun validateStockAvailability(): Boolean {
        val violations = mutableListOf<String>()
        
        cylinderItems.forEach { item ->
            if (item.quantity > 0) {
                val availableStock = getAvailableStock(item.cylinderType)
                if (item.quantity > availableStock) {
                    violations.add("${item.cylinderType.displayNameBn}: অর্ডার ${item.quantity}টি, স্টক ${availableStock}টি")
                }
            }
        }
        
        if (violations.isNotEmpty()) {
            stockAlertMessage = "স্টকে পর্যাপ্ত পরিমাণ নেই:\n${violations.joinToString("\n")}"
            showStockAlert = true
            return false
        }
        return true
    }
    
    // Function to create order
    fun createOrder() {
        if (!validateStockAvailability()) {
            return
        }
        
        if (customerName.isBlank() || customerPhone.isBlank() || deliveryAddress.isBlank()) {
            return
        }
        
        if (totalQuantity == 0) {
            return
        }
        
        isLoading = true
        
        try {
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                name = customerName,
                phone = customerPhone,
                address = deliveryAddress
            )
            
            val orderItems = cylinderItems.filter { it.quantity > 0 }.map { item ->
                OrderItem(
                    id = UUID.randomUUID().toString(),
                    cylinderType = item.cylinderType,
                    quantity = item.quantity,
                    unitPrice = item.unitPrice,
                    totalPrice = item.totalPrice
                )
            }
            
            if (existingOrder != null) {
                val updatedOrder = existingOrder.copy(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    orderItems = orderItems,
                    totalAmount = totalAmount,
                    remainingAmount = totalAmount - existingOrder.paidAmount,
                    notes = notes,
                    priority = selectedPriority,
                    lastUpdated = getCurrentDateTime()
                )
                orderViewModel.updateOrder(updatedOrder)
            } else {
                orderViewModel.createOrderWithCustomerDetails(
                    customerName = customerName,
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    orderItems = orderItems,
                    notes = notes,
                    priority = selectedPriority
                )
            }
            
            onOrderCreated()
        } catch (e: Exception) {
            Log.e("StockAwareOrderDialog", "Error creating order", e)
        } finally {
            isLoading = false
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingOrder != null) "অর্ডার আপডেট" else "নতুন অর্ডার",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Information Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "গ্রাহকের তথ্য",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTeal
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Customer Name
                                OutlinedTextField(
                                    value = customerName,
                                    onValueChange = { 
                                        customerName = it
                                        showCustomerSuggestions = it.isNotBlank()
                                    },
                                    label = { Text("গ্রাহকের নাম") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                // Customer suggestions
                                if (showCustomerSuggestions && filteredCustomers.isNotEmpty()) {
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color.White),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                                    ) {
                                        Column {
                                            filteredCustomers.forEach { customer ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            customerName = customer.name
                                                            customerPhone = customer.phone
                                                            deliveryAddress = customer.address
                                                            showCustomerSuggestions = false
                                                        }
                                                        .padding(12.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Person,
                                                        contentDescription = null,
                                                        tint = DarkTeal,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = customer.name,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = customer.phone,
                                                            fontSize = 12.sp,
                                                            color = Color.Gray
                                                        )
                                                    }
                                                }
                                                if (customer != filteredCustomers.last()) {
                                                    Divider(color = Color.Gray.copy(alpha = 0.3f))
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Customer Phone
                                OutlinedTextField(
                                    value = customerPhone,
                                    onValueChange = { customerPhone = it },
                                    label = { Text("ফোন নম্বর") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Delivery Address
                                OutlinedTextField(
                                    value = deliveryAddress,
                                    onValueChange = { deliveryAddress = it },
                                    label = { Text("ডেলিভারি ঠিকানা") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                    
                    // Cylinder Selection Section
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "সিলিন্ডার নির্বাচন",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal
                                    )
                                    
                                    // Stock refresh button
                                    IconButton(
                                        onClick = { stockRepository.syncWithCloud() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Refresh,
                                            contentDescription = "Refresh Stock",
                                            tint = DarkTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Stock summary
                                val lowStockCount = stockItems.count { it.getStockLevel() == StockLevel.LOW || it.getStockLevel() == StockLevel.CRITICAL }
                                if (lowStockCount > 0) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = Color(0xFFFF9800),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "$lowStockCount টি আইটেমের স্টক কম",
                                                fontSize = 12.sp,
                                                color = Color(0xFFE65100),
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                    
                    // Cylinder Items
                    items(cylinderItems) { item ->
                        StockAwareCylinderCard(
                            item = item,
                            availableStock = getAvailableStock(item.cylinderType),
                            onQuantityChange = { newQuantity ->
                                cylinderItems = cylinderItems.map { 
                                    if (it.cylinderType == item.cylinderType) {
                                        it.copy(quantity = newQuantity)
                                    } else it
                                }
                            }
                        )
                    }
                    
                    // Order Summary
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = DarkTeal.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "অর্ডার সারসংক্ষেপ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkTeal
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট পরিমাণ:", fontSize = 14.sp)
                                    Text("$totalQuantity টি", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট দাম:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "৳${String.format("%.0f", totalAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal
                                    )
                                }
                            }
                        }
                    }
                    
                    // Additional Options
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                // Priority Selection
                                Text(
                                    text = "অগ্রাধিকার",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkTeal
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OrderPriority.values().forEach { priority ->
                                        FilterChip(
                                            onClick = { selectedPriority = priority },
                                            label = { Text(priority.displayNameBn, fontSize = 12.sp) },
                                            selected = selectedPriority == priority,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Notes
                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { notes = it },
                                    label = { Text("নোট (ঐচ্ছিক)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2,
                                    maxLines = 3
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = { createOrder() },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && customerName.isNotBlank() && customerPhone.isNotBlank() && 
                                deliveryAddress.isNotBlank() && totalQuantity > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(if (existingOrder != null) "আপডেট করুন" else "অর্ডার করুন")
                        }
                    }
                }
            }
        }
    }
    
    // Stock Alert Dialog
    if (showStockAlert) {
        AlertDialog(
            onDismissRequest = { showStockAlert = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("স্টক সতর্কতা")
                }
            },
            text = {
                Text(stockAlertMessage)
            },
            confirmButton = {
                TextButton(onClick = { showStockAlert = false }) {
                    Text("বুঝেছি")
                }
            }
        )
    }
}

@Composable
private fun StockAwareCylinderCard(
    item: CylinderOrderItem,
    availableStock: Int,
    onQuantityChange: (Int) -> Unit
) {
    val stockLevel = when {
        availableStock <= 0 -> StockLevel.CRITICAL
        availableStock <= 5 -> StockLevel.CRITICAL
        availableStock <= 10 -> StockLevel.LOW
        else -> StockLevel.NORMAL
    }
    
    val stockColor = when (stockLevel) {
        StockLevel.CRITICAL -> Color(0xFFF44336)
        StockLevel.LOW -> Color(0xFFFF9800)
        StockLevel.NORMAL -> Color(0xFF4CAF50)
        StockLevel.OVERSTOCKED -> Color(0xFF2196F3)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with cylinder info and stock status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.cylinderType.displayNameBn,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "৳${String.format("%.0f", item.unitPrice)} প্রতি পিস",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                // Stock indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stockColor)
                    )
                    Text(
                        text = "স্টক: $availableStock",
                        fontSize = 12.sp,
                        color = stockColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Quantity controls and total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Quantity controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { if (item.quantity > 0) onQuantityChange(item.quantity - 1) },
                        enabled = item.quantity > 0
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Decrease",
                            tint = if (item.quantity > 0) PrimaryBlue else Color.Gray
                        )
                    }
                    
                    Text(
                        text = "${item.quantity}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(30.dp),
                        textAlign = TextAlign.Center
                    )
                    
                    IconButton(
                        onClick = { 
                            if (item.quantity < availableStock) {
                                onQuantityChange(item.quantity + 1)
                            }
                        },
                        enabled = item.quantity < availableStock && availableStock > 0
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase",
                            tint = if (item.quantity < availableStock && availableStock > 0) PrimaryBlue else Color.Gray
                        )
                    }
                }
                
                // Total price
                if (item.quantity > 0) {
                    Text(
                        text = "৳${String.format("%.0f", item.totalPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                }
            }
            
            // Stock warning
            if (item.quantity > availableStock && availableStock >= 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "স্টকে পর্যাপ্ত পরিমাণ নেই!",
                        fontSize = 11.sp,
                        color = Color(0xFFF44336),
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (stockLevel == StockLevel.CRITICAL && availableStock > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "স্টক কম! তাড়াতাড়ি অর্ডার করুন",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Helper function to get default price for cylinder types
private fun getDefaultPrice(cylinderType: CylinderType): Double {
    return when (cylinderType) {
        CylinderType.OMERA_12KG -> 1200.0
        CylinderType.OMERA_25KG -> 2200.0
        CylinderType.OMERA_35KG -> 3000.0
        CylinderType.LAUGFS_12KG -> 1250.0
        CylinderType.TOTAL_12KG -> 1180.0
        CylinderType.TOTAL_15KG -> 1450.0
        else -> 1200.0 // Default price for legacy types
    }
}

// Utility function
private fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD"))
    return sdf.format(Date())
}
