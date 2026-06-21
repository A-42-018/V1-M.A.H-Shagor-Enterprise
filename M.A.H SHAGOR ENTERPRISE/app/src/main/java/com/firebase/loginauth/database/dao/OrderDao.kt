package com.firebase.loginauth.database.dao

import androidx.room.*
import com.firebase.loginauth.database.entities.OrderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    
    @Query("SELECT * FROM orders WHERE userId = :userId ORDER BY orderDate DESC")
    fun getAllOrders(userId: String): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND id = :orderId")
    suspend fun getOrderById(userId: String, orderId: String): OrderEntity?
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND status = :status ORDER BY orderDate DESC")
    fun getOrdersByStatus(userId: String, status: String): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND status = 'Pending' ORDER BY orderDate DESC")
    fun getPendingOrders(userId: String): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND status = 'Delivered' ORDER BY orderDate DESC")
    fun getDeliveredOrders(userId: String): Flow<List<OrderEntity>>
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedOrders(userId: String): List<OrderEntity>
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND customerPhone = :phone ORDER BY orderDate DESC")
    fun getOrdersByCustomer(userId: String, phone: String): Flow<List<OrderEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrders(orders: List<OrderEntity>)
    
    @Update
    suspend fun updateOrder(order: OrderEntity)
    
    @Delete
    suspend fun deleteOrder(order: OrderEntity)
    
    @Query("DELETE FROM orders WHERE userId = :userId AND id = :orderId")
    suspend fun deleteOrderById(userId: String, orderId: String)
    
    @Query("UPDATE orders SET status = :status, updatedAt = :updatedAt, needsSync = 1 WHERE userId = :userId AND id = :orderId")
    suspend fun updateOrderStatus(userId: String, orderId: String, status: String, updatedAt: Long)
    
    @Query("UPDATE orders SET paymentStatus = :paymentStatus, updatedAt = :updatedAt, needsSync = 1 WHERE userId = :userId AND id = :orderId")
    suspend fun updatePaymentStatus(userId: String, orderId: String, paymentStatus: String, updatedAt: Long)
    
    @Query("UPDATE orders SET isSynced = :isSynced, needsSync = :needsSync WHERE userId = :userId AND id = :orderId")
    suspend fun updateSyncStatus(userId: String, orderId: String, isSynced: Boolean, needsSync: Boolean)
    
    @Query("SELECT SUM(totalAmount) FROM orders WHERE userId = :userId AND status = 'Delivered'")
    suspend fun getTotalSalesAmount(userId: String): Double?
    
    @Query("SELECT COUNT(*) FROM orders WHERE userId = :userId AND status = :status")
    suspend fun getOrderCountByStatus(userId: String, status: String): Int
    
    @Query("SELECT * FROM orders WHERE userId = :userId AND orderDate BETWEEN :startDate AND :endDate ORDER BY orderDate DESC")
    fun getOrdersByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<OrderEntity>>
}
