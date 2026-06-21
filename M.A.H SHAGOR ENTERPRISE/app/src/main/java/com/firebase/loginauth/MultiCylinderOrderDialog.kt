package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.firebase.loginauth.ui.theme.*

data class CylinderOrderItem(
    val cylinderType: CylinderType,
    var quantity: Int = 0,
    val unitPrice: Double = getDefaultPrice(cylinderType)
) {
    val totalPrice: Double
        get() = quantity * unitPrice
}

// Default prices for different cylinder types
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiCylinderOrderDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onOrderCreated: (Order) -> Unit
) {
    // Initialize cylinder order items with all available types
    var cylinderItems by remember {
        mutableStateOf(
            listOf(
                CylinderOrderItem(CylinderType.OMERA_12KG),
                CylinderOrderItem(CylinderType.OMERA_25KG),
                CylinderOrderItem(CylinderType.OMERA_35KG),
                CylinderOrderItem(CylinderType.LAUGFS_12KG),
                CylinderOrderItem(CylinderType.TOTAL_12KG),
                CylinderOrderItem(CylinderType.TOTAL_15KG)
            )
        )
    }
    
    var deliveryAddress by remember { mutableStateOf(customer.address) }
    var notes by remember { mutableStateOf("") }
    var orderPriority by remember { mutableStateOf(OrderPriority.NORMAL) }
    var deliveryDate by remember { mutableStateOf("") }
    
    // Calculate totals
    val totalQuantity = cylinderItems.sumOf { it.quantity }
    val totalAmount = cylinderItems.sumOf { it.totalPrice }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = "নতুন অর্ডার তৈরি করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = "গ্রাহক: ${customer.name} (${customer.phone})",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cylinder Selection Section
                    item {
                        Text(
                            text = "সিলিন্ডার নির্বাচন করুন",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = DarkTeal,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(cylinderItems) { item ->
                        CylinderOrderItemCard(
                            item = item,
                            onQuantityChange = { newQuantity ->
                                cylinderItems = cylinderItems.map { 
                                    if (it.cylinderType == item.cylinderType) {
                                        it.copy(quantity = newQuantity.coerceAtLeast(0))
                                    } else it
                                }
                            }
                        )
                    }
                    
                    // Order Summary
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = LightTeal.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "অর্ডার সারসংক্ষেপ",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DarkTeal,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "মোট সিলিন্ডার:",
                                        fontSize = 14.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "$totalQuantity টি",
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
                                        text = "মোট পরিমাণ:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DarkTeal
                                    )
                                    Text(
                                        text = "৳ ${String.format("%.2f", totalAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal
                                    )
                                }
                            }
                        }
                    }
                    
                    // Delivery Address
                    item {
                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text("ডেলিভারি ঠিকানা") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkTeal,
                                focusedLabelColor = DarkTeal
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    
                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("বিশেষ নির্দেশনা (ঐচ্ছিক)") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkTeal,
                                focusedLabelColor = DarkTeal
                            ),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2
                        )
                    }
                    
                    // Priority Selection
                    item {
                        Text(
                            text = "অগ্রাধিকার",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkTeal,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderPriority.values().forEach { priority ->
                                FilterChip(
                                    onClick = { orderPriority = priority },
                                    label = { 
                                        Text(
                                            text = priority.displayNameBn,
                                            fontSize = 12.sp
                                        )
                                    },
                                    selected = orderPriority == priority,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DarkTeal,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkTeal
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = DarkTeal
                        )
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            if (totalQuantity > 0) {
                                val orderItems = cylinderItems
                                    .filter { it.quantity > 0 }
                                    .map { cylinderItem ->
                                        OrderItem(
                                            cylinderType = cylinderItem.cylinderType,
                                            quantity = cylinderItem.quantity,
                                            unitPrice = cylinderItem.unitPrice,
                                            totalPrice = cylinderItem.totalPrice
                                        )
                                    }
                                
                                val order = Order(
                                    customerId = customer.id,
                                    customerName = customer.name,
                                    customerPhone = customer.phone,
                                    deliveryAddress = deliveryAddress,
                                    orderItems = orderItems,
                                    totalAmount = totalAmount,
                                    notes = notes,
                                    priority = orderPriority,
                                    deliveryArea = customer.area
                                )
                                
                                onOrderCreated(order)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = totalQuantity > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal
                        )
                    ) {
                        Text("অর্ডার তৈরি করুন")
                    }
                }
            }
        }
    }
}

@Composable
private fun CylinderOrderItemCard(
    item: CylinderOrderItem,
    onQuantityChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FA)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cylinder Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.cylinderType.displayNameBn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DarkTeal
                )
                Text(
                    text = "৳ ${String.format("%.0f", item.unitPrice)} প্রতি টি",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                if (item.quantity > 0) {
                    Text(
                        text = "মোট: ৳ ${String.format("%.2f", item.totalPrice)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                }
            }
            
            // Quantity Controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = { onQuantityChange(item.quantity - 1) },
                    enabled = item.quantity > 0
                ) {
                    Text(
                        text = "−",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.quantity > 0) DarkTeal else Color.Gray
                    )
                }
                
                Text(
                    text = "${item.quantity}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(40.dp)
                )
                
                IconButton(
                    onClick = { onQuantityChange(item.quantity + 1) }
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "বাড়ান",
                        tint = DarkTeal
                    )
                }
            }
        }
    }
}
