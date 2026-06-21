package com.firebase.loginauth

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a business expense entry
 */
data class ExpenseEntry(
    val id: String = "",
    val expenseType: String = "",
    val description: String = "",
    val amount: String = "",
    val paymentMethod: String = "",
    val vendor: String = "",
    val location: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val date: String = getCurrentDateString(),
    val time: String = getCurrentTimeString()
) {
    companion object {
        private fun getCurrentDateString(): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date())
        }
        
        private fun getCurrentTimeString(): String {
            val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}

/**
 * Enum for expense types in gas cylinder business
 */
enum class ExpenseType(val displayName: String, val displayNameBn: String) {
    FUEL("Fuel", "জ্বালানি"),
    TRANSPORT("Transport", "পরিবহন"),
    MAINTENANCE("Maintenance", "রক্ষণাবেক্ষণ"),
    SALARY("Salary", "বেতন"),
    RENT("Rent", "ভাড়া"),
    UTILITIES("Utilities", "ইউটিলিটি"),
    MARKETING("Marketing", "বিপণন"),
    OFFICE_SUPPLIES("Office Supplies", "অফিস সরবরাহ"),
    EQUIPMENT("Equipment", "যন্ত্রপাতি"),
    INSURANCE("Insurance", "বীমা"),
    TAX("Tax", "কর"),
    OTHER("Other", "অন্যান্য")
}

/**
 * Enum for payment methods
 */
enum class PaymentMethod(val displayName: String, val displayNameBn: String) {
    CASH("Cash", "নগদ"),
    BANK_TRANSFER("Bank Transfer", "ব্যাংক ট্রান্সফার"),
    MOBILE_BANKING("Mobile Banking", "মোবাইল ব্যাংকিং"),
    CREDIT_CARD("Credit Card", "ক্রেডিট কার্ড"),
    CHEQUE("Cheque", "চেক"),
    OTHER("Other", "অন্যান্য")
}

// Using ExpenseSummary from AutomatedReport.kt to avoid duplication
