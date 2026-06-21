package com.firebase.loginauth.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "stocks")
data class StockEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val brand: String,
    val size: String,
    val quantity: Int,
    val pricePerUnit: Double,
    val totalValue: Double,
    val dateAdded: Date,
    val lastUpdated: Date,
    val isLowStock: Boolean = false,
    val lowStockThreshold: Int = 5,
    val isSynced: Boolean = false,
    val needsSync: Boolean = true
)
