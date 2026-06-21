package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onStatusUpdate: () -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    val customer by remember { 
        derivedStateOf { orderViewModel.getCustomerById(order.customerId) }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "অর্ডার বিস্তারিত",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                        Text(
                            text = order.orderNumber,
                            fontSize = 14.sp,
                            color = DarkTeal.copy(alpha = 0.7f)
                        )
                    }
                    
                    Row {
                        IconButton(onClick = onEdit) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = "Edit",
                                tint = DarkTeal
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = DarkTeal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Order Status Section
                    item {
                        OrderStatusSection(order = order, onStatusUpdate = onStatusUpdate)
                    }
                    
                    // Customer Information
                    item {
                        CustomerInfoSection(customer = customer, order = order)
                    }
                    
                    // Order Items
                    item {
                        OrderItemsSection(orderItems = order.orderItems)
                    }
                    
                    // Payment Information
                    item {
                        PaymentInfoSection(order = order)
                    }
                    
                    // Delivery Information
                    item {
                        DeliveryInfoSection(order = order)
                    }
                    
                    // Additional Information
                    item {
                        AdditionalInfoSection(order = order)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStatusSection(
    order: Order,
    onStatusUpdate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
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
                Text(
                    text = "অর্ডার স্ট্যাটাস",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                Button(
                    onClick = onStatusUpdate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkTeal,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("আপডেট", fontSize = 12.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Order Status
                StatusChip(
                    label = "অর্ডার",
                    status = order.orderStatus.displayNameBn,
                    color = getOrderStatusColor(order.orderStatus),
                    modifier = Modifier.weight(1f)
                )
                
                // Payment Status
                StatusChip(
                    label = "পেমেন্ট",
                    status = order.paymentStatus.displayNameBn,
                    color = getPaymentStatusColor(order.paymentStatus),
                    modifier = Modifier.weight(1f)
                )
                
                // Delivery Status
                StatusChip(
                    label = "ডেলিভারি",
                    status = order.deliveryStatus.displayNameBn,
                    color = getDeliveryStatusColor(order.deliveryStatus),
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Priority Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "অগ্রাধিকার:",
                    fontSize = 14.sp,
                    color = DarkTeal.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(getPriorityColor(order.priority))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = order.priority.displayNameBn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = getPriorityColor(order.priority)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = DarkTeal.copy(alpha = 0.6f)
            )
            Text(
                text = status,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CustomerInfoSection(
    customer: Customer?,
    order: Order
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "গ্রাহকের তথ্য",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (customer != null) {
                InfoRow(
                    icon = Icons.Filled.Person,
                    label = "নাম",
                    value = customer.name
                )
                InfoRow(
                    icon = Icons.Filled.Phone,
                    label = "ফোন",
                    value = customer.phone
                )
                InfoRow(
                    icon = Icons.Filled.LocationOn,
                    label = "এলাকা",
                    value = customer.area
                )
                InfoRow(
                    icon = Icons.Filled.List,
                    label = "ধরন",
                    value = customer.customerType.displayNameBn
                )
            } else {
                Text(
                    text = "গ্রাহকের তথ্য পাওয়া যায়নি",
                    fontSize = 14.sp,
                    color = Color.Red,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            InfoRow(
                icon = Icons.Filled.Home,
                label = "ডেলিভারি ঠিকানা",
                value = order.deliveryAddress
            )
        }
    }
}

@Composable
private fun OrderItemsSection(orderItems: List<OrderItem>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "অর্ডার আইটেম (${orderItems.size}টি)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            orderItems.forEach { item ->
                OrderItemDetailCard(item = item)
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Total Summary
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkTeal.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "মোট পরিমাণ:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                    Text(
                        text = "৳${orderItems.sumOf { it.totalPrice }.toInt()}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                }
            }
        }
    }
}

@Composable
private fun OrderItemDetailCard(item: OrderItem) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.cylinderType.displayNameBn,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                Text(
                    text = "${item.quantity} টি × ৳${item.unitPrice.toInt()}",
                    fontSize = 12.sp,
                    color = DarkTeal.copy(alpha = 0.7f)
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        fontSize = 11.sp,
                        color = DarkTeal.copy(alpha = 0.6f),
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
            
            Text(
                text = "৳${item.totalPrice.toInt()}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
        }
    }
}

@Composable
private fun PaymentInfoSection(order: Order) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "পেমেন্ট তথ্য",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                icon = Icons.Filled.Star,
                label = "মোট পরিমাণ",
                value = "৳${order.totalAmount.toInt()}"
            )
            InfoRow(
                icon = Icons.Filled.Star,
                label = "পরিশোধিত",
                value = "৳${order.paidAmount.toInt()}"
            )
            InfoRow(
                icon = Icons.Filled.Warning,
                label = "বকেয়া",
                value = "৳${order.remainingAmount.toInt()}",
                valueColor = if (order.remainingAmount > 0) Color.Red else Color.Green
            )
        }
    }
}

@Composable
private fun DeliveryInfoSection(order: Order) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "ডেলিভারি তথ্য",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            InfoRow(
                icon = Icons.Filled.Info,
                label = "আনুমানিক সময়",
                value = order.estimatedDeliveryTime.ifBlank { "নির্ধারিত নয়" }
            )
            InfoRow(
                icon = Icons.Filled.DateRange,
                label = "অর্ডারের তারিখ",
                value = order.orderDate
            )
            if (order.deliveryDate.isNotBlank()) {
                InfoRow(
                    icon = Icons.Filled.CheckCircle,
                    label = "ডেলিভারির তারিখ",
                    value = order.deliveryDate
                )
            }
        }
    }
}

@Composable
private fun AdditionalInfoSection(order: Order) {
    if (order.notes.isNotBlank()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "অতিরিক্ত তথ্য",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = order.notes,
                    fontSize = 14.sp,
                    color = DarkTeal.copy(alpha = 0.8f),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = DarkTeal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = DarkTeal.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label:",
            fontSize = 14.sp,
            color = DarkTeal.copy(alpha = 0.7f),
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun OrderStatusUpdateDialog(
    order: Order,
    onDismiss: () -> Unit,
    onStatusUpdate: (String, OrderStatus, PaymentStatus?, DeliveryStatus?, Double) -> Unit
) {
    var selectedOrderStatus by remember { mutableStateOf(order.orderStatus) }
    var selectedPaymentStatus by remember { mutableStateOf(order.paymentStatus) }
    var selectedDeliveryStatus by remember { mutableStateOf(order.deliveryStatus) }
    var paidAmount by remember { mutableStateOf(order.paidAmount.toString()) }
    var updatePayment by remember { mutableStateOf(false) }
    var updateDelivery by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "স্ট্যাটাস আপডেট",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = DarkTeal
                        )
                    }
                }
                
                Text(
                    text = order.orderNumber,
                    fontSize = 14.sp,
                    color = DarkTeal.copy(alpha = 0.7f)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Order Status
                Text(
                    text = "অর্ডার স্ট্যাটাস",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkTeal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrderStatus.values().forEach { status ->
                        FilterChip(
                            onClick = { selectedOrderStatus = status },
                            label = { Text(status.displayNameBn, fontSize = 12.sp) },
                            selected = selectedOrderStatus == status,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = getOrderStatusColor(status).copy(alpha = 0.2f),
                                selectedLabelColor = getOrderStatusColor(status),
                                containerColor = Color.Gray.copy(alpha = 0.2f),
                                labelColor = DarkTeal
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Payment Status Update
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = updatePayment,
                        onCheckedChange = { updatePayment = it },
                        colors = CheckboxDefaults.colors(checkedColor = DarkTeal)
                    )
                    Text(
                        text = "পেমেন্ট স্ট্যাটাস আপডেট করুন",
                        fontSize = 14.sp,
                        color = DarkTeal
                    )
                }
                
                if (updatePayment) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaymentStatus.values().forEach { status ->
                            FilterChip(
                                onClick = { selectedPaymentStatus = status },
                                label = { Text(status.displayNameBn, fontSize = 12.sp) },
                                selected = selectedPaymentStatus == status,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getPaymentStatusColor(status).copy(alpha = 0.2f),
                                    selectedLabelColor = getPaymentStatusColor(status),
                                    containerColor = Color.Gray.copy(alpha = 0.2f),
                                    labelColor = DarkTeal
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = paidAmount,
                        onValueChange = { paidAmount = it },
                        label = { Text("পরিশোধিত পরিমাণ") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkTeal,
                            focusedLabelColor = DarkTeal
                        )
                    )
                    
                    val paid = paidAmount.toDoubleOrNull() ?: 0.0
                    val remaining = order.totalAmount - paid
                    
                    Text(
                        text = "বকেয়া: ৳${remaining.toInt()}",
                        fontSize = 12.sp,
                        color = if (remaining > 0) Color.Red else Color.Green,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Delivery Status Update
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = updateDelivery,
                        onCheckedChange = { updateDelivery = it },
                        colors = CheckboxDefaults.colors(checkedColor = DarkTeal)
                    )
                    Text(
                        text = "ডেলিভারি স্ট্যাটাস আপডেট করুন",
                        fontSize = 14.sp,
                        color = DarkTeal
                    )
                }
                
                if (updateDelivery) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DeliveryStatus.values().forEach { status ->
                            FilterChip(
                                onClick = { selectedDeliveryStatus = status },
                                label = { Text(status.displayNameBn, fontSize = 12.sp) },
                                selected = selectedDeliveryStatus == status,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = getDeliveryStatusColor(status).copy(alpha = 0.2f),
                                    selectedLabelColor = getDeliveryStatusColor(status),
                                    containerColor = Color.Gray.copy(alpha = 0.2f),
                                    labelColor = DarkTeal
                                )
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkTeal)
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            val paymentStatusToUpdate = if (updatePayment) selectedPaymentStatus else null
                            val deliveryStatusToUpdate = if (updateDelivery) selectedDeliveryStatus else null
                            val paidAmountValue = paidAmount.toDoubleOrNull() ?: order.paidAmount
                            
                            onStatusUpdate(
                                order.id,
                                selectedOrderStatus,
                                paymentStatusToUpdate,
                                deliveryStatusToUpdate,
                                paidAmountValue
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal,
                            contentColor = Color.White
                        )
                    ) {
                        Text("আপডেট")
                    }
                }
            }
        }
    }
}
