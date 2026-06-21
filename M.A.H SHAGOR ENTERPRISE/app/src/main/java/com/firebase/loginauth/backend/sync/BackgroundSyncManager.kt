package com.firebase.loginauth.backend.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.firebase.loginauth.backend.repositories.*
import com.firebase.loginauth.database.AppDatabase
import com.firebase.loginauth.database.entities.SyncQueueEntity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundSyncManager @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val stockRepository: StockBackendRepository,
    private val orderRepository: OrderBackendRepository,
    private val deliveryRepository: DeliveryBackendRepository,
    private val sellsRepository: SellsBackendRepository,
    private val dueAccountRepository: DueAccountBackendRepository
) {
    private val database = AppDatabase.getDatabase(context)
    private val syncQueueDao = database.syncQueueDao()
    
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus
    
    private val _lastSyncTime = MutableStateFlow<Date?>(null)
    val lastSyncTime: StateFlow<Date?> = _lastSyncTime
    
    private val _pendingSyncCount = MutableStateFlow(0)
    val pendingSyncCount: StateFlow<Int> = _pendingSyncCount
    
    private var syncJob: Job? = null
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    enum class SyncStatus {
        IDLE, SYNCING, SUCCESS, ERROR, NO_INTERNET
    }
    
    // **AUTOMATIC SYNC INITIALIZATION**
    
    fun startPeriodicSync() {
        syncJob?.cancel()
        syncJob = syncScope.launch {
            while (isActive) {
                if (isInternetAvailable() && auth.currentUser != null) {
                    performFullSync()
                    updatePendingSyncCount()
                }
                delay(30000) // Sync every 30 seconds
            }
        }
    }
    
    fun stopPeriodicSync() {
        syncJob?.cancel()
        syncJob = null
    }
    
    // **MANUAL SYNC TRIGGER**
    
    suspend fun performFullSync(): Result<Boolean> {
        return try {
            if (!isInternetAvailable()) {
                _syncStatus.value = SyncStatus.NO_INTERNET
                return Result.failure(Exception("No internet connection"))
            }
            
            if (auth.currentUser == null) {
                return Result.failure(Exception("User not authenticated"))
            }
            
            _syncStatus.value = SyncStatus.SYNCING
            
            // 1. Process pending sync queue items
            processSyncQueue()
            
            // 2. Sync all repositories to Firebase
            val syncResults = listOf(
                stockRepository.syncAllStocks(),
                orderRepository.syncAllOrders(),
                deliveryRepository.syncAllDeliveries(),
                sellsRepository.syncAllSells(),
                dueAccountRepository.syncAllDueAccounts()
            )
            
            // 3. Sync from Firebase (conflict resolution: Firebase > Local)
            val downloadResults = listOf(
                stockRepository.syncFromFirebase(),
                orderRepository.syncFromFirebase(),
                deliveryRepository.syncFromFirebase(),
                sellsRepository.syncFromFirebase(),
                dueAccountRepository.syncFromFirebase()
            )
            
            // Check if all syncs were successful
            val allSyncsSuccessful = syncResults.all { it.isSuccess } && downloadResults.all { it.isSuccess }
            
            if (allSyncsSuccessful) {
                _syncStatus.value = SyncStatus.SUCCESS
                _lastSyncTime.value = Date()
                cleanupCompletedSyncItems()
            } else {
                _syncStatus.value = SyncStatus.ERROR
            }
            
            updatePendingSyncCount()
            
            Result.success(allSyncsSuccessful)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.ERROR
            Result.failure(e)
        }
    }
    
    // **SYNC QUEUE PROCESSING**
    
    private suspend fun processSyncQueue() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val pendingItems = syncQueueDao.getPendingSyncItems(userId)
            val retryableItems = syncQueueDao.getRetryableSyncItems(userId)
            
            val allItems = (pendingItems + retryableItems).sortedBy { it.priority }
            
            for (syncItem in allItems) {
                processSyncItem(syncItem)
                delay(100) // Small delay between sync operations
            }
        } catch (e: Exception) {
            println("Sync queue processing failed: ${e.message}")
        }
    }
    
    private suspend fun processSyncItem(syncItem: SyncQueueEntity) {
        try {
            // Update sync item status to IN_PROGRESS
            val updatedItem = syncItem.copy(
                status = "IN_PROGRESS",
                lastAttempt = Date()
            )
            syncQueueDao.updateSyncItem(updatedItem)
            
            // Process based on entity type and operation
            val success = when (syncItem.entityType) {
                "stock" -> processStockSync(syncItem)
                "order" -> processOrderSync(syncItem)
                "delivery" -> processDeliverySync(syncItem)
                "sell" -> processSellSync(syncItem)
                "due_account" -> processDueAccountSync(syncItem)
                else -> false
            }
            
            if (success) {
                // Mark as completed and remove from queue
                syncQueueDao.updateSyncStatus(
                    userId = syncItem.userId,
                    syncId = syncItem.id,
                    status = "COMPLETED",
                    retryCount = syncItem.retryCount,
                    lastAttempt = Date().time,
                    errorMessage = ""
                )
            } else {
                // Mark as failed and increment retry count
                val newRetryCount = syncItem.retryCount + 1
                val newStatus = if (newRetryCount >= syncItem.maxRetries) "FAILED" else "PENDING"
                
                syncQueueDao.updateSyncStatus(
                    userId = syncItem.userId,
                    syncId = syncItem.id,
                    status = newStatus,
                    retryCount = newRetryCount,
                    lastAttempt = Date().time,
                    errorMessage = "Sync failed after $newRetryCount attempts"
                )
            }
        } catch (e: Exception) {
            println("Failed to process sync item ${syncItem.id}: ${e.message}")
        }
    }
    
    // **ENTITY-SPECIFIC SYNC PROCESSING**
    
    private suspend fun processStockSync(syncItem: SyncQueueEntity): Boolean {
        return try {
            when (syncItem.operation) {
                "CREATE", "UPDATE" -> stockRepository.syncAllStocks().isSuccess
                "DELETE" -> true // Deletion is handled separately
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun processOrderSync(syncItem: SyncQueueEntity): Boolean {
        return try {
            when (syncItem.operation) {
                "CREATE", "UPDATE" -> orderRepository.syncAllOrders().isSuccess
                "DELETE" -> true // Deletion is handled separately
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun processDeliverySync(syncItem: SyncQueueEntity): Boolean {
        return try {
            when (syncItem.operation) {
                "CREATE", "UPDATE" -> deliveryRepository.syncAllDeliveries().isSuccess
                "DELETE" -> true // Deletion is handled separately
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun processSellSync(syncItem: SyncQueueEntity): Boolean {
        return try {
            when (syncItem.operation) {
                "CREATE", "UPDATE" -> sellsRepository.syncAllSells().isSuccess
                "DELETE" -> true // Deletion is handled separately
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun processDueAccountSync(syncItem: SyncQueueEntity): Boolean {
        return try {
            when (syncItem.operation) {
                "CREATE", "UPDATE" -> dueAccountRepository.syncAllDueAccounts().isSuccess
                "DELETE" -> true // Deletion is handled separately
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // **CONFLICT RESOLUTION (Firebase > Local)**
    
    suspend fun resolveConflicts(): Result<Boolean> {
        return try {
            // Firebase data takes precedence over local data
            // This is implemented in the syncFromFirebase methods of each repository
            
            val conflictResolutionResults = listOf(
                stockRepository.syncFromFirebase(),
                orderRepository.syncFromFirebase(),
                deliveryRepository.syncFromFirebase(),
                sellsRepository.syncFromFirebase(),
                dueAccountRepository.syncFromFirebase()
            )
            
            val allResolved = conflictResolutionResults.all { it.isSuccess }
            Result.success(allResolved)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **UTILITY METHODS**
    
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    
    private suspend fun updatePendingSyncCount() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val count = syncQueueDao.getPendingSyncCount(userId)
            _pendingSyncCount.value = count
        } catch (e: Exception) {
            println("Failed to update pending sync count: ${e.message}")
        }
    }
    
    private suspend fun cleanupCompletedSyncItems() {
        try {
            val userId = auth.currentUser?.uid ?: return
            syncQueueDao.clearCompletedSyncItems(userId)
            syncQueueDao.clearFailedSyncItems(userId)
        } catch (e: Exception) {
            println("Failed to cleanup sync items: ${e.message}")
        }
    }
    
    // **SYNC STATUS METHODS**
    
    fun getSyncStatusInfo(): SyncStatusInfo {
        return SyncStatusInfo(
            status = _syncStatus.value,
            lastSyncTime = _lastSyncTime.value,
            pendingSyncCount = _pendingSyncCount.value,
            isInternetAvailable = isInternetAvailable(),
            isUserAuthenticated = auth.currentUser != null
        )
    }
    
    // **MANUAL SYNC TRIGGERS FOR SPECIFIC ENTITIES**
    
    suspend fun syncStocksOnly(): Result<Boolean> {
        return if (isInternetAvailable()) {
            stockRepository.syncAllStocks()
        } else {
            Result.failure(Exception("No internet connection"))
        }
    }
    
    suspend fun syncOrdersOnly(): Result<Boolean> {
        return if (isInternetAvailable()) {
            orderRepository.syncAllOrders()
        } else {
            Result.failure(Exception("No internet connection"))
        }
    }
    
    suspend fun syncDeliveriesOnly(): Result<Boolean> {
        return if (isInternetAvailable()) {
            deliveryRepository.syncAllDeliveries()
        } else {
            Result.failure(Exception("No internet connection"))
        }
    }
    
    suspend fun syncSellsOnly(): Result<Boolean> {
        return if (isInternetAvailable()) {
            sellsRepository.syncAllSells()
        } else {
            Result.failure(Exception("No internet connection"))
        }
    }
    
    suspend fun syncDueAccountsOnly(): Result<Boolean> {
        return if (isInternetAvailable()) {
            dueAccountRepository.syncAllDueAccounts()
        } else {
            Result.failure(Exception("No internet connection"))
        }
    }
}

// **DATA CLASSES**

data class SyncStatusInfo(
    val status: BackgroundSyncManager.SyncStatus,
    val lastSyncTime: Date?,
    val pendingSyncCount: Int,
    val isInternetAvailable: Boolean,
    val isUserAuthenticated: Boolean
)
