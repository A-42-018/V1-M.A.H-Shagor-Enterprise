package com.firebase.loginauth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.firebase.loginauth.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDeliveryStatusDialog(
    delivery: DeliveryEntry,
    onDismiss: () -> Unit,
    onUpdate: (DeliveryStatusType, String) -> Unit,
    onUpdatePaymentStatus: ((PaymentStatus, Double) -> Unit)? = null,
    currentPaymentStatus: PaymentStatus? = null,
    totalAmount: Double = 0.0,
    currentPaidAmount: Double = 0.0
) {
    var selectedStatus by remember { mutableStateOf(delivery.deliveryStatus) }
    var notes by remember { mutableStateOf(delivery.notes) }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var selectedPaymentStatus by remember { mutableStateOf(currentPaymentStatus ?: PaymentStatus.PENDING) }
    var showPaymentDropdown by remember { mutableStateOf(false) }
    var paidAmount by remember { mutableStateOf(currentPaidAmount.toString()) }
    
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
                    text = "ডেলিভারি স্ট্যাটাস আপডেট",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Order Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "অর্ডার: ${delivery.orderNumber}",
                            fontWeight = FontWeight.Medium,
                            color = DarkTeal
                        )
                        Text(
                            text = "কাস্টমার: ${delivery.customerName}",
                            fontSize = 14.sp,
                            color = DarkGray
                        )
                        Text(
                            text = "বর্তমান স্ট্যাটাস: ${delivery.deliveryStatus.displayNameBn}",
                            fontSize = 14.sp,
                            color = DarkGray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Status Dropdown
                ExposedDropdownMenuBox(
                    expanded = showStatusDropdown,
                    onExpandedChange = { showStatusDropdown = !showStatusDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.displayNameBn,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("নতুন স্ট্যাটাস *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown)
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
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        DeliveryStatusType.values().forEach { status ->
                            DropdownMenuItem(
                                text = { 
                                    Text(
                                        text = status.displayNameBn,
                                        color = if (status == selectedStatus) DarkTeal else DarkGray
                                    )
                                },
                                onClick = {
                                    selectedStatus = status
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Payment Status (Cash Clearance) - Only show if callback is provided
                if (onUpdatePaymentStatus != null) {
                    ExposedDropdownMenuBox(
                        expanded = showPaymentDropdown,
                        onExpandedChange = { showPaymentDropdown = !showPaymentDropdown }
                    ) {
                        OutlinedTextField(
                            value = when (selectedPaymentStatus) {
                                PaymentStatus.PENDING -> "পেমেন্ট পেন্ডিং"
                                PaymentStatus.PARTIAL -> "আংশিক পেইড"
                                PaymentStatus.PAID -> "পেইড"
                                PaymentStatus.OVERDUE -> "বকেয়া"
                            },
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("পেমেন্ট স্ট্যাটাস *") },
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
                            PaymentStatus.values().forEach { status ->
                                DropdownMenuItem(
                                    text = { 
                                        Text(
                                            text = when (status) {
                                                PaymentStatus.PENDING -> "পেমেন্ট পেন্ডিং"
                                                PaymentStatus.PARTIAL -> "আংশিক পেইড"
                                                PaymentStatus.PAID -> "পেইড"
                                                PaymentStatus.OVERDUE -> "বকেয়া"
                                            },
                                            color = if (status == selectedPaymentStatus) DarkTeal else DarkGray
                                        )
                                    },
                                    onClick = {
                                        selectedPaymentStatus = status
                                        showPaymentDropdown = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Paid Amount Input
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
                    
                    // Automatic Due Calculation
                    val paid = paidAmount.toDoubleOrNull() ?: currentPaidAmount
                    val remaining = totalAmount - paid
                    
                    // Auto-update payment status based on payment amount
                    LaunchedEffect(paid, totalAmount) {
                        if (totalAmount > 0) {
                            selectedPaymentStatus = when {
                                paid <= 0 -> PaymentStatus.PENDING
                                paid >= totalAmount -> PaymentStatus.PAID
                                paid > 0 && paid < totalAmount -> PaymentStatus.PARTIAL
                                else -> PaymentStatus.PENDING
                            }
                        }
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (remaining > 0) Color.Red.copy(alpha = 0.1f) else Color.Green.copy(alpha = 0.1f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            Text(
                                text = "মোট বিল: ৳${totalAmount.toInt()}",
                                fontSize = 12.sp,
                                color = DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "পরিশোধিত: ৳${paid.toInt()}",
                                fontSize = 12.sp,
                                color = Color.Blue,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "বকেয়া: ৳${remaining.toInt()}",
                                fontSize = 14.sp,
                                color = if (remaining > 0) Color.Red else Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট/মন্তব্য") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    placeholder = { Text("স্ট্যাটাস আপডেটের কারণ বা অতিরিক্ত তথ্য লিখুন...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal
                    )
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkGray
                        )
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            // Update delivery status first
                            onUpdate(selectedStatus, notes)
                            
                            // Update payment status if callback is provided and payment section is visible
                            if (onUpdatePaymentStatus != null && totalAmount > 0) {
                                val paidAmountValue = paidAmount.toDoubleOrNull() ?: currentPaidAmount
                                
                                // Ensure payment status is consistent with paid amount
                                val finalPaymentStatus = when {
                                    paidAmountValue <= 0 -> PaymentStatus.PENDING
                                    paidAmountValue >= totalAmount -> PaymentStatus.PAID
                                    paidAmountValue > 0 && paidAmountValue < totalAmount -> PaymentStatus.PARTIAL
                                    else -> selectedPaymentStatus
                                }
                                
                                onUpdatePaymentStatus(finalPaymentStatus, paidAmountValue)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                    ) {
                        Text("আপডেট করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignDeliveryPersonDialog(
    delivery: DeliveryEntry,
    availablePersons: List<DeliveryPerson>,
    onDismiss: () -> Unit,
    onAssign: (String, String) -> Unit
) {
    var selectedPerson by remember { mutableStateOf<DeliveryPerson?>(null) }
    var showPersonDropdown by remember { mutableStateOf(false) }
    
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
            ) {
                Text(
                    text = "ডেলিভারি পার্সন নিযুক্ত করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Order Info
                Card(
                    colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "অর্ডার: ${delivery.orderNumber}",
                            fontWeight = FontWeight.Medium,
                            color = DarkTeal
                        )
                        Text(
                            text = "কাস্টমার: ${delivery.customerName}",
                            fontSize = 14.sp,
                            color = DarkGray
                        )
                        Text(
                            text = "ঠিকানা: ${delivery.deliveryAddress}",
                            fontSize = 14.sp,
                            color = DarkGray
                        )
                        if (delivery.deliveryPerson.isNotEmpty()) {
                            Text(
                                text = "বর্তমান ডেলিভারি পার্সন: ${delivery.deliveryPerson}",
                                fontSize = 14.sp,
                                color = Color(0xFFFF9800)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Person Dropdown
                ExposedDropdownMenuBox(
                    expanded = showPersonDropdown,
                    onExpandedChange = { showPersonDropdown = !showPersonDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedPerson?.name ?: "ডেলিভারি পার্সন নির্বাচন করুন",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("ডেলিভারি পার্সন *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPersonDropdown)
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
                        expanded = showPersonDropdown,
                        onDismissRequest = { showPersonDropdown = false }
                    ) {
                        if (availablePersons.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("কোন ডেলিভারি পার্সন উপলব্ধ নেই") },
                                onClick = { }
                            )
                        } else {
                            availablePersons.forEach { person ->
                                DropdownMenuItem(
                                    text = { 
                                        Column {
                                            Text(
                                                text = person.name,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${person.phone} • ${person.vehicleType}",
                                                fontSize = 12.sp,
                                                color = DarkGray
                                            )
                                            Text(
                                                text = "রেটিং: ${person.rating} • ডেলিভারি: ${person.totalDeliveries}",
                                                fontSize = 10.sp,
                                                color = LightGray
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedPerson = person
                                        showPersonDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Selected Person Details
                selectedPerson?.let { person ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkTeal.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "নির্বাচিত ডেলিভারি পার্সন",
                                fontWeight = FontWeight.Medium,
                                color = DarkTeal,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "নাম: ${person.name}",
                                fontSize = 14.sp,
                                color = DarkGray
                            )
                            Text(
                                text = "ফোন: ${person.phone}",
                                fontSize = 14.sp,
                                color = DarkGray
                            )
                            Text(
                                text = "যানবাহন: ${person.vehicleType} (${person.vehicleNumber})",
                                fontSize = 14.sp,
                                color = DarkGray
                            )
                            Text(
                                text = "বর্তমান অবস্থান: ${person.currentLocation}",
                                fontSize = 14.sp,
                                color = DarkGray
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkGray
                        )
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            selectedPerson?.let { person ->
                                onAssign(person.name, person.phone)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = selectedPerson != null,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                    ) {
                        Text("নিযুক্ত করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryFilterDialog(
    onDismiss: () -> Unit,
    onApplyFilter: (DeliveryFilter) -> Unit,
    onClearFilter: () -> Unit
) {
    var selectedStatus by remember { mutableStateOf<DeliveryStatusType?>(null) }
    var deliveryPerson by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf<DeliveryPriority?>(null) }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showPriorityDropdown by remember { mutableStateOf(false) }
    
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
                    text = "ডেলিভারি ফিল্টার",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Status Filter
                ExposedDropdownMenuBox(
                    expanded = showStatusDropdown,
                    onExpandedChange = { showStatusDropdown = !showStatusDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedStatus?.displayNameBn ?: "সব স্ট্যাটাস",
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("স্ট্যাটাস") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showStatusDropdown)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    
                    ExposedDropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("সব স্ট্যাটাস") },
                            onClick = {
                                selectedStatus = null
                                showStatusDropdown = false
                            }
                        )
                        DeliveryStatusType.values().forEach { status ->
                            DropdownMenuItem(
                                text = { Text(status.displayNameBn) },
                                onClick = {
                                    selectedStatus = status
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Delivery Person Filter
                OutlinedTextField(
                    value = deliveryPerson,
                    onValueChange = { deliveryPerson = it },
                    label = { Text("ডেলিভারি পার্সন") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Area Filter
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("এলাকা") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Priority Filter
                ExposedDropdownMenuBox(
                    expanded = showPriorityDropdown,
                    onExpandedChange = { showPriorityDropdown = !showPriorityDropdown }
                ) {
                    OutlinedTextField(
                        value = selectedPriority?.displayNameBn ?: "সব অগ্রাধিকার",
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
                        DropdownMenuItem(
                            text = { Text("সব অগ্রাধিকার") },
                            onClick = {
                                selectedPriority = null
                                showPriorityDropdown = false
                            }
                        )
                        DeliveryPriority.values().forEach { priority ->
                            DropdownMenuItem(
                                text = { Text(priority.displayNameBn) },
                                onClick = {
                                    selectedPriority = priority
                                    showPriorityDropdown = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Date Range
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dateFrom,
                        onValueChange = { dateFrom = it },
                        label = { Text("থেকে তারিখ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("dd/mm/yyyy") }
                    )
                    
                    OutlinedTextField(
                        value = dateTo,
                        onValueChange = { dateTo = it },
                        label = { Text("পর্যন্ত তারিখ") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("dd/mm/yyyy") }
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("বাতিল")
                    }
                    
                    OutlinedButton(
                        onClick = {
                            onClearFilter()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF9800)
                        )
                    ) {
                        Text("রিসেট")
                    }
                    
                    Button(
                        onClick = {
                            val filter = DeliveryFilter(
                                status = selectedStatus,
                                deliveryPerson = deliveryPerson,
                                area = area,
                                priority = selectedPriority,
                                dateFrom = dateFrom,
                                dateTo = dateTo
                            )
                            onApplyFilter(filter)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkTeal)
                    ) {
                        Text("প্রয়োগ", color = Color.White)
                    }
                }
            }
        }
    }
}
