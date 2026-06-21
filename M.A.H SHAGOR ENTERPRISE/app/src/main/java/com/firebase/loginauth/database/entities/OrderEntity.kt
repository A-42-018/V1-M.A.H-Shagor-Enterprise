package com.firebase.loginauth.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.firebase.loginauth.database.CylinderItem
import java.util.*

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val orderId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String = "",
    val orderDate: Date,
    val cylinderItems: List<CylinderItem>,
    val totalAmount: Double,
    val status: String = "Pending", // Pending, Processing, Ready, Delivered, Cancelled
    val paymentStatus: String = "Pending", // Pending, Partial, Paid, Overdue
    val deliveryDate: Date? = null,
    val notes: String = "",
    val priority: String = "Medium", // Low, Medium, High, Urgent
    val isSynced: Boolean = false,
    val needsSync: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
