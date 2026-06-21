package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreamlinedOrderDialog(
    onDismiss: () -> Unit,
    onOrderCreated: () -> Unit,
    orderViewModel: OrderViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel()
) {
    var customerName by remember { mutableStateOf("") }
    var cylinderAmount by remember { mutableStateOf(1) }
    var selectedCylinderType by remember { mutableStateOf(CylinderType.KG_12) }
    var showCylinderTypeDropdown by remember { mutableStateOf(false) }
    var deliveryAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showCustomerSuggestions by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val availableCustomers by customerViewModel.customers.collectAsState()
    val focusManager = LocalFocusManager.current
    
    // Filter customers based on input
    val filteredCustomers = remember(customerName, availableCustomers) {
        if (customerName.isBlank()) {
            emptyList()
        } else {
            availableCustomers.filter { 
                it.name.contains(customerName, ignoreCase = true) && it.isActive 
            }.take(5)
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = {
                            focusManager.clearFocus()
                            showCustomerSuggestions = false
                        })
                    }
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "নতুন অর্ডার",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "বন্ধ করুন",
                            tint = DarkTeal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Customer Name Input with Suggestions
                Column {
                    Text(
                        text = "গ্রাহকের নাম *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { 
                            customerName = it
                            showCustomerSuggestions = it.isNotBlank()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("গ্রাহকের নাম লিখুন") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightTeal,
                            unfocusedBorderColor = SlateBluGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                tint = LightTeal
                            )
                        }
                    )
                    
                    // Customer Suggestions Dropdown
                    if (showCustomerSuggestions && filteredCustomers.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 200.dp)
                            ) {
                                items(filteredCustomers) { customer ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                customerName = customer.name
                                                deliveryAddress = customer.address
                                                showCustomerSuggestions = false
                                                focusManager.clearFocus()
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Person,
                                            contentDescription = null,
                                            tint = LightTeal,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        
                                        Spacer(modifier = Modifier.width(12.dp))
                                        
                                        Column {
                                            Text(
                                                text = customer.name,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DarkTeal
                                            )
                                            if (customer.address.isNotBlank()) {
                                                Text(
                                                    text = customer.address,
                                                    fontSize = 14.sp,
                                                    color = SlateBluGray,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Cylinder Type Selection
                Column {
                    Text(
                        text = "সিলিন্ডারের ধরন *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showCylinderTypeDropdown,
                        onExpandedChange = { showCylinderTypeDropdown = !showCylinderTypeDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCylinderType.displayNameBn,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                unfocusedBorderColor = SlateBluGray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Build,
                                    contentDescription = null,
                                    tint = LightTeal
                                )
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(
                                    expanded = showCylinderTypeDropdown
                                )
                            }
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showCylinderTypeDropdown,
                            onDismissRequest = { showCylinderTypeDropdown = false }
                        ) {
                            CylinderType.values().filter { !it.name.startsWith("KG_") }.forEach { cylinderType ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(
                                                text = cylinderType.displayNameBn,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = DarkTeal
                                            )
                                            if (cylinderType.brand.isNotBlank()) {
                                                Text(
                                                    text = cylinderType.brand,
                                                    fontSize = 14.sp,
                                                    color = SlateBluGray
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedCylinderType = cylinderType
                                        showCylinderTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Cylinder Amount Input with +/- Controls
                Column {
                    Text(
                        text = "সিলিন্ডারের পরিমাণ *",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Decrease Button with Long Press
                        var isDecreasing by remember { mutableStateOf(false) }
                        
                        Button(
                            onClick = { 
                                if (cylinderAmount > 1) {
                                    cylinderAmount--
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isDecreasing = true
                                            // Long press functionality
                                            while (isDecreasing && cylinderAmount > 1) {
                                                cylinderAmount--
                                                delay(150) // Adjust speed as needed
                                            }
                                        }
                                    )
                                },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = cylinderAmount > 1
                        ) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = "কমান",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        LaunchedEffect(Unit) {
                            // Stop decreasing when button is released
                            isDecreasing = false
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // Amount Display
                        Card(
                            modifier = Modifier.width(100.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkTeal.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = cylinderAmount.toString(),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkTeal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp)
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(24.dp))
                        
                        // Increase Button with Long Press
                        var isIncreasing by remember { mutableStateOf(false) }
                        
                        Button(
                            onClick = { 
                                if (cylinderAmount < 999) {
                                    cylinderAmount++
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = {
                                            isIncreasing = true
                                            // Long press functionality
                                            while (isIncreasing && cylinderAmount < 999) {
                                                cylinderAmount++
                                                delay(150) // Adjust speed as needed
                                            }
                                        }
                                    )
                                },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LightTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            enabled = cylinderAmount < 999
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "বাড়ান",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        LaunchedEffect(Unit) {
                            // Stop increasing when button is released
                            isIncreasing = false
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Optional Fields Section
                Text(
                    text = "অতিরিক্ত তথ্য (ঐচ্ছিক)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = SlateBluGray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Delivery Address
                OutlinedTextField(
                    value = deliveryAddress,
                    onValueChange = { deliveryAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("ডেলিভারি ঠিকানা") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = SlateBluGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = LightTeal
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("বিশেষ নির্দেশনা") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = SlateBluGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = null,
                            tint = LightTeal
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = SlateBluGray
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(SlateBluGray, SlateBluGray))
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "বাতিল",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    
                    // Create Order Button
                    Button(
                        onClick = {
                            if (customerName.isBlank()) {
                                return@Button
                            }
                            
                            isLoading = true
                            
                            // Create order with selected cylinder item using enhanced method
                            val orderItem = OrderItem(
                                cylinderType = selectedCylinderType,
                                quantity = cylinderAmount,
                                unitPrice = getDefaultPrice(selectedCylinderType),
                                totalPrice = cylinderAmount * getDefaultPrice(selectedCylinderType)
                            )
                            
                            // Use enhanced method that handles customer creation/lookup automatically
                            orderViewModel.createOrderWithCustomerDetails(
                                customerName = customerName.trim(),
                                customerPhone = "", // Phone not collected in streamlined dialog
                                deliveryAddress = deliveryAddress.ifBlank { "অনির্দিষ্ট" },
                                orderItems = listOf(orderItem),
                                priority = OrderPriority.NORMAL,
                                estimatedDeliveryTime = "",
                                notes = notes.trim()
                            )
                            
                            isLoading = false
                            onOrderCreated()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = customerName.isNotBlank() && !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightTeal,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "অর্ডার তৈরি করুন",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Mandatory field notice
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "* গ্রাহকের নাম, সিলিন্ডারের ধরন এবং পরিমাণ বাধ্যতামূলক",
                    fontSize = 12.sp,
                    color = SlateBluGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Helper function to get default price for cylinder types
private fun getDefaultPrice(type: CylinderType): Double {
    return when (type) {
        CylinderType.OMERA_12KG -> 1200.0
        CylinderType.OMERA_25KG -> 2500.0
        CylinderType.OMERA_35KG -> 3500.0
        CylinderType.LAUGFS_12KG -> 1250.0
        CylinderType.TOTAL_12KG -> 1180.0
        CylinderType.TOTAL_15KG -> 1400.0
        CylinderType.KG_12 -> 1200.0
        CylinderType.KG_25 -> 2500.0
        CylinderType.KG_35 -> 3500.0
    }
}
