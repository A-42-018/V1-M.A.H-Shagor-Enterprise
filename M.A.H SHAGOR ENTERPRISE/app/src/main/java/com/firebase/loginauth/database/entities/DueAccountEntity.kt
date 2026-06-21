package com.firebase.loginauth.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "due_accounts")
data class DueAccountEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String = "",
    val totalDueAmount: Double,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double,
    val dueDate: Date,
    val originalOrderId: String = "",
    val paymentHistory: List<String> = emptyList(), // JSON string of payment records
    val status: String = "Pending", // Pending, Partial, Cleared, Overdue
    val priority: String = "Medium", // Low, Medium, High, Critical
    val notes: String = "",
    val lastPaymentDate: Date? = null,
    val reminderDate: Date? = null,
    val isSynced: Boolean = false,
    val needsSync: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
