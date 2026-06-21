package com.firebase.loginauth.database.dao

import androidx.room.*
import com.firebase.loginauth.database.entities.StockEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StockDao {
    
    @Query("SELECT * FROM stocks WHERE userId = :userId ORDER BY lastUpdated DESC")
    fun getAllStocks(userId: String): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM stocks WHERE userId = :userId AND id = :stockId")
    suspend fun getStockById(userId: String, stockId: String): StockEntity?
    
    @Query("SELECT * FROM stocks WHERE userId = :userId AND brand = :brand AND size = :size")
    suspend fun getStockByBrandAndSize(userId: String, brand: String, size: String): StockEntity?
    
    @Query("SELECT * FROM stocks WHERE userId = :userId AND quantity <= lowStockThreshold")
    fun getLowStocks(userId: String): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM stocks WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedStocks(userId: String): List<StockEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)
    
    @Update
    suspend fun updateStock(stock: StockEntity)
    
    @Delete
    suspend fun deleteStock(stock: StockEntity)
    
    @Query("DELETE FROM stocks WHERE userId = :userId AND id = :stockId")
    suspend fun deleteStockById(userId: String, stockId: String)
    
    @Query("UPDATE stocks SET quantity = :newQuantity, totalValue = :totalValue, lastUpdated = :lastUpdated, needsSync = 1 WHERE userId = :userId AND id = :stockId")
    suspend fun updateStockQuantity(userId: String, stockId: String, newQuantity: Int, totalValue: Double, lastUpdated: Long)
    
    @Query("UPDATE stocks SET isSynced = :isSynced, needsSync = :needsSync WHERE userId = :userId AND id = :stockId")
    suspend fun updateSyncStatus(userId: String, stockId: String, isSynced: Boolean, needsSync: Boolean)
    
    @Query("SELECT SUM(totalValue) FROM stocks WHERE userId = :userId")
    suspend fun getTotalStockValue(userId: String): Double?
    
    @Query("SELECT COUNT(*) FROM stocks WHERE userId = :userId")
    suspend fun getStockCount(userId: String): Int
}
