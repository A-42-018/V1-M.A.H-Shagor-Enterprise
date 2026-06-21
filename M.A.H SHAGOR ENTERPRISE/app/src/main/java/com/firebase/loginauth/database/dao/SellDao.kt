package com.firebase.loginauth.database.dao

import androidx.room.*
import com.firebase.loginauth.database.entities.SellEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SellDao {
    
    @Query("SELECT * FROM sells WHERE userId = :userId ORDER BY saleDate DESC")
    fun getAllSells(userId: String): Flow<List<SellEntity>>
    
    @Query("SELECT * FROM sells WHERE userId = :userId AND id = :sellId")
    suspend fun getSellById(userId: String, sellId: String): SellEntity?
    
    @Query("SELECT * FROM sells WHERE userId = :userId AND orderId = :orderId")
    suspend fun getSellByOrderId(userId: String, orderId: String): SellEntity?
    
    @Query("SELECT * FROM sells WHERE userId = :userId AND paymentStatus = :status ORDER BY saleDate DESC")
    fun getSellsByPaymentStatus(userId: String, status: String): Flow<List<SellEntity>>
    
    @Query("SELECT * FROM sells WHERE userId = :userId AND paymentStatus IN ('Partial', 'Due') ORDER BY saleDate DESC")
    fun getDueSells(userId: String): Flow<List<SellEntity>>
    
    @Query("SELECT * FROM sells WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedSells(userId: String): List<SellEntity>
    
    @Query("SELECT * FROM sells WHERE userId = :userId AND saleDate BETWEEN :startDate AND :endDate ORDER BY saleDate DESC")
    fun getSellsByDateRange(userId: String, startDate: Long, endDate: Long): Flow<List<SellEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSell(sell: SellEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSells(sells: List<SellEntity>)
    
    @Update
    suspend fun updateSell(sell: SellEntity)
    
    @Delete
    suspend fun deleteSell(sell: SellEntity)
    
    @Query("UPDATE sells SET paymentStatus = :paymentStatus, paidAmount = :paidAmount, dueAmount = :dueAmount, updatedAt = :updatedAt, needsSync = 1 WHERE userId = :userId AND id = :sellId")
    suspend fun updatePaymentInfo(userId: String, sellId: String, paymentStatus: String, paidAmount: Double, dueAmount: Double, updatedAt: Long)
    
    @Query("UPDATE sells SET isSynced = :isSynced, needsSync = :needsSync WHERE userId = :userId AND id = :sellId")
    suspend fun updateSyncStatus(userId: String, sellId: String, isSynced: Boolean, needsSync: Boolean)
    
    @Query("SELECT SUM(totalAmount) FROM sells WHERE userId = :userId")
    suspend fun getTotalSalesAmount(userId: String): Double?
    
    @Query("SELECT SUM(paidAmount) FROM sells WHERE userId = :userId")
    suspend fun getTotalPaidAmount(userId: String): Double?
    
    @Query("SELECT SUM(dueAmount) FROM sells WHERE userId = :userId")
    suspend fun getTotalDueAmount(userId: String): Double?
    
    @Query("SELECT SUM(totalAmount) FROM sells WHERE userId = :userId AND saleDate BETWEEN :startDate AND :endDate")
    suspend fun getSalesAmountByDateRange(userId: String, startDate: Long, endDate: Long): Double?
    
    @Query("SELECT COUNT(*) FROM sells WHERE userId = :userId")
    suspend fun getTotalSalesCount(userId: String): Int
}
