package com.firebase.loginauth

import java.util.*

// Data class for Due Account Entry
data class DueAccountEntry(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val transactionType: DueTransactionType,
    val amount: Double,
    val description: String,
    val date: Long = System.currentTimeMillis(),
    val orderId: String? = null, // Optional link to order
    val paymentMethod: String = "ক্যাশ",
    val notes: String = ""
)

// Enum for transaction types
enum class DueTransactionType(val displayNameBengali: String) {
    CREDIT("বাকী দেওয়া"), // Customer owes money (due from customer)
    PAYMENT("পেমেন্ট"), // Customer paid money (payment received)
    ADJUSTMENT("সমন্বয়") // Manual adjustment
}

// Data class for Due Summary
data class DueSummary(
    val totalDueAmount: Double = 0.0,
    val totalCustomersWithDue: Int = 0,
    val totalPaymentsToday: Double = 0.0,
    val totalDueGivenToday: Double = 0.0,
    val highestDueCustomer: String = "",
    val highestDueAmount: Double = 0.0
)

// Data class for Customer Due Balance
data class CustomerDueBalance(
    val customerId: String,
    val customerName: String,
    val customerPhone: String,
    val totalDueAmount: Double,
    val lastTransactionDate: Long,
    val transactionCount: Int
)

// Filter options for due account
enum class DueAccountFilter(val displayName: String) {
    ALL("সব"),
    TODAY("আজ"),
    WEEK("সপ্তাহ"),
    MONTH("মাস"),
    YEAR("বছর")
}
