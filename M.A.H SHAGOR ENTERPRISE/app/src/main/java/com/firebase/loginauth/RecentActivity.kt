package com.firebase.loginauth

import java.text.SimpleDateFormat
import java.util.*

// Enum for different activity types
enum class ActivityType {
    CASH_DEPOSIT,
    EXPENSE,
    ORDER_CREATE,
    ORDER_UPDATE,
    CUSTOMER_CREATE,
    CUSTOMER_UPDATE,
    STOCK_ADD,
    STOCK_UPDATE,
    DUE_ACCOUNT,
    NOTE_CREATE,
    NOTE_UPDATE
}

// Data class for recent activity entries
data class RecentActivityEntry(
    val id: String = "",
    val type: ActivityType = ActivityType.ORDER_CREATE,
    val title: String = "",
    val description: String = "",
    val amount: Double? = null,
    val customerName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isCredit: Boolean = false // true for income/credit, false for expense/debit
) {
    // Format timestamp to Bengali readable format
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    // Get Bengali time format
    fun getBengaliTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60000 -> "এখনই" // Just now
            diff < 3600000 -> "${diff / 60000} মিনিট আগে" // X minutes ago
            diff < 86400000 -> "${diff / 3600000} ঘন্টা আগে" // X hours ago
            diff < 172800000 -> "গতকাল" // Yesterday
            else -> {
                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
    
    // Get activity icon based on type
    fun getActivityIcon(): String {
        return when (type) {
            ActivityType.CASH_DEPOSIT -> "💰"
            ActivityType.EXPENSE -> "💸"
            ActivityType.ORDER_CREATE -> "📦"
            ActivityType.ORDER_UPDATE -> "🔄"
            ActivityType.CUSTOMER_CREATE -> "👤"
            ActivityType.CUSTOMER_UPDATE -> "✏️"
            ActivityType.STOCK_ADD -> "📋"
            ActivityType.STOCK_UPDATE -> "🔄"
            ActivityType.DUE_ACCOUNT -> "💳"
            ActivityType.NOTE_CREATE -> "📝"
            ActivityType.NOTE_UPDATE -> "✏️"
        }
    }
    
    // Get formatted amount in Bengali
    fun getFormattedAmount(): String? {
        return amount?.let { 
            val prefix = if (isCredit) "+" else "-"
            "$prefix৳${String.format("%.0f", it)}"
        }
    }
}

// Transaction data class for backward compatibility
data class Transaction(
    val id: Int,
    val title: String,
    val amount: String,
    val type: String, // "credit" or "debit"
    val description: String,
    val date: String
)

// Convert RecentActivityEntry to Transaction for existing UI
fun RecentActivityEntry.toTransaction(): Transaction {
    return Transaction(
        id = id.hashCode(),
        title = title,
        amount = amount?.let { String.format("%.0f", it) } ?: "0",
        type = if (isCredit) "credit" else "debit",
        description = description,
        date = getBengaliTime()
    )
}
