package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.firebase.loginauth.ui.theme.*

@Composable
fun AddEditCustomerDialog(
    customer: Customer? = null,
    onDismiss: () -> Unit,
    onSave: (Customer) -> Unit
) {
    var name by remember { mutableStateOf(customer?.name ?: "") }
    var phone by remember { mutableStateOf(customer?.phone ?: "") }
    var address by remember { mutableStateOf(customer?.address ?: "") }
    var area by remember { mutableStateOf(customer?.area ?: "") }
    var email by remember { mutableStateOf(customer?.email ?: "") }
    var notes by remember { mutableStateOf(customer?.notes ?: "") }
    var creditLimit by remember { mutableStateOf(customer?.creditLimit?.toString() ?: "0") }
    var selectedType by remember { mutableStateOf(customer?.customerType ?: CustomerType.REGULAR) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    
    val isEditing = customer != null
    
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
                        text = if (isEditing) "গ্রাহক সম্পাদনা" else "নতুন গ্রাহক",
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
                
                // Customer Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("গ্রাহকের নাম *") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = "Name",
                            tint = DarkTeal
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal,
                        focusedLeadingIconColor = DarkTeal
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Phone Number
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("ফোন নম্বর *") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = "Phone",
                            tint = DarkTeal
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal,
                        focusedLeadingIconColor = DarkTeal
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Customer Type Selection
                Box {
                    OutlinedTextField(
                        value = selectedType.displayNameBn,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("গ্রাহকের ধরন") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = "Type",
                                tint = DarkTeal
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { showTypeDropdown = !showTypeDropdown }) {
                                Icon(
                                    imageVector = if (showTypeDropdown) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = "Dropdown",
                                    tint = DarkTeal
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkTeal,
                            focusedLabelColor = DarkTeal,
                            focusedLeadingIconColor = DarkTeal
                        )
                    )
                    
                    // Invisible clickable overlay to handle clicks on the entire field
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showTypeDropdown = !showTypeDropdown }
                    )
                    
                    if (showTypeDropdown) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column {
                                CustomerType.values().forEach { type ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedType = type
                                                showTypeDropdown = false
                                            }
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = type.displayNameBn,
                                            color = DarkTeal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Area
                OutlinedTextField(
                    value = area,
                    onValueChange = { area = it },
                    label = { Text("এলাকা") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = "Area",
                            tint = DarkTeal
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal,
                        focusedLeadingIconColor = DarkTeal
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Address
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("ঠিকানা") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Home,
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
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Email (Optional)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("ইমেইল (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Email,
                            contentDescription = "Email",
                            tint = DarkTeal
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal,
                        focusedLeadingIconColor = DarkTeal
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Credit Limit
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { creditLimit = it },
                    label = { Text("ক্রেডিট সীমা (টাকা)") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Credit Limit",
                            tint = DarkTeal
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkTeal,
                        focusedLabelColor = DarkTeal,
                        focusedLeadingIconColor = DarkTeal
                    ),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Notes
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
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkTeal)
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            val creditLimitValue = creditLimit.toDoubleOrNull() ?: 0.0
                            val newCustomer = if (isEditing) {
                                customer!!.copy(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    area = area.trim(),
                                    email = email.trim(),
                                    notes = notes.trim(),
                                    creditLimit = creditLimitValue,
                                    customerType = selectedType
                                )
                            } else {
                                Customer(
                                    name = name.trim(),
                                    phone = phone.trim(),
                                    address = address.trim(),
                                    area = area.trim(),
                                    email = email.trim(),
                                    notes = notes.trim(),
                                    creditLimit = creditLimitValue,
                                    customerType = selectedType
                                )
                            }
                            onSave(newCustomer)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkTeal,
                            contentColor = Color.White
                        ),
                        enabled = name.isNotBlank() && phone.isNotBlank()
                    ) {
                        Text(if (isEditing) "আপডেট" else "সংরক্ষণ")
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerDetailsDialog(
    customer: Customer,
    onDismiss: () -> Unit,
    onEdit: () -> Unit
) {
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
                        text = "গ্রাহকের বিস্তারিত",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    
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
                
                // Customer Type Badge
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when (customer.customerType) {
                            CustomerType.VIP -> Color(0xFFFFD700).copy(alpha = 0.2f)
                            CustomerType.COMMERCIAL -> Color.Blue.copy(alpha = 0.2f)

                            else -> LightTeal.copy(alpha = 0.2f)
                        }
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = customer.customerType.displayNameBn,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTeal
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Customer Information
                CustomerDetailRow(
                    icon = Icons.Filled.Person,
                    label = "নাম",
                    value = customer.name
                )
                
                CustomerDetailRow(
                    icon = Icons.Filled.Phone,
                    label = "ফোন",
                    value = customer.phone
                )
                
                if (customer.area.isNotBlank()) {
                    CustomerDetailRow(
                        icon = Icons.Filled.LocationOn,
                        label = "এলাকা",
                        value = customer.area
                    )
                }
                
                if (customer.address.isNotBlank()) {
                    CustomerDetailRow(
                        icon = Icons.Filled.Home,
                        label = "ঠিকানা",
                        value = customer.address
                    )
                }
                
                if (customer.email.isNotBlank()) {
                    CustomerDetailRow(
                        icon = Icons.Filled.Email,
                        label = "ইমেইল",
                        value = customer.email
                    )
                }
                
                // Financial Information
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "আর্থিক তথ্য",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                CustomerDetailRow(
                    icon = Icons.Filled.Star,
                    label = "ক্রেডিট সীমা",
                    value = "৳${customer.creditLimit.toInt()}"
                )
                
                CustomerDetailRow(
                    icon = Icons.Filled.Info,
                    label = "বর্তমান বাকি",
                    value = "৳${customer.currentCredit.toInt()}",
                    valueColor = if (customer.currentCredit > 0) Color.Red else Color.Green
                )
                
                CustomerDetailRow(
                    icon = Icons.Filled.ShoppingCart,
                    label = "মোট অর্ডার",
                    value = customer.totalOrders.toString()
                )
                
                if (customer.lastOrderDate.isNotBlank()) {
                    CustomerDetailRow(
                        icon = Icons.Filled.DateRange,
                        label = "শেষ অর্ডার",
                        value = customer.lastOrderDate
                    )
                }
                
                // Account Information
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "অ্যাকাউন্ট তথ্য",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                CustomerDetailRow(
                    icon = Icons.Filled.DateRange,
                    label = "যোগদানের তারিখ",
                    value = customer.createdDate
                )
                
                CustomerDetailRow(
                    icon = Icons.Filled.CheckCircle,
                    label = "অবস্থা",
                    value = if (customer.isActive) "সক্রিয়" else "নিষ্ক্রিয়",
                    valueColor = if (customer.isActive) Color.Green else Color.Red
                )
                
                // Notes
                if (customer.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "নোট",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LightGray.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = customer.notes,
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = DarkTeal,
                            textAlign = TextAlign.Start
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Action Button
                Button(
                    onClick = onEdit,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkTeal,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Edit",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সম্পাদনা করুন")
                }
            }
        }
    }
}

@Composable
private fun CustomerDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = DarkTeal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = DarkTeal.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = DarkTeal.copy(alpha = 0.6f),
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = value,
                fontSize = 16.sp,
                color = valueColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
