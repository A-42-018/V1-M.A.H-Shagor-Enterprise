package com.firebase.loginauth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

data class SyncStatus(
    val isLoading: Boolean = false,
    val lastSyncTime: Long? = null,
    val error: String? = null,
    val successMessage: String? = null,
    val syncedCollections: List<String> = emptyList()
)

class DataSyncService(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val _syncStatus = MutableStateFlow(SyncStatus())
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()
    
    companion object {
        private const val TAG = "DataSyncService"
        private const val SYNC_PREFS = "data_sync_prefs"
        private const val LAST_SYNC_KEY = "last_sync_time"
    }
    
    private fun getCurrentUserId(): String {
        val user = auth.currentUser
        if (user == null) {
            throw IllegalStateException("User not authenticated")
        }
        return user.uid
    }
    
    private fun getUserSpecificKey(baseKey: String): String {
        val user = auth.currentUser
        return if (user != null) {
            "${baseKey}_${user.uid}"
        } else {
            baseKey
        }
    }
    
    /**
     * Perform a complete data sync from Firebase to local storage
     */
    suspend fun performFullSync(): Boolean {
        return try {
            _syncStatus.value = _syncStatus.value.copy(
                isLoading = true,
                error = null,
                successMessage = null,
                syncedCollections = emptyList()
            )
            
            val userId = getCurrentUserId()
            val userDocRef = firestore.collection("users").document(userId)
            val syncedCollections = mutableListOf<String>()
            
            // Sync Customers
            try {
                val customerRepository = CustomerRepository(context)
                customerRepository.forceSync()
                syncedCollections.add("Customers")
                Log.d(TAG, "Customers synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing customers: ${e.message}")
            }
            
            // Sync Orders
            try {
                val orderRepository = OrderRepository.getInstance(context)
                orderRepository.forceSync()
                syncedCollections.add("Orders")
                Log.d(TAG, "Orders synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing orders: ${e.message}")
            }
            
            // Sync Cylinder Stock
            try {
                val stockRepository = CylinderStockRepository(context)
                stockRepository.forceSync()
                syncedCollections.add("Stock Management")
                Log.d(TAG, "Stock synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing stock: ${e.message}")
            }
            
            // Sync Expenses
            try {
                val expenseRepository = ExpenseRepository(context)
                expenseRepository.forceSync()
                syncedCollections.add("Expenses")
                Log.d(TAG, "Expenses synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing expenses: ${e.message}")
            }
            
            // Sync Cash Deposits
            try {
                val cashDepositRepository = CashDepositRepository(context)
                cashDepositRepository.forceSync()
                syncedCollections.add("Cash Deposits")
                Log.d(TAG, "Cash deposits synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing cash deposits: ${e.message}")
            }
            
            // Sync Recent Activities
            try {
                val activityRepository = RecentActivityRepository.getInstance(context)
                activityRepository.forceSync()
                syncedCollections.add("Recent Activities")
                Log.d(TAG, "Recent activities synced successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing recent activities: ${e.message}")
            }
            
            // Update sync status
            val currentTime = System.currentTimeMillis()
            saveLastSyncTime(currentTime)
            
            _syncStatus.value = _syncStatus.value.copy(
                isLoading = false,
                lastSyncTime = currentTime,
                successMessage = "Data sync completed successfully. Synced: ${syncedCollections.joinToString(", ")}",
                syncedCollections = syncedCollections
            )
            
            Log.d(TAG, "Full sync completed successfully")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during full sync: ${e.message}")
            _syncStatus.value = _syncStatus.value.copy(
                isLoading = false,
                error = "Sync failed: ${e.message}"
            )
            false
        }
    }
    
    /**
     * Force sync all repositories
     */
    fun startFullSync() {
        coroutineScope.launch {
            performFullSync()
        }
    }
    
    /**
     * Get last sync time
     */
    fun getLastSyncTime(): Long? {
        val prefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        val lastSync = prefs.getLong(getUserSpecificKey(LAST_SYNC_KEY), 0L)
        return if (lastSync > 0) lastSync else null
    }
    
    /**
     * Save last sync time
     */
    private fun saveLastSyncTime(time: Long) {
        val prefs = context.getSharedPreferences(SYNC_PREFS, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(getUserSpecificKey(LAST_SYNC_KEY), time)
            .apply()
    }
    
    /**
     * Clear sync status messages
     */
    fun clearMessages() {
        _syncStatus.value = _syncStatus.value.copy(
            error = null,
            successMessage = null
        )
    }
    
    /**
     * Check if sync is needed (based on time)
     */
    fun isSyncNeeded(): Boolean {
        val lastSync = getLastSyncTime()
        if (lastSync == null) return true
        
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastSync
        val oneHour = 60 * 60 * 1000L // 1 hour in milliseconds
        
        return timeDiff > oneHour
    }
    
    /**
     * Get formatted last sync time
     */
    fun getFormattedLastSyncTime(): String {
        val lastSync = getLastSyncTime()
        return if (lastSync != null) {
            val date = Date(lastSync)
            java.text.SimpleDateFormat("MMM dd, yyyy hh:mm a", java.util.Locale.getDefault()).format(date)
        } else {
            "Never"
        }
    }
}

// Extension functions to add forceSync to repositories
suspend fun CustomerRepository.forceSync() {
    // Force sync customers from Firebase
    syncWithFirestore()
}

suspend fun OrderRepository.forceSync() {
    // Force sync orders from Firebase
    syncWithFirestore()
}

suspend fun CylinderStockRepository.forceSync() {
    // Force sync stock from Firebase
    forceFirestoreSync()
}

suspend fun ExpenseRepository.forceSync() {
    // Force sync expenses from Firebase
    syncWithFirestore()
}

suspend fun CashDepositRepository.forceSync() {
    // Force sync cash deposits from Firebase
    syncWithFirestore()
}

suspend fun RecentActivityRepository.forceSync() {
    // Force sync recent activities from Firebase
    syncWithFirestore()
}
