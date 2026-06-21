package com.firebase.loginauth.database.dao

import androidx.room.*
import com.firebase.loginauth.database.entities.DueAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DueAccountDao {
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId ORDER BY dueDate ASC")
    fun getAllDueAccounts(userId: String): Flow<List<DueAccountEntity>>
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId AND id = :dueId")
    suspend fun getDueAccountById(userId: String, dueId: String): DueAccountEntity?
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId AND customerPhone = :phone ORDER BY dueDate ASC")
    fun getDueAccountsByCustomer(userId: String, phone: String): Flow<List<DueAccountEntity>>
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId AND status = :status ORDER BY dueDate ASC")
    fun getDueAccountsByStatus(userId: String, status: String): Flow<List<DueAccountEntity>>
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId AND status IN ('Pending', 'Partial') ORDER BY dueDate ASC")
    fun getActiveDueAccounts(userId: String): Flow<List<DueAccountEntity>>
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId AND dueDate < :currentDate AND status != 'Cleared' ORDER BY dueDate ASC")
    fun getOverdueDueAccounts(userId: String, currentDate: Long): Flow<List<DueAccountEntity>>
    
    @Query("SELECT * FROM due_accounts WHERE userId = :userId AND needsSync = 1")
    suspend fun getUnsyncedDueAccounts(userId: String): List<DueAccountEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDueAccount(dueAccount: DueAccountEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDueAccounts(dueAccounts: List<DueAccountEntity>)
    
    @Update
    suspend fun updateDueAccount(dueAccount: DueAccountEntity)
    
    @Delete
    suspend fun deleteDueAccount(dueAccount: DueAccountEntity)
    
    @Query("UPDATE due_accounts SET paidAmount = :paidAmount, remainingAmount = :remainingAmount, status = :status, lastPaymentDate = :lastPaymentDate, updatedAt = :updatedAt, needsSync = 1 WHERE userId = :userId AND id = :dueId")
    suspend fun updatePayment(userId: String, dueId: String, paidAmount: Double, remainingAmount: Double, status: String, lastPaymentDate: Long?, updatedAt: Long)
    
    @Query("UPDATE due_accounts SET isSynced = :isSynced, needsSync = :needsSync WHERE userId = :userId AND id = :dueId")
    suspend fun updateSyncStatus(userId: String, dueId: String, isSynced: Boolean, needsSync: Boolean)
    
    @Query("SELECT SUM(remainingAmount) FROM due_accounts WHERE userId = :userId AND status != 'Cleared'")
    suspend fun getTotalDueAmount(userId: String): Double?
    
    @Query("SELECT COUNT(*) FROM due_accounts WHERE userId = :userId AND status != 'Cleared'")
    suspend fun getActiveDueCount(userId: String): Int
    
    @Query("SELECT COUNT(*) FROM due_accounts WHERE userId = :userId AND dueDate < :currentDate AND status != 'Cleared'")
    suspend fun getOverdueCount(userId: String, currentDate: Long): Int
}
