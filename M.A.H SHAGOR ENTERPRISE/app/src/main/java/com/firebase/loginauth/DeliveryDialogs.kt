package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@Composable
fun RunningOrderCard(
    order: Order,
    onConvertToDelivery: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        text = order.orderNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = order.customerName,
                        fontSize = 14.sp,
                        color = DarkGray
                    )
                }
                
                // Status Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (order.orderStatus) {
                            OrderStatus.PENDING -> Color(0xFFFF9800)
                            OrderStatus.CONFIRMED -> Color(0xFF2196F3)
                            else -> LightGray
                        }
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = when (order.orderStatus) {
                            OrderStatus.PENDING -> "পেন্ডিং"
                            OrderStatus.CONFIRMED -> "নিশ্চিত"
                            else -> "অজানা"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
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
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Filled.Phone,
                        label = "ফোন",
                        value = order.customerPhone
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.LocationOn,
                        label = "ঠিকানা",
                        value = order.deliveryAddress
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.ShoppingCart,
                        label = "পণ্য",
                        value = order.orderItems.joinToString(", ") { "${it.cylinderType} (${it.quantity}টি)" }
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Filled.DateRange,
                        label = "অর্ডার তারিখ",
                        value = order.orderDate
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.Star,
                        label = "মোট টাকা",
                        value = "৳${order.totalAmount.toInt()}"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.Star,
                        label = "অগ্রাধিকার",
                        value = when (order.priority) {
                            OrderPriority.HIGH -> "উচ্চ"
                            OrderPriority.URGENT -> "জরুরি"
                            else -> "সাধারণ"
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Convert to Delivery Button
                Button(
                    onClick = onConvertToDelivery,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                ) {
                    Icon(
                        Icons.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ডেলিভারিতে পাঠান", fontSize = 12.sp)
                }
                
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(0.7f)
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সম্পাদনা", fontSize = 12.sp)
                }
                
                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(0.7f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Red
                    )
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মুছুন", fontSize = 12.sp)
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("নিশ্চিত করুন") },
            text = { Text("আপনি কি এই অর্ডারটি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("হ্যাঁ", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmation = false }
                ) {
                    Text("না")
                }
            }
        )
    }
}

@Composable
fun DeliveryCard(
    delivery: DeliveryEntry,
    onUpdateStatus: () -> Unit,
    onAssignPerson: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                        text = delivery.orderNumber,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = delivery.customerName,
                        fontSize = 14.sp,
                        color = DarkGray
                    )
                }
                
                // Status Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getDeliveryStatusColor(delivery.deliveryStatus)
                    ),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = delivery.deliveryStatus.displayNameBn,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Filled.Phone,
                        label = "ফোন",
                        value = delivery.customerPhone
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.LocationOn,
                        label = "ঠিকানা",
                        value = delivery.deliveryAddress
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.ShoppingCart,
                        label = "পণ্য",
                        value = delivery.cylinderDetails
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow(
                        icon = Icons.Filled.DateRange,
                        label = "তারিখ",
                        value = delivery.scheduledDate
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.Person,
                        label = "ডেলিভারি পার্সন",
                        value = delivery.deliveryPerson.ifEmpty { "নিযুক্ত নয়" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        icon = Icons.Filled.Star,
                        label = "মূল্য",
                        value = "৳${delivery.totalAmount.toInt()}"
                    )
                }
            }
            
            if (delivery.priority != DeliveryPriority.NORMAL) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getDeliveryPriorityColor(delivery.priority)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "অগ্রাধিকার: ${delivery.priority.displayNameBn}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp
                    )
                }
            }
            
            if (delivery.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "নোট: ${delivery.notes}",
                    fontSize = 12.sp,
                    color = DarkGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LightGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Update Status Button
                OutlinedButton(
                    onClick = onUpdateStatus,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = DarkTeal
                    )
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("স্ট্যাটাস", fontSize = 12.sp)
                }
                
                // Assign Person Button
                OutlinedButton(
                    onClick = onAssignPerson,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF2196F3)
                    )
                ) {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("নিযুক্ত", fontSize = 12.sp)
                }
                
                // Edit Button
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFFF9800)
                    )
                ) {
                    Icon(
                        Icons.Filled.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("সম্পাদনা", fontSize = 12.sp)
                }
                
                // Delete Button
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("মুছুন", fontSize = 12.sp)
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("নিশ্চিত করুন") },
            text = { Text("আপনি কি এই ডেলিভারি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("হ্যাঁ", color = Color(0xFFF44336))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("না")
                }
            }
        )
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = LightGray
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 10.sp,
                color = LightGray
            )
            Text(
                text = value,
                fontSize = 12.sp,
                color = DarkGray,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeliveryDialog(
    delivery: DeliveryEntry? = null,
    onDismiss: () -> Unit,
    onSave: (DeliveryEntry) -> Unit,
    deliveryViewModel: DeliveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    var orderNumber by remember { mutableStateOf(delivery?.orderNumber ?: "") }
    var customerName by remember { mutableStateOf(delivery?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(delivery?.customerPhone ?: "") }
    var deliveryAddress by remember { mutableStateOf(delivery?.deliveryAddress ?: "") }
    var cylinderDetails by remember { mutableStateOf(delivery?.cylinderDetails ?: "") }
    var totalAmount by remember { mutableStateOf(delivery?.totalAmount?.toString() ?: "") }
    var scheduledDate by remember { mutableStateOf(delivery?.scheduledDate ?: getCurrentDateForDelivery()) }
    var scheduledTime by remember { mutableStateOf(delivery?.scheduledTime ?: "") }
    var deliveryArea by remember { mutableStateOf(delivery?.deliveryArea ?: "") }
    var priority by remember { mutableStateOf(delivery?.priority ?: DeliveryPriority.NORMAL) }
    var notes by remember { mutableStateOf(delivery?.notes ?: "") }
    
    var showPriorityDropdown by remember { mutableStateOf(false) }
    var showCustomerSuggestions by remember { mutableStateOf(false) }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    
    // Customer suggestions from ViewModel
    val customerSuggestions by deliveryViewModel.customerSuggestions.collectAsState()
    
    // Load initial customer suggestions
    LaunchedEffect(Unit) {
        deliveryViewModel.loadActiveCustomers()
    }
    
    // Search customers when name changes
    LaunchedEffect(customerName) {
        if (customerName.isNotEmpty() && customerName.length >= 2) {
            deliveryViewModel.searchCustomers(customerName)
        } else if (customerName.isEmpty()) {
            deliveryViewModel.loadActiveCustomers()
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (delivery != null) "ডেলিভারি সম্পাদনা" else "নতুন ডেলিভারি",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Order Number
                OutlinedTextField(
                    value = orderNumber,
                    onValueChange = { orderNumber = it },
                    label = { Text("অর্ডার নম্বর *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Customer Name with Autocomplete
                Column {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { 
                            customerName = it
                            showCustomerSuggestions = it.isNotEmpty()
                            selectedCustomer = null
                        },
                        label = { Text("কাস্টমার নাম *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (customerSuggestions.isNotEmpty() && showCustomerSuggestions) {
                                Icon(
                                    Icons.Filled.ArrowDropDown,
                                    contentDescription = "সাজেশন দেখান",
                                    modifier = Modifier.clickable {
                                        showCustomerSuggestions = !showCustomerSuggestions
                                    }
                                )
                            }
                        }
                    )
                    
                    // Customer Suggestions Dropdown
                    if (showCustomerSuggestions && customerSuggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            LazyColumn {
                                items(customerSuggestions.take(5)) { customer ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedCustomer = customer
                                                customerName = customer.name
                                                customerPhone = customer.phone
                                                deliveryAddress = customer.address
                                                deliveryArea = customer.area
                                                showCustomerSuggestions = false
                                                deliveryViewModel.clearCustomerSuggestions()
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = customer.name,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkTeal
                                        )
                                        Text(
                                            text = customer.phone,
                                            fontSize = 12.sp,
                                            color = DarkGray
                                        )
                                        Text(
                                            text = "${customer.area} - ${customer.address}",
                                            fontSize = 11.sp,
                                            color = LightGray,
                                            maxLines = 1
                                        )
                                        if (customer != customerSuggestions.last()) {
                                            Divider(
                                                modifier = Modifier.padding(top = 8.dp),
                                                color = LightGray.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Customer Phone (auto-filled from selection)
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("ফোন নম্বর *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = selectedCustomer == null // Disable if customer is selected from suggestions
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Delivery Address (auto-filled from selection)
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    label = { Text("ডেলিভারি ঠিকানা *") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    trailingIcon = if (selectedCustomer != null) {
                        {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "কাস্টমার নির্বাচিত",
                                tint = Color.Green
                            )
                        }
                    } else null
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Cylinder Details
                OutlinedTextField(
                    value = cylinderDetails,
                    onValueChange = { cylinderDetails = it },
                    label = { Text("সিলিন্ডার বিবরণ *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Amount
                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("মোট মূল্য *") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Delivery Area
                    OutlinedTextField(
                        value = deliveryArea,
                        onValueChange = { deliveryArea = it },
                        label = { Text("এলাকা") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scheduled Date
                    OutlinedTextField(
                        value = scheduledDate,
                        onValueChange = { scheduledDate = it },
                        label = { Text("তারিখ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    // Scheduled Time
                    OutlinedTextField(
                        value = scheduledTime,
                        onValueChange = { scheduledTime = it },
                        label = { Text("সময়") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Priority Dropdown
                ExposedDropdownMenuBox(
                    expanded = showPriorityDropdown,
                    onExpandedChange = { showPriorityDropdown = !showPriorityDropdown }
                ) {
                    OutlinedTextField(
                        value = priority.displayNameBn,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("অগ্রাধিকার") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPriorityDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showPriorityDropdown,
                        onDismissRequest = { showPriorityDropdown = false }
                    ) {
                        DeliveryPriority.values().forEach { priorityOption ->
                            DropdownMenuItem(
                                text = { Text(priorityOption.displayNameBn) },
                                onClick = {
                                    priority = priorityOption
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Buttons
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
                            if (orderNumber.isNotBlank() && customerName.isNotBlank() && 
                                customerPhone.isNotBlank() && deliveryAddress.isNotBlank() && 
                                cylinderDetails.isNotBlank() && totalAmount.isNotBlank()) {
                                
                                val deliveryEntry = DeliveryEntry(
                                    id = delivery?.id ?: "",
                                    orderId = delivery?.orderId ?: "",
                                    orderNumber = orderNumber,
                                    customerId = delivery?.customerId ?: "",
                                    customerName = customerName,
                                    customerPhone = customerPhone,
                                    deliveryAddress = deliveryAddress,
                                    cylinderDetails = cylinderDetails,
                                    totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                                    scheduledDate = scheduledDate,
                                    scheduledTime = scheduledTime,
                                    deliveryArea = deliveryArea,
                                    priority = priority,
                                    notes = notes,
                                    deliveryStatus = delivery?.deliveryStatus ?: DeliveryStatusType.PENDING,
                                    deliveryPerson = delivery?.deliveryPerson ?: "",
                                    deliveryPersonPhone = delivery?.deliveryPersonPhone ?: "",
                                    actualDeliveryDate = delivery?.actualDeliveryDate ?: "",
                                    actualDeliveryTime = delivery?.actualDeliveryTime ?: "",
                                    estimatedDuration = calculateEstimatedDeliveryTime(deliveryArea),
                                    createdBy = delivery?.createdBy ?: "System",
                                    timestamp = delivery?.timestamp ?: System.currentTimeMillis(),
                                    lastUpdated = getCurrentDateTimeForDelivery()
                                )
                                
                                onSave(deliveryEntry)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                    ) {
                        Text("সংরক্ষণ", color = Color.White)
                    }
                }
            }
        }
    }
}
