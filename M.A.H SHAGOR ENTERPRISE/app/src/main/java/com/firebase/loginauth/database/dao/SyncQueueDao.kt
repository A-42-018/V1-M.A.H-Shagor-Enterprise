package com.firebase.loginauth.database.dao

import androidx.room.*
import com.firebase.loginauth.database.entities.SyncQueueEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncQueueDao {
    
    @Query("SELECT * FROM sync_queue WHERE userId = :userId ORDER BY priority ASC, createdAt ASC")
    fun getAllSyncItems(userId: String): Flow<List<SyncQueueEntity>>
    
    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND status = 'PENDING' ORDER BY priority ASC, createdAt ASC")
    suspend fun getPendingSyncItems(userId: String): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND status = 'FAILED' AND retryCount < maxRetries ORDER BY priority ASC, createdAt ASC")
    suspend fun getRetryableSyncItems(userId: String): List<SyncQueueEntity>
    
    @Query("SELECT * FROM sync_queue WHERE userId = :userId AND entityType = :entityType AND entityId = :entityId")
    suspend fun getSyncItemByEntity(userId: String, entityType: String, entityId: String): SyncQueueEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(syncItem: SyncQueueEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItems(syncItems: List<SyncQueueEntity>)
    
    @Update
    suspend fun updateSyncItem(syncItem: SyncQueueEntity)
    
    @Delete
    suspend fun deleteSyncItem(syncItem: SyncQueueEntity)
    
    @Query("DELETE FROM sync_queue WHERE userId = :userId AND id = :syncId")
    suspend fun deleteSyncItemById(userId: String, syncId: String)
    
    @Query("UPDATE sync_queue SET status = :status, retryCount = :retryCount, lastAttempt = :lastAttempt, errorMessage = :errorMessage WHERE userId = :userId AND id = :syncId")
    suspend fun updateSyncStatus(userId: String, syncId: String, status: String, retryCount: Int, lastAttempt: Long, errorMessage: String)
    
    @Query("DELETE FROM sync_queue WHERE userId = :userId AND status = 'COMPLETED'")
    suspend fun clearCompletedSyncItems(userId: String)
    
    @Query("DELETE FROM sync_queue WHERE userId = :userId AND status = 'FAILED' AND retryCount >= maxRetries")
    suspend fun clearFailedSyncItems(userId: String)
    
    @Query("SELECT COUNT(*) FROM sync_queue WHERE userId = :userId AND status = 'PENDING'")
    suspend fun getPendingSyncCount(userId: String): Int
}
