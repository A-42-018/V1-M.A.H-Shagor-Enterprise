package com.firebase.loginauth.backend.repositories

import android.content.Context
import com.firebase.loginauth.database.AppDatabase
import com.firebase.loginauth.database.CylinderItem
import com.firebase.loginauth.database.entities.OrderEntity
import com.firebase.loginauth.database.entities.SyncQueueEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderBackendRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val database = AppDatabase.getDatabase(context)
    private val orderDao = database.orderDao()
    private val syncQueueDao = database.syncQueueDao()
    private val gson = Gson()
    
    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    
    private fun getUserOrderCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("orders")
    
    // **ORDER CREATION (Offline-First)**
    
    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        cylinderItems: List<CylinderItem>,
        notes: String = "",
        priority: String = "Medium"
    ): Result<OrderEntity> {
        return try {
            val userId = getCurrentUserId()
            val orderId = generateOrderId()
            val currentTime = Date()
            val totalAmount = cylinderItems.sumOf { it.quantity * it.pricePerUnit }
            
            val order = OrderEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                orderId = orderId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                orderDate = currentTime,
                cylinderItems = cylinderItems,
                totalAmount = totalAmount,
                status = "Pending",
                paymentStatus = "Pending",
                notes = notes,
                priority = priority,
                needsSync = true,
                isSynced = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Save to local database first (offline-first)
            orderDao.insertOrder(order)
            
            // Add to sync queue for Firebase sync
            addToSyncQueue(order, "CREATE")
            
            // Try immediate sync if online
            trySyncOrder(order)
            
            Result.success(order)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun generateOrderId(): String {
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HHmmss", Locale.getDefault())
        val date = dateFormat.format(Date())
        val time = timeFormat.format(Date())
        return "ORD-$date-$time"
    }
    
    // **ORDER STATUS MANAGEMENT**
    
    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            
            // Update local database
            orderDao.updateOrderStatus(userId, orderId, newStatus, currentTime.time)
            
            // Get updated order for sync
            val order = orderDao.getOrderById(userId, orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            val updatedOrder = order.copy(
                status = newStatus,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Add to sync queue
            addToSyncQueue(updatedOrder, "UPDATE")
            
            // Try immediate sync
            trySyncOrder(updatedOrder)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePaymentStatus(orderId: String, paymentStatus: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            
            // Update local database
            orderDao.updatePaymentStatus(userId, orderId, paymentStatus, currentTime.time)
            
            // Get updated order for sync
            val order = orderDao.getOrderById(userId, orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            val updatedOrder = order.copy(
                paymentStatus = paymentStatus,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Add to sync queue
            addToSyncQueue(updatedOrder, "UPDATE")
            
            // Try immediate sync
            trySyncOrder(updatedOrder)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun markOrderAsDelivered(orderId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val order = orderDao.getOrderById(userId, orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            val currentTime = Date()
            val updatedOrder = order.copy(
                status = "Delivered",
                deliveryDate = currentTime,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Update local database
            orderDao.updateOrder(updatedOrder)
            
            // Add to sync queue
            addToSyncQueue(updatedOrder, "UPDATE")
            
            // Try immediate sync
            trySyncOrder(updatedOrder)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **FIREBASE SYNC OPERATIONS**
    
    private suspend fun trySyncOrder(order: OrderEntity) {
        try {
            val orderData = mapOf(
                "id" to order.id,
                "userId" to order.userId,
                "orderId" to order.orderId,
                "customerName" to order.customerName,
                "customerPhone" to order.customerPhone,
                "customerAddress" to order.customerAddress,
                "orderDate" to order.orderDate.time,
                "cylinderItems" to order.cylinderItems.map { item ->
                    mapOf(
                        "brand" to item.brand,
                        "size" to item.size,
                        "quantity" to item.quantity,
                        "pricePerUnit" to item.pricePerUnit
                    )
                },
                "totalAmount" to order.totalAmount,
                "status" to order.status,
                "paymentStatus" to order.paymentStatus,
                "deliveryDate" to order.deliveryDate?.time,
                "notes" to order.notes,
                "priority" to order.priority,
                "createdAt" to order.createdAt.time,
                "updatedAt" to order.updatedAt.time
            )
            
            getUserOrderCollection()
                .document(order.id)
                .set(orderData)
                .await()
            
            // Mark as synced in local database
            orderDao.updateSyncStatus(order.userId, order.id, isSynced = true, needsSync = false)
            
            // Remove from sync queue
            removeSyncQueueItem("order", order.id)
            
        } catch (e: Exception) {
            println("Order sync failed: ${e.message}")
        }
    }
    
    suspend fun syncAllOrders(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val unsyncedOrders = orderDao.getUnsyncedOrders(userId)
            
            for (order in unsyncedOrders) {
                trySyncOrder(order)
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFromFirebase(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = getUserOrderCollection().get().await()
            
            val firebaseOrders = snapshot.documents.mapNotNull { doc ->
                try {
                    val cylinderItemsData = doc.get("cylinderItems") as? List<Map<String, Any>> ?: emptyList()
                    val cylinderItems = cylinderItemsData.map { item ->
                        CylinderItem(
                            brand = item["brand"] as? String ?: "",
                            size = item["size"] as? String ?: "",
                            quantity = (item["quantity"] as? Long)?.toInt() ?: 0,
                            pricePerUnit = item["pricePerUnit"] as? Double ?: 0.0
                        )
                    }
                    
                    OrderEntity(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        orderId = doc.getString("orderId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        customerPhone = doc.getString("customerPhone") ?: "",
                        customerAddress = doc.getString("customerAddress") ?: "",
                        orderDate = Date(doc.getLong("orderDate") ?: 0),
                        cylinderItems = cylinderItems,
                        totalAmount = doc.getDouble("totalAmount") ?: 0.0,
                        status = doc.getString("status") ?: "Pending",
                        paymentStatus = doc.getString("paymentStatus") ?: "Pending",
                        deliveryDate = doc.getLong("deliveryDate")?.let { Date(it) },
                        notes = doc.getString("notes") ?: "",
                        priority = doc.getString("priority") ?: "Medium",
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
            orderDao.insertOrders(firebaseOrders)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DATA ACCESS METHODS**
    
    fun getAllOrders(): Flow<List<OrderEntity>> {
        return orderDao.getAllOrders(getCurrentUserId())
    }
    
    fun getPendingOrders(): Flow<List<OrderEntity>> {
        return orderDao.getPendingOrders(getCurrentUserId())
    }
    
    fun getDeliveredOrders(): Flow<List<OrderEntity>> {
        return orderDao.getDeliveredOrders(getCurrentUserId())
    }
    
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByStatus(getCurrentUserId(), status)
    }
    
    fun getOrdersByCustomer(customerPhone: String): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByCustomer(getCurrentUserId(), customerPhone)
    }
    
    fun getOrdersByDateRange(startDate: Date, endDate: Date): Flow<List<OrderEntity>> {
        return orderDao.getOrdersByDateRange(getCurrentUserId(), startDate.time, endDate.time)
    }
    
    suspend fun getOrderById(orderId: String): OrderEntity? {
        return orderDao.getOrderById(getCurrentUserId(), orderId)
    }
    
    suspend fun getTotalSalesAmount(): Double {
        return orderDao.getTotalSalesAmount(getCurrentUserId()) ?: 0.0
    }
    
    suspend fun getOrderCountByStatus(status: String): Int {
        return orderDao.getOrderCountByStatus(getCurrentUserId(), status)
    }
    
    // **ORDER DELETION**
    
    suspend fun deleteOrder(orderId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val order = orderDao.getOrderById(userId, orderId)
                ?: return Result.failure(Exception("Order not found"))
            
            // Delete from local database
            orderDao.deleteOrderById(userId, orderId)
            
            // Add to sync queue for Firebase deletion
            addToSyncQueue(order, "DELETE")
            
            // Try immediate sync
            trySyncOrderDeletion(orderId)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun trySyncOrderDeletion(orderId: String) {
        try {
            getUserOrderCollection()
                .document(orderId)
                .delete()
                .await()
            
            // Remove from sync queue
            removeSyncQueueItem("order", orderId)
            
        } catch (e: Exception) {
            println("Order deletion sync failed: ${e.message}")
        }
    }
    
    // **SYNC QUEUE MANAGEMENT**
    
    private suspend fun addToSyncQueue(order: OrderEntity, operation: String) {
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            userId = order.userId,
            entityType = "order",
            entityId = order.id,
            operation = operation,
            data = gson.toJson(order),
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
