package com.firebase.loginauth

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.firebase.loginauth.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDueEntryDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, String, DueTransactionType, Double, String, String, String) -> Unit,
    customers: List<Customer> = emptyList(),
    entry: DueAccountEntry? = null
) {
    var selectedCustomer by remember { mutableStateOf<Customer?>(null) }
    var customerName by remember { mutableStateOf(entry?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(entry?.customerPhone ?: "") }
    var transactionType by remember { mutableStateOf(entry?.transactionType ?: DueTransactionType.CREDIT) }
    var amount by remember { mutableStateOf(entry?.amount?.toString() ?: "") }
    var description by remember { mutableStateOf(entry?.description ?: "") }
    var paymentMethod by remember { mutableStateOf(entry?.paymentMethod ?: "ক্যাশ") }
    var notes by remember { mutableStateOf(entry?.notes ?: "") }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showTransactionTypeDropdown by remember { mutableStateOf(false) }
    var showPaymentMethodDropdown by remember { mutableStateOf(false) }

    // Validation states
    var customerNameError by remember { mutableStateOf<String?>(null) }
    var customerPhoneError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }
    var descriptionError by remember { mutableStateOf<String?>(null) }

    val paymentMethods = listOf("ক্যাশ", "বিকাশ", "নগদ", "রকেট", "ব্যাংক", "অন্যান্য")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (entry != null) "বাকীর এন্ট্রি সম্পাদনা" else "নতুন বাকীর এন্ট্রি",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = DarkGray
                        )
                    }
                }

                // Customer Selection
                Column {
                    Text(
                        text = "কাস্টমার নির্বাচন *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showCustomerDropdown,
                        onExpandedChange = { showCustomerDropdown = !showCustomerDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedCustomer?.name ?: "নতুন কাস্টমার",
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCustomerDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showCustomerDropdown,
                            onDismissRequest = { showCustomerDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("নতুন কাস্টমার") },
                                onClick = {
                                    selectedCustomer = null
                                    showCustomerDropdown = false
                                }
                            )
                            customers.forEach { customer ->
                                DropdownMenuItem(
                                    text = { Text("${customer.name} - ${customer.phone}") },
                                    onClick = {
                                        selectedCustomer = customer
                                        customerName = customer.name
                                        customerPhone = customer.phone
                                        showCustomerDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Customer Name (if new customer)
                if (selectedCustomer == null) {
                    Column {
                        OutlinedTextField(
                            value = customerName,
                            onValueChange = { 
                                customerName = it
                                customerNameError = null
                            },
                            label = { Text("কাস্টমারের নাম *") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = customerNameError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                            )
                        )
                        customerNameError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }

                    // Customer Phone
                    Column {
                        OutlinedTextField(
                            value = customerPhone,
                            onValueChange = { 
                                customerPhone = it
                                customerPhoneError = null
                            },
                            label = { Text("ফোন নম্বর *") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            isError = customerPhoneError != null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                            )
                        )
                        customerPhoneError?.let { error ->
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                            )
                        }
                    }
                }

                // Transaction Type
                Column {
                    Text(
                        text = "লেনদেনের ধরন *",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showTransactionTypeDropdown,
                        onExpandedChange = { showTransactionTypeDropdown = !showTransactionTypeDropdown }
                    ) {
                        OutlinedTextField(
                            value = transactionType.displayNameBengali,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTransactionTypeDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showTransactionTypeDropdown,
                            onDismissRequest = { showTransactionTypeDropdown = false }
                        ) {
                            DueTransactionType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayNameBengali) },
                                    onClick = {
                                        transactionType = type
                                        showTransactionTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Amount
                Column {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { 
                            amount = it
                            amountError = null
                        },
                        label = { Text("পরিমাণ (টাকা) *") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightTeal,
                            unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                        )
                    )
                    amountError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                // Description
                Column {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { 
                            description = it
                            descriptionError = null
                        },
                        label = { Text("বিবরণ *") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        isError = descriptionError != null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LightTeal,
                            unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                        )
                    )
                    descriptionError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                        )
                    }
                }

                // Payment Method
                Column {
                    Text(
                        text = "পেমেন্ট পদ্ধতি",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = showPaymentMethodDropdown,
                        onExpandedChange = { showPaymentMethodDropdown = !showPaymentMethodDropdown }
                    ) {
                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = { },
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showPaymentMethodDropdown) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = LightTeal,
                                unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                            )
                        )
                        
                        ExposedDropdownMenu(
                            expanded = showPaymentMethodDropdown,
                            onDismissRequest = { showPaymentMethodDropdown = false }
                        ) {
                            paymentMethods.forEach { method ->
                                DropdownMenuItem(
                                    text = { Text(method) },
                                    onClick = {
                                        paymentMethod = method
                                        showPaymentMethodDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("অতিরিক্ত নোট") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = DarkGray.copy(alpha = 0.3f)
                    )
                )

                // Buttons
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
                            var hasError = false
                            
                            if (selectedCustomer == null && customerName.isBlank()) {
                                customerNameError = "কাস্টমারের নাম লিখুন"
                                hasError = true
                            }
                            
                            if (selectedCustomer == null && customerPhone.isBlank()) {
                                customerPhoneError = "ফোন নম্বর লিখুন"
                                hasError = true
                            }
                            
                            if (amount.isBlank() || amount.toDoubleOrNull() == null || amount.toDouble() <= 0) {
                                amountError = "সঠিক পরিমাণ লিখুন"
                                hasError = true
                            }
                            
                            if (description.isBlank()) {
                                descriptionError = "বিবরণ লিখুন"
                                hasError = true
                            }
                            
                            if (!hasError) {
                                val customerId = selectedCustomer?.id ?: java.util.UUID.randomUUID().toString()
                                val finalCustomerName = selectedCustomer?.name ?: customerName
                                val finalCustomerPhone = selectedCustomer?.phone ?: customerPhone
                                
                                onSave(
                                    customerId,
                                    finalCustomerName,
                                    finalCustomerPhone,
                                    transactionType,
                                    amount.toDouble(),
                                    description,
                                    paymentMethod,
                                    notes
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LightTeal,
                            contentColor = Color.White
                        )
                    ) {
                        Text("সংরক্ষণ")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDueDetailsDialog(
    customerBalance: CustomerDueBalance,
    entries: List<DueAccountEntry>,
    onDismiss: () -> Unit,
    onAddPayment: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "কাস্টমারের বাকীর বিবরণ",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = DarkGray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Customer Info
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
                            text = customerBalance.customerName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkTeal
                        )
                        Text(
                            text = customerBalance.customerPhone,
                            fontSize = 14.sp,
                            color = DarkGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "মোট বাকী: ৳${formatCurrencyBengali(customerBalance.totalDueAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (customerBalance.totalDueAmount > 0) Color(0xFFE53E3E) else Color(0xFF38A169)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Add Payment Button
                if (customerBalance.totalDueAmount > 0) {
                    Button(
                        onClick = { onAddPayment(customerBalance.customerId) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF38A169),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("পেমেন্ট যোগ করুন")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Transaction History
                Text(
                    text = "লেনদেনের ইতিহাস",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Entries List
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries.sortedByDescending { it.date }) { entry ->
                        DueEntryCard(entry = entry)
                    }
                }
            }
        }
    }
}

@Composable
private fun DueEntryCard(entry: DueAccountEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.transactionType.displayNameBengali,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = when (entry.transactionType) {
                        DueTransactionType.CREDIT -> Color(0xFFE53E3E)
                        DueTransactionType.PAYMENT -> Color(0xFF38A169)
                        DueTransactionType.ADJUSTMENT -> Color(0xFF3182CE)
                    }
                )
                Text(
                    text = "৳${formatCurrencyBengali(entry.amount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (entry.transactionType) {
                        DueTransactionType.CREDIT -> Color(0xFFE53E3E)
                        DueTransactionType.PAYMENT -> Color(0xFF38A169)
                        DueTransactionType.ADJUSTMENT -> Color(0xFF3182CE)
                    }
                )
            }
            
            Text(
                text = entry.description,
                fontSize = 12.sp,
                color = DarkGray,
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Text(
                text = formatDateForEntry(entry.date),
                fontSize = 11.sp,
                color = DarkGray.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

// Utility function to format date for entry
private fun formatDateForEntry(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
