package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesReportDialog(
    onDismiss: () -> Unit,
    onSalesReportCreated: () -> Unit,
    orderViewModel: OrderViewModel = viewModel(),
    customerViewModel: CustomerViewModel = viewModel()
) {
    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var deliveryAddress by remember { mutableStateOf("") }
    var salesAmount by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("নগদ") }
    var notes by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var showCustomerSuggestions by remember { mutableStateOf(false) }
    
    // Customer suggestions
    val customers by customerViewModel.customers.collectAsState()
    val filteredCustomers = remember(customerName, customers) {
        if (customerName.isBlank()) emptyList()
        else customers.filter { 
            it.name.contains(customerName, ignoreCase = true) || 
            it.phone.contains(customerName, ignoreCase = true)
        }.take(5)
    }
    
    // Cylinder order items
    var cylinderItems by remember {
        mutableStateOf(
            CylinderType.values().filter { !it.name.startsWith("KG_") }.map { type ->
                CylinderOrderItem(
                    cylinderType = type,
                    quantity = 0,
                    unitPrice = getDefaultPrice(type)
                )
            }
        )
    }
    
    val totalAmount = cylinderItems.sumOf { it.totalPrice }
    val totalQuantity = cylinderItems.sumOf { it.quantity }
    
    val paymentMethods = listOf("নগদ", "বিকাশ", "নগদ", "রকেট", "ব্যাংক")
    var showPaymentDropdown by remember { mutableStateOf(false) }
    
    // Get current date
    val currentDate = remember {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
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
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "নতুন বিক্রির হিসাব",
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
                
                // Date Display
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = LightTeal.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DateRange,
                            contentDescription = "Date",
                            tint = DarkTeal,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "তারিখ: $currentDate",
                            fontSize = 14.sp,
                            color = DarkTeal
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Customer Name with Suggestions
                Column {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { 
                            customerName = it
                            showCustomerSuggestions = it.isNotBlank()
                        },
                        label = { Text("গ্রাহকের নাম") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkTeal,
                            focusedLabelColor = DarkTeal
                        )
                    )
                    
                    // Customer Suggestions
                    if (showCustomerSuggestions && filteredCustomers.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
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
                                            contentDescription = "Customer",
                                            tint = DarkTeal,
                                            modifier = Modifier.size(20.dp)
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
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Customer Phone
                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("ফোন নম্বর") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    )
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Sales Amount
                OutlinedTextField(
                    value = salesAmount,
                    onValueChange = { salesAmount = it },
                    label = { Text("বিক্রির পরিমাণ (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    ),
                    leadingIcon = {
                        Text(
                            text = "৳",
                            fontSize = 16.sp,
                            color = DarkTeal,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Payment Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = showPaymentDropdown,
                    onExpandedChange = { showPaymentDropdown = !showPaymentDropdown }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("পেমেন্ট পদ্ধতি") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkTeal,
                            focusedLabelColor = DarkTeal
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showPaymentDropdown,
                        onDismissRequest = { showPaymentDropdown = false }
                    ) {
                        paymentMethods.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method) },
                                onClick = {
                                    paymentMethod = method
                                    showPaymentDropdown = false
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
                    label = { Text("নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    )
                )
                
                // Error Message
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
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
                        )
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            // Validate inputs
                            when {
                                customerName.isBlank() -> {
                                    errorMessage = "গ্রাহকের নাম প্রয়োজন"
                                    return@Button
                                }
                                customerPhone.isBlank() -> {
                                    errorMessage = "ফোন নম্বর প্রয়োজন"
                                    return@Button
                                }
                                salesAmount.isBlank() -> {
                                    errorMessage = "বিক্রির পরিমাণ প্রয়োজন"
                                    return@Button
                                }
                                salesAmount.toDoubleOrNull() == null || salesAmount.toDouble() <= 0 -> {
                                    errorMessage = "সঠিক বিক্রির পরিমাণ দিন"
                                    return@Button
                                }
                                else -> {
                                    errorMessage = ""
                                    isLoading = true
                                    
                                    // Create a simple order record for the sales report
                                    // Create sales order using enhanced method with customer details
                                    orderViewModel.createOrderWithCustomerDetails(
                                        customerName = customerName.trim(),
                                        customerPhone = customerPhone.trim(),
                                        deliveryAddress = "N/A", // Not required for sales reports
                                        orderItems = listOf(), // Empty for direct sales
                                        priority = OrderPriority.NORMAL,
                                        estimatedDeliveryTime = "",
                                        notes = "বিক্রির হিসাব - $paymentMethod${if (notes.isNotEmpty()) " | $notes" else ""}"
                                    )
                                    
                                    isLoading = false
                                    onSalesReportCreated()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("সংরক্ষণ করুন")
                        }
                    }
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
