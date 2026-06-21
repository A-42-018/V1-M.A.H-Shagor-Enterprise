package com.firebase.loginauth

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedMultiCylinderOrderDialog(
    existingOrder: Order? = null,
    onDismiss: () -> Unit,
    onOrderCreated: () -> Unit,
    orderViewModel: OrderViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel()
) {
    // Stock repository for displaying current stock levels
    val context = LocalContext.current
    val stockRepository = remember { CylinderStockRepository(context) }
    val stockItems by stockRepository.stockItems.collectAsState()
    var customerName by remember { mutableStateOf(existingOrder?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(existingOrder?.customerPhone ?: "") }
    var deliveryAddress by remember { mutableStateOf(existingOrder?.deliveryAddress ?: "") }
    var notes by remember { mutableStateOf(existingOrder?.notes ?: "") }
    var selectedPriority by remember { mutableStateOf(existingOrder?.priority ?: OrderPriority.NORMAL) }
    var showCustomerSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Customer suggestions
    val customers by customerViewModel.customers.collectAsState()
    val filteredCustomers = remember(customerName, customers) {
        if (customerName.isBlank()) emptyList()
        else customers.filter { 
            it.name.contains(customerName, ignoreCase = true) || 
            it.phone.contains(customerName, ignoreCase = true)
        }.take(5)
    }
    
    // Helper function to get price from stock data
    fun getPriceFromStock(cylinderType: CylinderType): Double {
        val matchingStock = stockItems.find { stock ->
            stock.cylinderType.equals(cylinderType.displayName, ignoreCase = true) ||
            stock.cylinderType.equals(cylinderType.displayNameBn, ignoreCase = true) ||
            stock.brand.equals(cylinderType.brand, ignoreCase = true)
        }
        return matchingStock?.pricePerUnit ?: 0.0
    }
    
    // Cylinder order items - initialize from existing order or default
    var cylinderItems by remember {
        mutableStateOf(
            if (existingOrder != null && existingOrder.orderItems.isNotEmpty()) {
                // Parse existing order items
                existingOrder.orderItems.map { orderItem ->
                    CylinderOrderItem(
                        cylinderType = orderItem.cylinderType,
                        quantity = orderItem.quantity,
                        unitPrice = orderItem.unitPrice
                    )
                }
            } else {
                // Default cylinder types with 0 quantity and stock-based pricing
                CylinderType.values().filter { !it.name.startsWith("KG_") }.map { type ->
                    CylinderOrderItem(
                        cylinderType = type,
                        quantity = 0,
                        unitPrice = getPriceFromStock(type)
                    )
                }
            }
        )
    }
    
    val totalAmount = cylinderItems.sumOf { it.totalPrice }
    val totalQuantity = cylinderItems.sumOf { it.quantity }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Enhanced Header with pricing info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (existingOrder != null) "অর্ডার আপডেট করুন" else "নতুন অর্ডার",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    
                    // Pricing mode indicator
                    Card(
                        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "কাস্টমার রেট সিস্টেম",
                            fontSize = 10.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                        Text(
                            text = "গ্রাহকের তথ্য",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Box {
                            OutlinedTextField(
                                value = customerName,
                                onValueChange = { 
                                    customerName = it
                                    showCustomerSuggestions = it.isNotBlank() && filteredCustomers.isNotEmpty()
                                },
                                label = { Text("গ্রাহকের নাম") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    focusedLabelColor = PrimaryBlue
                                )
                            )
                            
                            if (showCustomerSuggestions && filteredCustomers.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 56.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = customer.name,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    Text(
                                                        text = customer.phone,
                                                        fontSize = 12.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { customerPhone = it },
                            label = { Text("ফোন নম্বর") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue
                            )
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text("ডেলিভারি ঠিকানা") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue
                            )
                        )
                    }
                    
                    // Priority Selection
                    item {
                        Text(
                            text = "অগ্রাধিকার",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderPriority.values().forEach { priority ->
                                FilterChip(
                                    onClick = { selectedPriority = priority },
                                    label = { 
                                        Text(
                                            text = when (priority) {
                                                OrderPriority.LOW -> "কম"
                                                OrderPriority.NORMAL -> "সাধারণ"
                                                OrderPriority.HIGH -> "উচ্চ"
                                                OrderPriority.URGENT -> "জরুরি"
                                                OrderPriority.IRREGULAR -> "অনিয়মিত"
                                            }
                                        )
                                    },
                                    selected = selectedPriority == priority,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                    
                    // Cylinder Selection Section with pricing instructions
                    item {
                        Column {
                            Text(
                                text = "সিলিন্ডার নির্বাচন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal
                            )
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            // Pricing instructions
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "নির্দেশনা: পরিমাণ নির্বাচনের পর কাস্টমার রেট বক্স দেখা যাবে। ডিসকাউন্টের জন্য কাস্টম রেট দিন অথবা ডিফল্ট রেটের জন্য খালি রাখুন।",
                                    fontSize = 11.sp,
                                    color = Color(0xFF666666),
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                    
                    items(cylinderItems) { item ->
                        val matchingStock = stockItems.find { stock ->
                            // Convert CylinderType enum to string for comparison
                            stock.cylinderType == item.cylinderType.name || 
                            stock.cylinderType.contains(item.cylinderType.displayNameBn, ignoreCase = true) ||
                            stock.cylinderType.contains(item.cylinderType.displayName, ignoreCase = true)
                        }
                        
                        CylinderOrderItemCard(
                            item = item,
                            currentStock = matchingStock?.availableQuantity ?: 0,
                            isLowStock = (matchingStock?.availableQuantity ?: 0) <= 5, // Default low stock threshold
                            onQuantityChange = { newQuantity ->
                                cylinderItems = cylinderItems.map { 
                                    if (it.cylinderType == item.cylinderType) {
                                        it.copy(quantity = newQuantity)
                                    } else it
                                }
                            },
                            onPriceChange = { newPrice ->
                                cylinderItems = cylinderItems.map { 
                                    if (it.cylinderType == item.cylinderType) {
                                        it.copy(unitPrice = newPrice)
                                    } else it
                                }
                            }
                        )
                    }
                    
                    // Notes Section
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("বিশেষ নোট (ঐচ্ছিক)") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                focusedLabelColor = PrimaryBlue
                            )
                        )
                    }
                    
                    // Enhanced Order Summary with pricing breakdown
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = LightTeal.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "অর্ডার সারাংশ",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal
                                    )
                                    
                                    // Show if custom pricing is being used
                                    val hasCustomPricing = cylinderItems.any { it.quantity > 0 }
                                    if (hasCustomPricing) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8)),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "কাস্টম রেট সক্রিয়",
                                                fontSize = 10.sp,
                                                color = Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // Item breakdown
                                cylinderItems.filter { it.quantity > 0 }.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${item.cylinderType.displayNameBn} x ${item.quantity}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "৳${String.format("%.0f", item.totalPrice)}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                                
                                if (cylinderItems.any { it.quantity > 0 }) {
                                    Divider(
                                        modifier = Modifier.padding(vertical = 8.dp),
                                        color = Color.Gray.copy(alpha = 0.3f)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট পরিমাণ:", fontWeight = FontWeight.Medium)
                                    Text("$totalQuantity পিস", fontWeight = FontWeight.Bold)
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("মোট দাম:", fontWeight = FontWeight.Medium, fontSize = 16.sp)
                                    Text(
                                        "৳${String.format("%.0f", totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        fontSize = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
                
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
                        onClick = {
                            isLoading = true
                            
                            val orderItems = cylinderItems.filter { it.quantity > 0 }.map { item ->
                                OrderItem(
                                    cylinderType = item.cylinderType,
                                    quantity = item.quantity,
                                    unitPrice = item.unitPrice
                                )
                            }
                            
                            if (existingOrder != null) {
                                // Update existing order
                                val updatedOrder = existingOrder.copy(
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    deliveryAddress = deliveryAddress,
                                    orderItems = orderItems,
                                    totalAmount = totalAmount,
                                    notes = notes,
                                    priority = selectedPriority
                                )
                                orderViewModel.updateOrder(updatedOrder)
                            } else {
                                // Create new order
                                orderViewModel.createOrderWithCustomerDetails(
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    deliveryAddress = deliveryAddress,
                                    orderItems = orderItems,
                                    notes = notes,
                                    priority = selectedPriority
                                )
                            }
                            
                            isLoading = false
                            onOrderCreated()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = customerName.isNotBlank() && customerPhone.isNotBlank() && totalQuantity > 0 && !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
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
}

@Composable
private fun CylinderOrderItemCard(
    item: CylinderOrderItem,
    currentStock: Int = 0,
    isLowStock: Boolean = false,
    onQuantityChange: (Int) -> Unit,
    onPriceChange: (Double) -> Unit = {}
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = when (item.cylinderType) {
                            CylinderType.OMERA_12KG -> "ওমেরা ১২ কেজি"
                            CylinderType.OMERA_25KG -> "ওমেরা ২৫ কেজি"
                            CylinderType.OMERA_35KG -> "ওমেরা ৩৫ কেজি"
                            CylinderType.LAUGFS_12KG -> "লাফ্স ১২ কেজি"
                            CylinderType.TOTAL_12KG -> "টোটাল ১২ কেজি"
                            CylinderType.TOTAL_15KG -> "টোটাল ১৫ কেজি"
                            else -> item.cylinderType.name
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "৳${String.format("%.0f", item.unitPrice)} প্রতি পিস",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    
                    // Current stock display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "স্টক: $currentStock পিস",
                            fontSize = 11.sp,
                            color = if (currentStock > 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Low stock alert
                        if (isLowStock && currentStock > 0) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Low Stock",
                                tint = Color(0xFFFF9800),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        
                        // Out of stock alert
                        if (currentStock == 0) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Out of Stock",
                                tint = Color(0xFFF44336),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
                
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
                            if (item.quantity < currentStock) {
                                onQuantityChange(item.quantity + 1)
                            }
                        },
                        enabled = item.quantity < currentStock
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Increase",
                            tint = if (item.quantity < currentStock) PrimaryBlue else Color.Gray
                        )
                    }
                }
                
                if (item.quantity > 0) {
                    Text(
                        text = "৳${String.format("%.0f", item.totalPrice)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            
            // Customer-specific price input (optional) - only show when quantity > 0
            if (item.quantity > 0) {
                var customPrice by remember(item.cylinderType) { mutableStateOf("") }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = customPrice,
                    onValueChange = { newPrice ->
                        customPrice = newPrice
                        if (newPrice.isBlank()) {
                            // Use default price when field is empty - don't call onPriceChange
                            // The original unitPrice will be used
                        } else {
                            val price = newPrice.toDoubleOrNull()
                            if (price != null && price >= 0) {
                                onPriceChange(price)
                            }
                        }
                    },
                    label = { Text("কাস্টমার রেট (ঐচ্ছিক)", fontSize = 12.sp) },
                    placeholder = { Text("ডিফল্ট: ৳${String.format("%.0f", item.unitPrice)}", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Gray
                    ),
                    singleLine = true
                )
            }
        }
    }
}
