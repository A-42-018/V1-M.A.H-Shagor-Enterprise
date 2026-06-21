package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@Composable
fun CreateOrderDialog(
    order: Order? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, List<OrderItem>, OrderPriority, String, String) -> Unit,
    orderViewModel: OrderViewModel = viewModel()
) {
    var selectedCustomerId by remember { mutableStateOf(order?.customerId ?: "") }
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var deliveryAddress by remember { mutableStateOf(order?.deliveryAddress ?: "") }
    var orderItems by remember { mutableStateOf(order?.orderItems ?: emptyList()) }
    var selectedPriority by remember { mutableStateOf(order?.priority ?: OrderPriority.NORMAL) }
    var estimatedDeliveryTime by remember { mutableStateOf(order?.estimatedDeliveryTime ?: "") }
    var notes by remember { mutableStateOf(order?.notes ?: "") }
    var showCustomerSelector by remember { mutableStateOf(false) }
    var showAddItemDialog by remember { mutableStateOf(false) }
    
    val availableCustomers by orderViewModel.availableCustomers.collectAsState()
    val isEditing = order != null
    
    // Update selected customer when ID changes
    LaunchedEffect(selectedCustomerId) {
        selectedCustomer = orderViewModel.getCustomerById(selectedCustomerId)
        if (selectedCustomer != null && deliveryAddress.isBlank()) {
            deliveryAddress = selectedCustomer!!.address
        }
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
                    Text(
                        text = if (isEditing) "অর্ডার সম্পাদনা" else "নতুন অর্ডার",
                        fontSize = 20.sp,
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
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Customer Selection
                    item {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "গ্রাহক নির্বাচন করুন",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("গ্রাহক *") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCustomerSelector = true },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Customer",
                                    tint = DarkTeal
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Select",
                                    tint = DarkTeal
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkTeal,
                                focusedLabelColor = DarkTeal,
                                focusedLeadingIconColor = DarkTeal
                            )
                        )
                        
                        if (selectedCustomer != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ফোন: ${selectedCustomer!!.phone} | এলাকা: ${selectedCustomer!!.area}",
                                fontSize = 12.sp,
                                color = DarkTeal.copy(alpha = 0.7f)
                            )
                        }
                    }
                    
                    // Delivery Address
                    item {
                        OutlinedTextField(
                            value = deliveryAddress,
                            onValueChange = { deliveryAddress = it },
                            label = { Text("ডেলিভারি ঠিকানা *") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.LocationOn,
                                    contentDescription = "Address",
                                    tint = DarkTeal
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkTeal,
                                focusedLabelColor = DarkTeal,
                                focusedLeadingIconColor = DarkTeal
                            ),
                            maxLines = 2
                        )
                    }
                    
                    // Priority Selection
                    item {
                        Text(
                            text = "অগ্রাধিকার",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkTeal
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OrderPriority.values().forEach { priority ->
                                FilterChip(
                                    onClick = { selectedPriority = priority },
                                    label = { Text(priority.displayNameBn, fontSize = 12.sp) },
                                    selected = selectedPriority == priority,
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = getPriorityColor(priority).copy(alpha = 0.2f),
                                        selectedLabelColor = getPriorityColor(priority),
                                        containerColor = Color.Gray.copy(alpha = 0.2f),
                                        labelColor = DarkTeal
                                    )
                                )
                            }
                        }
                    }
                    
                    // Estimated Delivery Time
                    item {
                        OutlinedTextField(
                            value = estimatedDeliveryTime,
                            onValueChange = { estimatedDeliveryTime = it },
                            label = { Text("আনুমানিক ডেলিভারি সময়") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "Time",
                                    tint = DarkTeal
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkTeal,
                                focusedLabelColor = DarkTeal,
                                focusedLeadingIconColor = DarkTeal
                            ),
                            placeholder = { Text("যেমন: সকাল ১০টা, দুপুর ২টা") }
                        )
                    }
                    
                    // Order Items Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "অর্ডার আইটেম *",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = DarkTeal
                            )
                            
                            Button(
                                onClick = { showAddItemDialog = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkTeal,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Add Item",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("যোগ করুন", fontSize = 12.sp)
                            }
                        }
                    }
                    
                    // Order Items List
                    items(orderItems) { item ->
                        OrderItemCard(
                            item = item,
                            onEdit = { editedItem ->
                                orderItems = orderItems.map { 
                                    if (it.id == editedItem.id) editedItem else it 
                                }
                            },
                            onDelete = { itemId ->
                                orderItems = orderItems.filter { it.id != itemId }
                            }
                        )
                    }
                    
                    // Total Amount
                    if (orderItems.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DarkTeal.copy(alpha = 0.1f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "মোট পরিমাণ:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = DarkTeal
                                    )
                                    Text(
                                        text = "৳${calculateOrderTotal(orderItems).toInt()}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal
                                    )
                                }
                            }
                        }
                    }
                    
                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("নোট (ঐচ্ছিক)") },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = "Notes",
                                    tint = DarkTeal
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkTeal,
                                focusedLabelColor = DarkTeal,
                                focusedLeadingIconColor = DarkTeal
                            ),
                            maxLines = 3
                        )
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
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkTeal
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkTeal)
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            onSave(
                                selectedCustomerId,
                                deliveryAddress.trim(),
                                orderItems,
                                selectedPriority,
                                estimatedDeliveryTime.trim(),
                                notes.trim()
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal,
                            contentColor = Color.White
                        ),
                        enabled = selectedCustomerId.isNotBlank() && 
                                deliveryAddress.isNotBlank() && 
                                orderItems.isNotEmpty()
                    ) {
                        Text(if (isEditing) "আপডেট" else "সংরক্ষণ")
                    }
                }
            }
        }
    }
    
    // Customer Selector Dialog
    if (showCustomerSelector) {
        CustomerSelectorDialog(
            customers = availableCustomers,
            onCustomerSelected = { customer ->
                selectedCustomerId = customer.id
                showCustomerSelector = false
            },
            onDismiss = { showCustomerSelector = false }
        )
    }
    
    // Add Item Dialog
    if (showAddItemDialog) {
        AddOrderItemDialog(
            onItemAdded = { newItem ->
                orderItems = orderItems + newItem
                showAddItemDialog = false
            },
            onDismiss = { showAddItemDialog = false }
        )
    }
}

@Composable
private fun CustomerSelectorDialog(
    customers: List<Customer>,
    onCustomerSelected: (Customer) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "গ্রাহক নির্বাচন করুন",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn {
                    items(customers) { customer ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCustomerSelected(customer) }
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = customer.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = DarkTeal
                                )
                                Text(
                                    text = customer.phone,
                                    fontSize = 14.sp,
                                    color = DarkTeal.copy(alpha = 0.7f)
                                )
                                if (customer.area.isNotBlank()) {
                                    Text(
                                        text = customer.area,
                                        fontSize = 12.sp,
                                        color = DarkTeal.copy(alpha = 0.6f)
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

@Composable
private fun AddOrderItemDialog(
    onItemAdded: (OrderItem) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCylinderType by remember { mutableStateOf<CylinderType?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var unitPrice by remember { mutableStateOf("") }
    var itemNotes by remember { mutableStateOf("") }
    var showCylinderTypeSelector by remember { mutableStateOf(false) }
    
    // Auto-fill price when cylinder type is selected
    LaunchedEffect(selectedCylinderType) {
        selectedCylinderType?.let { type ->
            unitPrice = getDefaultPrice(type).toString()
        }
    }
    
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
                Text(
                    text = "নতুন আইটেম যোগ করুন",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Cylinder Type Selection
                OutlinedTextField(
                    value = selectedCylinderType?.displayNameBn ?: "সিলিন্ডারের ধরন নির্বাচন করুন",
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("সিলিন্ডারের ধরন *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCylinderTypeSelector = true },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Type",
                            tint = DarkTeal
                        )
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = "Select",
                            tint = DarkTeal
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("পরিমাণ *") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.List,
                            contentDescription = "Quantity",
                            tint = DarkTeal
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Unit Price
                OutlinedTextField(
                    value = unitPrice,
                    onValueChange = { unitPrice = it },
                    label = { Text("একক দাম (টাকা) *") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Price",
                            tint = DarkTeal
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    )
                )
                
                // Total Price Display
                val qty = quantity.toIntOrNull() ?: 0
                val price = unitPrice.toDoubleOrNull() ?: 0.0
                val total = qty * price
                
                if (total > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkTeal.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "মোট: ৳${total.toInt()}",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Item Notes
                OutlinedTextField(
                    value = itemNotes,
                    onValueChange = { itemNotes = it },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    ),
                    maxLines = 2
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
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
                            if (selectedCylinderType != null && qty > 0 && price > 0) {
                                val newItem = OrderItem(
                                    cylinderType = selectedCylinderType!!,
                                    quantity = qty,
                                    unitPrice = price,
                                    totalPrice = total,
                                    notes = itemNotes.trim()
                                )
                                onItemAdded(newItem)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal,
                            contentColor = Color.White
                        ),
                        enabled = selectedCylinderType != null && qty > 0 && price > 0
                    ) {
                        Text("যোগ করুন")
                    }
                }
            }
        }
    }
    
    // Cylinder Type Selector
    if (showCylinderTypeSelector) {
        Dialog(onDismissRequest = { showCylinderTypeSelector = false }) {
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
                        .padding(16.dp)
                ) {
                    Text(
                        text = "সিলিন্ডারের ধরন নির্বাচন করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(CylinderType.values().filter { !it.name.startsWith("KG_") }) { type ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedCylinderType = type
                                        showCylinderTypeSelector = false
                                    }
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = type.displayNameBn,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = DarkTeal
                                        )
                                        Text(
                                            text = type.displayName,
                                            fontSize = 12.sp,
                                            color = DarkTeal.copy(alpha = 0.7f)
                                        )
                                    }
                                    Text(
                                        text = "৳${getDefaultPrice(type).toInt()}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DarkTeal
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

@Composable
private fun OrderItemCard(
    item: OrderItem,
    onEdit: (OrderItem) -> Unit,
    onDelete: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
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
                    text = "${item.quantity} টি × ৳${item.unitPrice.toInt()} = ৳${item.totalPrice.toInt()}",
                    fontSize = 12.sp,
                    color = DarkTeal.copy(alpha = 0.7f)
                )
                if (item.notes.isNotBlank()) {
                    Text(
                        text = item.notes,
                        fontSize = 11.sp,
                        color = DarkTeal.copy(alpha = 0.6f)
                    )
                }
            }
            
            IconButton(
                onClick = { onDelete(item.id) },
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
}


// Helper function to get default price for cylinder types
private fun getDefaultPrice(type: CylinderType): Double {
    return when (type) {
        // New branded cylinder types
        CylinderType.OMERA_12KG -> 1200.0
        CylinderType.OMERA_25KG -> 2200.0
        CylinderType.OMERA_35KG -> 3000.0
        CylinderType.LAUGFS_12KG -> 1250.0
        CylinderType.TOTAL_12KG -> 1180.0
        CylinderType.TOTAL_15KG -> 1450.0
        
        // Legacy types for backward compatibility
        CylinderType.KG_12 -> 1200.0
        CylinderType.KG_25 -> 2500.0
        CylinderType.KG_35 -> 3500.0
    }
}
