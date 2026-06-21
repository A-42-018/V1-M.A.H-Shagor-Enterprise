package com.firebase.loginauth.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.firebase.loginauth.database.CylinderItem
import java.util.*

@Entity(tableName = "sells")
data class SellEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val orderId: String,
    val customerName: String,
    val customerPhone: String,
    val saleDate: Date,
    val cylinderItems: List<CylinderItem>,
    val totalAmount: Double,
    val paidAmount: Double = 0.0,
    val dueAmount: Double = 0.0,
    val paymentMethod: String = "Cash", // Cash, Card, Bank Transfer, Due
    val paymentStatus: String = "Paid", // Paid, Partial, Due
    val salesPersonName: String = "",
    val discount: Double = 0.0,
    val tax: Double = 0.0,
    val notes: String = "",
    val isSynced: Boolean = false,
    val needsSync: Boolean = true,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
)
