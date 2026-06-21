package com.firebase.loginauth

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
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddStockDialog(
    onDismiss: () -> Unit,
    onConfirm: (CylinderStock) -> Unit
) {
    var cylinderType by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var totalQuantity by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var lowStockThreshold by remember { mutableStateOf("10") }
    var location by remember { mutableStateOf("") }
    var supplierName by remember { mutableStateOf("") }
    var supplierContact by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    var expandedCylinderType by remember { mutableStateOf(false) }
    var expandedBrand by remember { mutableStateOf(false) }
    
    // Validation states for mandatory fields
    var showCylinderTypeError by remember { mutableStateOf(false) }
    var showBrandError by remember { mutableStateOf(false) }
    var showQuantityError by remember { mutableStateOf(false) }

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
                    text = "নতুন স্টক যোগ করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Mandatory fields notice
                Text(
                    text = "* প্রথম ৩টি ক্ষেত্র বাধ্যতামূলক",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Cylinder Type Dropdown (Mandatory)
                ExposedDropdownMenuBox(
                    expanded = expandedCylinderType,
                    onExpandedChange = { 
                        expandedCylinderType = !expandedCylinderType
                        showCylinderTypeError = false
                    }
                ) {
                    OutlinedTextField(
                        value = cylinderType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("সিলিন্ডারের ধরন *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCylinderType) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (showCylinderTypeError) Color.Red else LightTeal,
                            unfocusedBorderColor = if (showCylinderTypeError) Color.Red else Color.Gray
                        ),
                        isError = showCylinderTypeError
                    )
                    ExposedDropdownMenu(
                        expanded = expandedCylinderType,
                        onDismissRequest = { expandedCylinderType = false }
                    ) {
                        CylinderType.values().filter { !it.name.startsWith("KG_") }.forEach { type ->
                            DropdownMenuItem(
                                text = { Text("${type.displayName} (${type.displayNameBn})") },
                                onClick = {
                                    cylinderType = type.displayName
                                    expandedCylinderType = false
                                    showCylinderTypeError = false
                                }
                            )
                        }
                    }
                }
                
                // Error message for Cylinder Type
                if (showCylinderTypeError) {
                    Text(
                        text = "সিলিন্ডারের ধরন নির্বাচন করুন",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Brand Dropdown (Mandatory)
                ExposedDropdownMenuBox(
                    expanded = expandedBrand,
                    onExpandedChange = { 
                        expandedBrand = !expandedBrand
                        showBrandError = false
                    }
                ) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("ব্র্যান্ড *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedBrand) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (showBrandError) Color.Red else LightTeal,
                            unfocusedBorderColor = if (showBrandError) Color.Red else Color.Gray
                        ),
                        isError = showBrandError
                    )
                    ExposedDropdownMenu(
                        expanded = expandedBrand,
                        onDismissRequest = { expandedBrand = false }
                    ) {
                        GasBrand.values().forEach { brandEnum ->
                            DropdownMenuItem(
                                text = { Text("${brandEnum.displayName} (${brandEnum.displayNameBn})") },
                                onClick = {
                                    brand = brandEnum.displayName
                                    expandedBrand = false
                                    showBrandError = false
                                }
                            )
                        }
                    }
                }
                
                // Error message for Brand
                if (showBrandError) {
                    Text(
                        text = "ব্র্যান্ড নির্বাচন করুন",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Total Quantity (Mandatory)
                OutlinedTextField(
                    value = totalQuantity,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                            totalQuantity = newValue
                            showQuantityError = false
                        }
                    },
                    label = { Text("মোট পরিমাণ *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (showQuantityError) Color.Red else LightTeal,
                        unfocusedBorderColor = if (showQuantityError) Color.Red else Color.Gray
                    ),
                    isError = showQuantityError
                )
                
                // Error message for Total Quantity
                if (showQuantityError) {
                    Text(
                        text = "মোট পরিমাণ লিখুন (সংখ্যায়)",
                        color = Color.Red,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Price Per Unit
                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { pricePerUnit = it },
                    label = { Text("প্রতি ইউনিট মূল্য (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Low Stock Threshold
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it },
                    label = { Text("কম স্টক সীমা") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("স্থান") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Supplier Name
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("সরবরাহকারীর নাম") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Supplier Contact
                OutlinedTextField(
                    value = supplierContact,
                    onValueChange = { supplierContact = it },
                    label = { Text("সরবরাহকারীর যোগাযোগ") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // Validate mandatory fields
                            val isCylinderTypeValid = cylinderType.isNotBlank()
                            val isBrandValid = brand.isNotBlank()
                            val isQuantityValid = totalQuantity.isNotBlank() && totalQuantity.toIntOrNull() != null && totalQuantity.toInt() > 0
                            
                            showCylinderTypeError = !isCylinderTypeValid
                            showBrandError = !isBrandValid
                            showQuantityError = !isQuantityValid
                            
                            if (isCylinderTypeValid && isBrandValid && isQuantityValid) {
                                val stock = CylinderStock(
                                    cylinderType = cylinderType,
                                    brand = brand,
                                    totalQuantity = totalQuantity.toInt(),
                                    availableQuantity = totalQuantity.toInt(),
                                    pricePerUnit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                                    lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 10,
                                    location = location.ifBlank { "অনির্দিষ্ট" },
                                    supplierName = supplierName.ifBlank { "অজানা" },
                                    supplierContact = supplierContact,
                                    notes = notes
                                )
                                onConfirm(stock)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightTeal)
                    ) {
                        Text("যোগ করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditStockDialog(
    stock: CylinderStock,
    onDismiss: () -> Unit,
    onConfirm: (CylinderStock) -> Unit
) {
    var cylinderType by remember { mutableStateOf(stock.cylinderType) }
    var brand by remember { mutableStateOf(stock.brand) }
    var pricePerUnit by remember { mutableStateOf(stock.pricePerUnit.toString()) }
    var lowStockThreshold by remember { mutableStateOf(stock.lowStockThreshold.toString()) }
    var location by remember { mutableStateOf(stock.location) }
    var supplierName by remember { mutableStateOf(stock.supplierName) }
    var supplierContact by remember { mutableStateOf(stock.supplierContact) }
    var notes by remember { mutableStateOf(stock.notes) }

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
                    text = "স্টক সম্পাদনা",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Price Per Unit
                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { pricePerUnit = it },
                    label = { Text("প্রতি ইউনিট মূল্য (৳)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Low Stock Threshold
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it },
                    label = { Text("কম স্টক সীমা") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("স্থান") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Supplier Name
                OutlinedTextField(
                    value = supplierName,
                    onValueChange = { supplierName = it },
                    label = { Text("সরবরাহকারীর নাম") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Supplier Contact
                OutlinedTextField(
                    value = supplierContact,
                    onValueChange = { supplierContact = it },
                    label = { Text("সরবরাহকারীর যোগাযোগ") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val updatedStock = stock.copy(
                                pricePerUnit = pricePerUnit.toDoubleOrNull() ?: stock.pricePerUnit,
                                lowStockThreshold = lowStockThreshold.toIntOrNull() ?: stock.lowStockThreshold,
                                location = location,
                                supplierName = supplierName,
                                supplierContact = supplierContact,
                                notes = notes,
                                lastUpdated = Timestamp.now()
                            )
                            onConfirm(updatedStock)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = LightTeal)
                    ) {
                        Text("আপডেট করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun StockDetailsDialog(
    stock: CylinderStock,
    onDismiss: () -> Unit
) {
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
                    text = "স্টক বিস্তারিত",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StockDetailRow("ধরন", "${stock.brand} - ${stock.cylinderType}")
                StockDetailRow("মোট পরিমাণ", stock.totalQuantity.toString())
                StockDetailRow("উপলব্ধ", stock.availableQuantity.toString())
                StockDetailRow("বিক্রিত", stock.soldQuantity.toString())
                StockDetailRow("প্রতি ইউনিট মূল্য", "৳${stock.pricePerUnit}")
                StockDetailRow("কম স্টক সীমা", stock.lowStockThreshold.toString())
                StockDetailRow("স্থান", stock.location)
                StockDetailRow("সরবরাহকারী", stock.supplierName)
                StockDetailRow("যোগাযোগ", stock.supplierContact)
                
                if (stock.notes.isNotBlank()) {
                    StockDetailRow("নোট", stock.notes)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LightTeal)
                ) {
                    Text("বন্ধ করুন", color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AdjustStockDialog(
    stock: CylinderStock,
    onDismiss: () -> Unit,
    onConfirm: (Int, String, String) -> Unit
) {
    var quantityChange by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isAddition by remember { mutableStateOf(true) }

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
                    text = "স্টক সমন্বয়",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "${stock.brand} - ${stock.cylinderType}",
                    fontSize = 16.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "বর্তমান স্টক: ${stock.availableQuantity}",
                    fontSize = 14.sp,
                    color = DarkTeal,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Add/Remove Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        onClick = { isAddition = true },
                        label = { Text("যোগ করুন") },
                        selected = isAddition,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LightTeal,
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        onClick = { isAddition = false },
                        label = { Text("বিয়োগ করুন") },
                        selected = !isAddition,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF5722),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quantity
                OutlinedTextField(
                    value = quantityChange,
                    onValueChange = { quantityChange = it },
                    label = { Text("পরিমাণ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Reason
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("কারণ") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LightTeal,
                        unfocusedBorderColor = Color.Gray
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val change = quantityChange.toIntOrNull() ?: 0
                            val finalChange = if (isAddition) change else -change
                            onConfirm(finalChange, reason, notes)
                        },
                        enabled = quantityChange.isNotBlank() && reason.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = LightTeal)
                    ) {
                        Text("সমন্বয় করুন", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun StockDetailRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = DarkTeal,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Divider(color = Color(0xFFE0E0E0), thickness = 0.5.dp)
    }
}
