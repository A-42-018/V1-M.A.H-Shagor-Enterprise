package com.firebase.loginauth.backend.repositories

import android.content.Context
import com.firebase.loginauth.database.AppDatabase
import com.firebase.loginauth.database.entities.StockEntity
import com.firebase.loginauth.database.entities.SyncQueueEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StockBackendRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val database = AppDatabase.getDatabase(context)
    private val stockDao = database.stockDao()
    private val syncQueueDao = database.syncQueueDao()
    private val gson = Gson()
    
    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    
    private fun getUserStockCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("stocks")
    
    // **LOCAL OPERATIONS (Offline-First)**
    
    suspend fun addStock(brand: String, size: String, quantity: Int, pricePerUnit: Double): Result<StockEntity> {
        return try {
            val userId = getCurrentUserId()
            val stockId = UUID.randomUUID().toString()
            val currentTime = Date()
            
            // Check if stock already exists
            val existingStock = stockDao.getStockByBrandAndSize(userId, brand, size)
            
            val stock = if (existingStock != null) {
                // Update existing stock
                val newQuantity = existingStock.quantity + quantity
                val newTotalValue = newQuantity * pricePerUnit
                existingStock.copy(
                    quantity = newQuantity,
                    pricePerUnit = pricePerUnit,
                    totalValue = newTotalValue,
                    lastUpdated = currentTime,
                    needsSync = true,
                    isSynced = false
                )
            } else {
                // Create new stock
                StockEntity(
                    id = stockId,
                    userId = userId,
                    brand = brand,
                    size = size,
                    quantity = quantity,
                    pricePerUnit = pricePerUnit,
                    totalValue = quantity * pricePerUnit,
                    dateAdded = currentTime,
                    lastUpdated = currentTime,
                    isLowStock = quantity <= 5,
                    needsSync = true,
                    isSynced = false
                )
            }
            
            // Save to local database first
            stockDao.insertStock(stock)
            
            // Add to sync queue for Firebase sync
            addToSyncQueue(stock, if (existingStock != null) "UPDATE" else "CREATE")
            
            // Try immediate sync if online
            trySyncStock(stock)
            
            Result.success(stock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateStockQuantity(stockId: String, newQuantity: Int): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val stock = stockDao.getStockById(userId, stockId)
                ?: return Result.failure(Exception("Stock not found"))
            
            val currentTime = Date()
            val newTotalValue = newQuantity * stock.pricePerUnit
            
            // Update local database
            stockDao.updateStockQuantity(
                userId = userId,
                stockId = stockId,
                newQuantity = newQuantity,
                totalValue = newTotalValue,
                lastUpdated = currentTime.time
            )
            
            // Get updated stock for sync
            val updatedStock = stock.copy(
                quantity = newQuantity,
                totalValue = newTotalValue,
                lastUpdated = currentTime,
                isLowStock = newQuantity <= stock.lowStockThreshold,
                needsSync = true,
                isSynced = false
            )
            
            // Add to sync queue
            addToSyncQueue(updatedStock, "UPDATE")
            
            // Try immediate sync
            trySyncStock(updatedStock)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deleteStock(stockId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val stock = stockDao.getStockById(userId, stockId)
                ?: return Result.failure(Exception("Stock not found"))
            
            // Delete from local database
            stockDao.deleteStockById(userId, stockId)
            
            // Add to sync queue for Firebase deletion
            addToSyncQueue(stock, "DELETE")
            
            // Try immediate sync
            trySyncStockDeletion(stockId)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **FIREBASE SYNC OPERATIONS**
    
    private suspend fun trySyncStock(stock: StockEntity) {
        try {
            val stockData = mapOf(
                "id" to stock.id,
                "userId" to stock.userId,
                "brand" to stock.brand,
                "size" to stock.size,
                "quantity" to stock.quantity,
                "pricePerUnit" to stock.pricePerUnit,
                "totalValue" to stock.totalValue,
                "dateAdded" to stock.dateAdded.time,
                "lastUpdated" to stock.lastUpdated.time,
                "isLowStock" to stock.isLowStock,
                "lowStockThreshold" to stock.lowStockThreshold
            )
            
            getUserStockCollection()
                .document(stock.id)
                .set(stockData)
                .await()
            
            // Mark as synced in local database
            stockDao.updateSyncStatus(stock.userId, stock.id, isSynced = true, needsSync = false)
            
            // Remove from sync queue
            removeSyncQueueItem("stock", stock.id)
            
        } catch (e: Exception) {
            // Sync failed, will retry later
            println("Stock sync failed: ${e.message}")
        }
    }
    
    private suspend fun trySyncStockDeletion(stockId: String) {
        try {
            getUserStockCollection()
                .document(stockId)
                .delete()
                .await()
            
            // Remove from sync queue
            removeSyncQueueItem("stock", stockId)
            
        } catch (e: Exception) {
            println("Stock deletion sync failed: ${e.message}")
        }
    }
    
    suspend fun syncAllStocks(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val unsyncedStocks = stockDao.getUnsyncedStocks(userId)
            
            for (stock in unsyncedStocks) {
                trySyncStock(stock)
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFromFirebase(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = getUserStockCollection().get().await()
            
            val firebaseStocks = snapshot.documents.mapNotNull { doc ->
                try {
                    StockEntity(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        brand = doc.getString("brand") ?: "",
                        size = doc.getString("size") ?: "",
                        quantity = doc.getLong("quantity")?.toInt() ?: 0,
                        pricePerUnit = doc.getDouble("pricePerUnit") ?: 0.0,
                        totalValue = doc.getDouble("totalValue") ?: 0.0,
                        dateAdded = Date(doc.getLong("dateAdded") ?: 0),
                        lastUpdated = Date(doc.getLong("lastUpdated") ?: 0),
                        isLowStock = doc.getBoolean("isLowStock") ?: false,
                        lowStockThreshold = doc.getLong("lowStockThreshold")?.toInt() ?: 5,
                        isSynced = true,
                        needsSync = false
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Insert/update local database with Firebase data
            stockDao.insertStocks(firebaseStocks)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DATA ACCESS METHODS**
    
    fun getAllStocks(): Flow<List<StockEntity>> {
        return stockDao.getAllStocks(getCurrentUserId())
    }
    
    fun getLowStocks(): Flow<List<StockEntity>> {
        return stockDao.getLowStocks(getCurrentUserId())
    }
    
    suspend fun getStockById(stockId: String): StockEntity? {
        return stockDao.getStockById(getCurrentUserId(), stockId)
    }
    
    suspend fun getTotalStockValue(): Double {
        return stockDao.getTotalStockValue(getCurrentUserId()) ?: 0.0
    }
    
    suspend fun getStockCount(): Int {
        return stockDao.getStockCount(getCurrentUserId())
    }
    
    // **SYNC QUEUE MANAGEMENT**
    
    private suspend fun addToSyncQueue(stock: StockEntity, operation: String) {
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            userId = stock.userId,
            entityType = "stock",
            entityId = stock.id,
            operation = operation,
            data = gson.toJson(stock),
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
    
    // **STOCK OPERATIONS FOR SALES**
    
    suspend fun decreaseStockOnSale(brand: String, size: String, quantity: Int): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val stock = stockDao.getStockByBrandAndSize(userId, brand, size)
                ?: return Result.failure(Exception("Stock not found for $brand $size"))
            
            if (stock.quantity < quantity) {
                return Result.failure(Exception("Insufficient stock. Available: ${stock.quantity}, Required: $quantity"))
            }
            
            val newQuantity = stock.quantity - quantity
            updateStockQuantity(stock.id, newQuantity)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
