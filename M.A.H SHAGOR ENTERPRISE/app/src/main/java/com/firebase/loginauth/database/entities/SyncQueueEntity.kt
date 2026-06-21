package com.firebase.loginauth.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val id: String,
    val userId: String,
    val entityType: String, // "stock", "order", "delivery", "sell", "due_account"
    val entityId: String,
    val operation: String, // "CREATE", "UPDATE", "DELETE"
    val data: String, // JSON representation of the entity
    val priority: Int = 1, // 1 = High, 2 = Medium, 3 = Low
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val status: String = "PENDING", // PENDING, IN_PROGRESS, COMPLETED, FAILED
    val errorMessage: String = "",
    val createdAt: Date = Date(),
    val lastAttempt: Date? = null,
    val nextRetry: Date? = null
)
