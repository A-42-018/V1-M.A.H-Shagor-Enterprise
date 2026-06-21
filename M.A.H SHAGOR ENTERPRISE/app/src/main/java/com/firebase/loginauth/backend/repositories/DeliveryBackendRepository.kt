package com.firebase.loginauth.backend.repositories

import android.content.Context
import com.firebase.loginauth.database.AppDatabase
import com.firebase.loginauth.database.entities.DeliveryEntity
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
class DeliveryBackendRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val stockRepository: StockBackendRepository,
    private val sellsRepository: SellsBackendRepository
) {
    private val database = AppDatabase.getDatabase(context)
    private val deliveryDao = database.deliveryDao()
    private val orderDao = database.orderDao()
    private val syncQueueDao = database.syncQueueDao()
    private val gson = Gson()
    
    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    
    private fun getUserDeliveryCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("deliveries")
    
    // **DELIVERY CREATION FROM PENDING ORDERS**
    
    suspend fun createDeliveryFromOrder(orderId: String, deliveryPersonName: String = ""): Result<DeliveryEntity> {
        return try {
            val userId = getCurrentUserId()
            val order = orderDao.getOrderById(userId, orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            if (order.status != "Pending" && order.status != "Ready") {
                return Result.failure(Exception("Order is not ready for delivery"))
            }
            
            val currentTime = Date()
            val delivery = DeliveryEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                orderId = order.id,
                customerName = order.customerName,
                customerPhone = order.customerPhone,
                customerAddress = order.customerAddress,
                deliveryDate = currentTime,
                deliveryTime = currentTime.toString(),
                deliveryStatus = "Pending",
                deliveryPersonName = deliveryPersonName,
                totalAmount = order.totalAmount,
                needsSync = true,
                isSynced = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Save delivery to local database
            deliveryDao.insertDelivery(delivery)
            
            // Update order status to "Out for Delivery"
            orderDao.updateOrderStatus(userId, order.id, "Out for Delivery", currentTime.time)
            
            // Add to sync queue
            addToSyncQueue(delivery, "CREATE")
            
            // Try immediate sync
            trySyncDelivery(delivery)
            
            Result.success(delivery)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DELIVERY CONFIRMATION & STOCK UPDATE**
    
    suspend fun confirmDelivery(deliveryId: String, deliveryNotes: String = ""): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val delivery = deliveryDao.getDeliveryById(userId, deliveryId)
                ?: return Result.failure(Exception("Delivery not found"))
            
            val order = orderDao.getOrderById(userId, delivery.orderId)
                ?: return Result.failure(Exception("Associated order not found"))
            
            val currentTime = Date()
            
            // 1. Update delivery status to "Delivered"
            val updatedDelivery = delivery.copy(
                deliveryStatus = "Delivered",
                deliveryNotes = deliveryNotes,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            deliveryDao.updateDelivery(updatedDelivery)
            
            // 2. Update order status to "Delivered"
            val updatedOrder = order.copy(
                status = "Delivered",
                deliveryDate = currentTime,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            orderDao.updateOrder(updatedOrder)
            
            // 3. Decrease stock quantities for each cylinder item
            for (cylinderItem in order.cylinderItems) {
                val stockResult = stockRepository.decreaseStockOnSale(
                    brand = cylinderItem.brand,
                    size = cylinderItem.size,
                    quantity = cylinderItem.quantity
                )
                if (stockResult.isFailure) {
                    return Result.failure(Exception("Failed to update stock for ${cylinderItem.brand} ${cylinderItem.size}: ${stockResult.exceptionOrNull()?.message}"))
                }
            }
            
            // 4. Auto-move to Sells Management
            val sellResult = sellsRepository.createSellFromDeliveredOrder(order)
            if (sellResult.isFailure) {
                return Result.failure(Exception("Failed to create sell record: ${sellResult.exceptionOrNull()?.message}"))
            }
            
            // 5. Add to sync queue
            addToSyncQueue(updatedDelivery, "UPDATE")
            
            // 6. Try immediate sync
            trySyncDelivery(updatedDelivery)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DELIVERY STATUS MANAGEMENT**
    
    suspend fun updateDeliveryStatus(deliveryId: String, newStatus: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            
            // Update local database
            deliveryDao.updateDeliveryStatus(userId, deliveryId, newStatus, currentTime.time)
            
            // Get updated delivery for sync
            val delivery = deliveryDao.getDeliveryById(userId, deliveryId)
                ?: return Result.failure(Exception("Delivery not found"))
            
            val updatedDelivery = delivery.copy(
                deliveryStatus = newStatus,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Add to sync queue
            addToSyncQueue(updatedDelivery, "UPDATE")
            
            // Try immediate sync
            trySyncDelivery(updatedDelivery)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **FIREBASE SYNC OPERATIONS**
    
    private suspend fun trySyncDelivery(delivery: DeliveryEntity) {
        try {
            val deliveryData = mapOf(
                "id" to delivery.id,
                "userId" to delivery.userId,
                "orderId" to delivery.orderId,
                "customerName" to delivery.customerName,
                "customerPhone" to delivery.customerPhone,
                "customerAddress" to delivery.customerAddress,
                "deliveryDate" to delivery.deliveryDate.time,
                "deliveryTime" to delivery.deliveryTime,
                "deliveryStatus" to delivery.deliveryStatus,
                "deliveryPersonName" to delivery.deliveryPersonName,
                "deliveryNotes" to delivery.deliveryNotes,
                "totalAmount" to delivery.totalAmount,
                "createdAt" to delivery.createdAt.time,
                "updatedAt" to delivery.updatedAt.time
            )
            
            getUserDeliveryCollection()
                .document(delivery.id)
                .set(deliveryData)
                .await()
            
            // Mark as synced in local database
            deliveryDao.updateSyncStatus(delivery.userId, delivery.id, isSynced = true, needsSync = false)
            
            // Remove from sync queue
            removeSyncQueueItem("delivery", delivery.id)
            
        } catch (e: Exception) {
            println("Delivery sync failed: ${e.message}")
        }
    }
    
    suspend fun syncAllDeliveries(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val unsyncedDeliveries = deliveryDao.getUnsyncedDeliveries(userId)
            
            for (delivery in unsyncedDeliveries) {
                trySyncDelivery(delivery)
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFromFirebase(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = getUserDeliveryCollection().get().await()
            
            val firebaseDeliveries = snapshot.documents.mapNotNull { doc ->
                try {
                    DeliveryEntity(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        orderId = doc.getString("orderId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        customerPhone = doc.getString("customerPhone") ?: "",
                        customerAddress = doc.getString("customerAddress") ?: "",
                        deliveryDate = Date(doc.getLong("deliveryDate") ?: 0),
                        deliveryTime = doc.getString("deliveryTime") ?: "",
                        deliveryStatus = doc.getString("deliveryStatus") ?: "Pending",
                        deliveryPersonName = doc.getString("deliveryPersonName") ?: "",
                        deliveryNotes = doc.getString("deliveryNotes") ?: "",
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
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
            deliveryDao.insertDeliveries(firebaseDeliveries)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DATA ACCESS METHODS**
    
    fun getAllDeliveries(): Flow<List<DeliveryEntity>> {
        return deliveryDao.getAllDeliveries(getCurrentUserId())
    }
    
    fun getPendingDeliveries(): Flow<List<DeliveryEntity>> {
        return deliveryDao.getPendingDeliveries(getCurrentUserId())
    }
    
    fun getDeliveriesByStatus(status: String): Flow<List<DeliveryEntity>> {
        return deliveryDao.getDeliveriesByStatus(getCurrentUserId(), status)
    }
    
    suspend fun getDeliveryById(deliveryId: String): DeliveryEntity? {
        return deliveryDao.getDeliveryById(getCurrentUserId(), deliveryId)
    }
    
    suspend fun getDeliveryByOrderId(orderId: String): DeliveryEntity? {
        return deliveryDao.getDeliveryByOrderId(getCurrentUserId(), orderId)
    }
    
    suspend fun getCompletedDeliveryCount(): Int {
        return deliveryDao.getCompletedDeliveryCount(getCurrentUserId())
    }
    
    // **DELIVERY DELETION**
    
    suspend fun deleteDelivery(deliveryId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val delivery = deliveryDao.getDeliveryById(userId, deliveryId)
                ?: return Result.failure(Exception("Delivery not found"))
            
            // Delete from local database
            deliveryDao.deleteDelivery(delivery)
            
            // Add to sync queue for Firebase deletion
            addToSyncQueue(delivery, "DELETE")
            
            // Try immediate sync
            trySyncDeliveryDeletion(deliveryId)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun trySyncDeliveryDeletion(deliveryId: String) {
        try {
            getUserDeliveryCollection()
                .document(deliveryId)
                .delete()
                .await()
            
            // Remove from sync queue
            removeSyncQueueItem("delivery", deliveryId)
            
        } catch (e: Exception) {
            println("Delivery deletion sync failed: ${e.message}")
        }
    }
    
    // **SYNC QUEUE MANAGEMENT**
    
    private suspend fun addToSyncQueue(delivery: DeliveryEntity, operation: String) {
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            userId = delivery.userId,
            entityType = "delivery",
            entityId = delivery.id,
            operation = operation,
            data = gson.toJson(delivery),
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
