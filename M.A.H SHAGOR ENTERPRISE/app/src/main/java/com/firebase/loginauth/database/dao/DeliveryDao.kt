package com.firebase.loginauth.database.dao

import androidx.room.*
import com.firebase.loginauth.database.entities.DeliveryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryDao {
    
    @Query("SELECT * FROM deliveries WHERE userId = :userId ORDER BY deliveryDate DESC")
    fun getAllDeliveries(userId: String): Flow<List<DeliveryEntity>>
    
    @Query("SELECT * FROM deliveries WHERE userId = :userId AND id = :deliveryId")
    suspend fun getDeliveryById(userId: String, deliveryId: String): DeliveryEntity?
    
    @Query("SELECT * FROM deliveries WHERE userId = :userId AND orderId = :orderId")
    suspend fun getDeliveryByOrderId(userId: String, orderId: String): DeliveryEntity?
    
    @Query("SELECT * FROM deliveries WHERE userId = :userId AND deliveryStatus = :status ORDER BY deliveryDate DESC")
    fun getDeliveriesByStatus(userId: String, status: String): Flow<List<DeliveryEntity>>
    
    @Query("SELECT * FROM deliveries WHERE userId = :userId AND deliveryStatus = 'Pending' ORDER BY deliveryDate ASC")
    fun getPendingDeliveries(userId: String): Flow<List<DeliveryEntity>>
    
    @Query("SELECT * FROM deliveries WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedDeliveries(userId: String): List<DeliveryEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveries(deliveries: List<DeliveryEntity>)
    
    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)
    
    @Delete
    suspend fun deleteDelivery(delivery: DeliveryEntity)
    
    @Query("UPDATE deliveries SET deliveryStatus = :status, updatedAt = :updatedAt, needsSync = 1 WHERE userId = :userId AND id = :deliveryId")
    suspend fun updateDeliveryStatus(userId: String, deliveryId: String, status: String, updatedAt: Long)
    
    @Query("UPDATE deliveries SET isSynced = :isSynced, needsSync = :needsSync WHERE userId = :userId AND id = :deliveryId")
    suspend fun updateSyncStatus(userId: String, deliveryId: String, isSynced: Boolean, needsSync: Boolean)
    
    @Query("SELECT COUNT(*) FROM deliveries WHERE userId = :userId AND deliveryStatus = 'Delivered'")
    suspend fun getCompletedDeliveryCount(userId: String): Int
}
