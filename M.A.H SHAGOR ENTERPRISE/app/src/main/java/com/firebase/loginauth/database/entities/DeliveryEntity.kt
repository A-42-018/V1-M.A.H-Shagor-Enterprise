package com.firebase.loginauth.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "deliveries")
data class DeliveryEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val orderId: String,
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val deliveryDate: Date,
    val deliveryTime: String,
    val deliveryStatus: String = "Pending", // Pending, InTransit, Delivered, Failed
    val deliveryPersonName: String = "",
    val deliveryNotes: String = "",
    val totalAmount: Double,
    val isSynced: Boolean = false,
    val needsSync: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
