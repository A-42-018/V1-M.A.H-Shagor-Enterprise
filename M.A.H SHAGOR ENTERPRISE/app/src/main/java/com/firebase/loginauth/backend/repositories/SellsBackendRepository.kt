package com.firebase.loginauth.backend.repositories

import android.content.Context
import com.firebase.loginauth.database.AppDatabase
import com.firebase.loginauth.database.entities.OrderEntity
import com.firebase.loginauth.database.entities.SellEntity
import com.firebase.loginauth.database.entities.SyncQueueEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SellsBackendRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val database = AppDatabase.getDatabase(context)
    private val sellDao = database.sellDao()
    private val syncQueueDao = database.syncQueueDao()
    private val gson = Gson()
    
    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    
    private fun getUserSellCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("sells")
    
    // **AUTO-CREATE SELL FROM DELIVERED ORDER**
    
    suspend fun createSellFromDeliveredOrder(order: OrderEntity): Result<SellEntity> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            
            // Determine payment status based on order payment status
            val (paidAmount, dueAmount, paymentStatus) = when (order.paymentStatus) {
                "Paid" -> Triple(order.totalAmount, 0.0, "Paid")
                "Partial" -> Triple(order.totalAmount * 0.5, order.totalAmount * 0.5, "Partial")
                "Due", "Pending" -> Triple(0.0, order.totalAmount, "Due")
                else -> Triple(order.totalAmount, 0.0, "Paid")
            }
            
            val sell = SellEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                orderId = order.id,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                saleDate = currentTime,
                cylinderItems = order.cylinderItems,
                totalAmount = order.totalAmount,
                paidAmount = paidAmount,
                dueAmount = dueAmount,
                paymentMethod = if (paidAmount > 0) "Cash" else "Due",
                paymentStatus = paymentStatus,
                notes = "Auto-created from delivered order: ${order.orderId}",
                needsSync = true,
                isSynced = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Save to local database
            sellDao.insertSell(sell)
            
            // Add to sync queue
            addToSyncQueue(sell, "CREATE")
            
            // Try immediate sync
            trySyncSell(sell)
            
            Result.success(sell)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **MANUAL SELL CREATION**
    
    suspend fun createDirectSell(
        customerName: String,
        customerPhone: String,
        cylinderItems: List<com.firebase.loginauth.database.CylinderItem>,
        paymentMethod: String = "Cash",
        paidAmount: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
        salesPersonName: String = "",
        notes: String = ""
    ): Result<SellEntity> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            val totalAmount = cylinderItems.sumOf { it.quantity * it.pricePerUnit } - discount + tax
            val dueAmount = totalAmount - paidAmount
            
            val paymentStatus = when {
                dueAmount <= 0 -> "Paid"
                paidAmount <= 0 -> "Due"
                else -> "Partial"
            }
            
            val sell = SellEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                orderId = "", // Direct sell, no associated order
                customerName = customerName,
                customerPhone = customerPhone,
                saleDate = currentTime,
                cylinderItems = cylinderItems,
                totalAmount = totalAmount,
                paidAmount = paidAmount,
                dueAmount = dueAmount,
                paymentMethod = paymentMethod,
                paymentStatus = paymentStatus,
                salesPersonName = salesPersonName,
                discount = discount,
                tax = tax,
                notes = notes,
                needsSync = true,
                isSynced = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Save to local database
            sellDao.insertSell(sell)
            
            // Add to sync queue
            addToSyncQueue(sell, "CREATE")
            
            // Try immediate sync
            trySyncSell(sell)
            
            Result.success(sell)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **PAYMENT MANAGEMENT**
    
    suspend fun updatePayment(
        sellId: String,
        additionalPayment: Double,
        paymentMethod: String = "Cash"
    ): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val sell = sellDao.getSellById(userId, sellId)
                ?: return Result.failure(Exception("Sell record not found"))
            
            val newPaidAmount = sell.paidAmount + additionalPayment
            val newDueAmount = sell.totalAmount - newPaidAmount
            
            val newPaymentStatus = when {
                newDueAmount <= 0 -> "Paid"
                newPaidAmount <= 0 -> "Due"
                else -> "Partial"
            }
            
            val currentTime = Date()
            
            // Update local database
            sellDao.updatePaymentInfo(
                userId = userId,
                sellId = sellId,
                paymentStatus = newPaymentStatus,
                paidAmount = newPaidAmount,
                dueAmount = maxOf(0.0, newDueAmount),
                updatedAt = currentTime.time
            )
            
            // Get updated sell for sync
            val updatedSell = sell.copy(
                paidAmount = newPaidAmount,
                dueAmount = maxOf(0.0, newDueAmount),
                paymentStatus = newPaymentStatus,
                paymentMethod = paymentMethod,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Add to sync queue
            addToSyncQueue(updatedSell, "UPDATE")
            
            // Try immediate sync
            trySyncSell(updatedSell)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **SALES ANALYTICS**
    
    suspend fun getDailySales(date: Date): Double {
        val startOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val endOfDay = Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.time
        
        return sellDao.getSalesAmountByDateRange(getCurrentUserId(), startOfDay.time, endOfDay.time) ?: 0.0
    }
    
    suspend fun getMonthlySales(year: Int, month: Int): Double {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.time
        
        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.time
        
        return sellDao.getSalesAmountByDateRange(getCurrentUserId(), startOfMonth.time, endOfMonth.time) ?: 0.0
    }
    
    suspend fun getCustomSales(startDate: Date, endDate: Date): Double {
        return sellDao.getSalesAmountByDateRange(getCurrentUserId(), startDate.time, endDate.time) ?: 0.0
    }
    
    suspend fun getSalesAnalytics(): SalesAnalytics {
        val userId = getCurrentUserId()
        return SalesAnalytics(
            totalSalesAmount = sellDao.getTotalSalesAmount(userId) ?: 0.0,
            totalPaidAmount = sellDao.getTotalPaidAmount(userId) ?: 0.0,
            totalDueAmount = sellDao.getTotalDueAmount(userId) ?: 0.0,
            totalSalesCount = sellDao.getTotalSalesCount(userId)
        )
    }
    
    // **FIREBASE SYNC OPERATIONS**
    
    private suspend fun trySyncSell(sell: SellEntity) {
        try {
            val sellData = mapOf(
                "id" to sell.id,
                "userId" to sell.userId,
                "orderId" to sell.orderId,
                "customerName" to sell.customerName,
                "customerPhone" to sell.customerPhone,
                "saleDate" to sell.saleDate.time,
                "cylinderItems" to sell.cylinderItems.map { item ->
                    mapOf(
                        "brand" to item.brand,
                        "size" to item.size,
                        "quantity" to item.quantity,
                        "pricePerUnit" to item.pricePerUnit
                    )
                },
                "totalAmount" to sell.totalAmount,
                "paidAmount" to sell.paidAmount,
                "dueAmount" to sell.dueAmount,
                "paymentMethod" to sell.paymentMethod,
                "paymentStatus" to sell.paymentStatus,
                "salesPersonName" to sell.salesPersonName,
                "discount" to sell.discount,
                "tax" to sell.tax,
                "notes" to sell.notes,
                "createdAt" to sell.createdAt.time,
                "updatedAt" to sell.updatedAt.time
            )
            
            getUserSellCollection()
                .document(sell.id)
                .set(sellData)
                .await()
            
            // Mark as synced in local database
            sellDao.updateSyncStatus(sell.userId, sell.id, isSynced = true, needsSync = false)
            
            // Remove from sync queue
            removeSyncQueueItem("sell", sell.id)
            
        } catch (e: Exception) {
            println("Sell sync failed: ${e.message}")
        }
    }
    
    suspend fun syncAllSells(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val unsyncedSells = sellDao.getUnsyncedSells(userId)
            
            for (sell in unsyncedSells) {
                trySyncSell(sell)
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFromFirebase(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = getUserSellCollection().get().await()
            
            val firebaseSells = snapshot.documents.mapNotNull { doc ->
                try {
                    val cylinderItemsData = doc.get("cylinderItems") as? List<Map<String, Any>> ?: emptyList()
                    val cylinderItems = cylinderItemsData.map { item ->
                        com.firebase.loginauth.database.CylinderItem(
                            brand = item["brand"] as? String ?: "",
                            size = item["size"] as? String ?: "",
                            quantity = (item["quantity"] as? Long)?.toInt() ?: 0,
                            pricePerUnit = item["pricePerUnit"] as? Double ?: 0.0
                        )
                    }
                    
                    SellEntity(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        orderId = doc.getString("orderId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        customerPhone = doc.getString("customerPhone") ?: "",
                        saleDate = Date(doc.getLong("saleDate") ?: 0),
                        cylinderItems = cylinderItems,
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                        dueAmount = doc.getDouble("dueAmount") ?: 0.0,
                        paymentMethod = doc.getString("paymentMethod") ?: "Cash",
                        paymentStatus = doc.getString("paymentStatus") ?: "Paid",
                        salesPersonName = doc.getString("salesPersonName") ?: "",
                        discount = doc.getDouble("discount") ?: 0.0,
                        tax = doc.getDouble("tax") ?: 0.0,
                        notes = doc.getString("notes") ?: "",
                        isSynced = true,
                        needsSync = false,
                        createdAt = Date(doc.getLong("createdAt") ?: 0),
                        updatedAt = Date(doc.getLong("updatedAt") ?: 0)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Insert/update local database with Firebase data
            sellDao.insertSells(firebaseSells)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DATA ACCESS METHODS**
    
    fun getAllSells(): Flow<List<SellEntity>> {
        return sellDao.getAllSells(getCurrentUserId())
    }
    
    fun getDueSells(): Flow<List<SellEntity>> {
        return sellDao.getDueSells(getCurrentUserId())
    }
    
    fun getSellsByPaymentStatus(status: String): Flow<List<SellEntity>> {
        return sellDao.getSellsByPaymentStatus(getCurrentUserId(), status)
    }
    
    fun getSellsByDateRange(startDate: Date, endDate: Date): Flow<List<SellEntity>> {
        return sellDao.getSellsByDateRange(getCurrentUserId(), startDate.time, endDate.time)
    }
    
    suspend fun getSellById(sellId: String): SellEntity? {
        return sellDao.getSellById(getCurrentUserId(), sellId)
    }
    
    suspend fun getSellByOrderId(orderId: String): SellEntity? {
        return sellDao.getSellByOrderId(getCurrentUserId(), orderId)
    }
    
    // **SELL DELETION**
    
    suspend fun deleteSell(sellId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val sell = sellDao.getSellById(userId, sellId)
                ?: return Result.failure(Exception("Sell record not found"))
            
            // Delete from local database
            sellDao.deleteSell(sell)
            
            // Add to sync queue for Firebase deletion
            addToSyncQueue(sell, "DELETE")
            
            // Try immediate sync
            trySyncSellDeletion(sellId)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun trySyncSellDeletion(sellId: String) {
        try {
            getUserSellCollection()
                .document(sellId)
                .delete()
                .await()
            
            // Remove from sync queue
            removeSyncQueueItem("sell", sellId)
            
        } catch (e: Exception) {
            println("Sell deletion sync failed: ${e.message}")
        }
    }
    
    // **SYNC QUEUE MANAGEMENT**
    
    private suspend fun addToSyncQueue(sell: SellEntity, operation: String) {
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            userId = sell.userId,
            entityType = "sell",
            entityId = sell.id,
            operation = operation,
            data = gson.toJson(sell),
            priority = 1,
            createdAt = Date()
        )
        syncQueueDao.insertSyncItem(syncItem)
    }
    
    private suspend fun removeSyncQueueItem(entityType: String, entityId: String) {
        val userId = getCurrentUserId()
        val syncItem = syncQueueDao.getSyncItemByEntity(userId, entityType, entityId)
        syncItem?.let { syncQueueDao.deleteSyncItem(it) }
    }
}

// **DATA CLASS FOR ANALYTICS**
data class SalesAnalytics(
    val totalSalesAmount: Double,
    val totalPaidAmount: Double,
    val totalDueAmount: Double,
    val totalSalesCount: Int
)
