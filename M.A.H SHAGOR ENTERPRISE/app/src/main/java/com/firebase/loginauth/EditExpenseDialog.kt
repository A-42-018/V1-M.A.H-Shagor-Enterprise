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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExpenseDialog(
    expense: ExpenseEntry,
    onDismiss: () -> Unit,
    onSave: (ExpenseEntry) -> Unit
) {
    // Form state variables initialized with existing expense data
    var selectedExpenseType by remember { mutableStateOf(expense.expenseType) }
    var description by remember { mutableStateOf(expense.description) }
    var amount by remember { mutableStateOf(expense.amount) }
    var selectedPaymentMethod by remember { mutableStateOf(expense.paymentMethod) }
    var vendor by remember { mutableStateOf(expense.vendor) }
    var location by remember { mutableStateOf(expense.location) }
    var notes by remember { mutableStateOf(expense.notes) }
    
    // Dropdown states
    var expenseTypeExpanded by remember { mutableStateOf(false) }
    var paymentMethodExpanded by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "খরচের তথ্য সম্পাদনা",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF006064),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Expense Type Dropdown (Mandatory)
                ExposedDropdownMenuBox(
                    expanded = expenseTypeExpanded,
                    onExpandedChange = { expenseTypeExpanded = !expenseTypeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedExpenseType,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("খরচের ধরন *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseTypeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00ACC1),
                            focusedLabelColor = Color(0xFF00ACC1)
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = expenseTypeExpanded,
                        onDismissRequest = { expenseTypeExpanded = false }
                    ) {
                        ExpenseType.values().forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayNameBn) },
                                onClick = {
                                    selectedExpenseType = type.displayNameBn
                                    expenseTypeExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Description (Mandatory)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("বিবরণ *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                // Amount (Mandatory)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("পরিমাণ (টাকা) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                // Payment Method Dropdown (Mandatory)
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedPaymentMethod,
                        onValueChange = { },
                        readOnly = true,
                        label = { Text("পেমেন্ট পদ্ধতি *") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00ACC1),
                            focusedLabelColor = Color(0xFF00ACC1)
                        )
                    )
                    
                    ExposedDropdownMenu(
                        expanded = paymentMethodExpanded,
                        onDismissRequest = { paymentMethodExpanded = false }
                    ) {
                        PaymentMethod.values().forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayNameBn) },
                                onClick = {
                                    selectedPaymentMethod = method.displayNameBn
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }
                
                // Vendor (Optional)
                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("বিক্রেতা/সরবরাহকারী") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                // Location (Optional)
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("স্থান") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                // Notes (Optional)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("নোট") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00ACC1),
                        focusedLabelColor = Color(0xFF00ACC1)
                    )
                )
                
                // Mandatory fields notice
                Text(
                    text = "* প্রথম ৪টি ক্ষেত্র বাধ্যতামূলক",
                    fontSize = 12.sp,
                    color = Color.Red,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF006064)
                        )
                    ) {
                        Text("বাতিল")
                    }
                    
                    Button(
                        onClick = {
                            if (selectedExpenseType.isNotBlank() && 
                                description.isNotBlank() && 
                                amount.isNotBlank() && 
                                selectedPaymentMethod.isNotBlank()) {
                                
                                val updatedExpense = expense.copy(
                                    expenseType = selectedExpenseType,
                                    description = description,
                                    amount = amount,
                                    paymentMethod = selectedPaymentMethod,
                                    vendor = vendor.ifBlank { "অজানা" },
                                    location = location.ifBlank { "অনির্দিষ্ট" },
                                    notes = notes,
                                    updatedAt = System.currentTimeMillis()
                                )
                                onSave(updatedExpense)
                            }
                        },
                        enabled = selectedExpenseType.isNotBlank() && 
                                 description.isNotBlank() && 
                                 amount.isNotBlank() && 
                                 selectedPaymentMethod.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00ACC1),
                            disabledContainerColor = Color.Gray
                        )
                    ) {
                        Text(
                            text = "সংরক্ষণ",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
