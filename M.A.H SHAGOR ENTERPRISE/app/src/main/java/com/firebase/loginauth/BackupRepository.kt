package com.firebase.loginauth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

data class BackupEntry(
    val id: String = "",
    val userId: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val description: String = "",
    val dataSize: Long = 0,
    val backupType: String = "manual", // manual, automatic
    val status: String = "completed" // completed, failed, in_progress
)

data class BackupData(
    val customers: List<Customer> = emptyList(),
    val orders: List<Order> = emptyList(),
    val cylinderStock: List<CylinderStock> = emptyList(),
    val stockTransactions: List<StockTransaction> = emptyList(),
    val expenses: List<ExpenseEntry> = emptyList(),
    val cashDeposits: List<CashDepositEntry> = emptyList(),
    val recentActivities: List<RecentActivityEntry> = emptyList()
)

class BackupRepository(private val context: Context) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _backupEntries = MutableStateFlow<List<BackupEntry>>(emptyList())
    val backupEntries: StateFlow<List<BackupEntry>> = _backupEntries.asStateFlow()
    
    companion object {
        private const val TAG = "BackupRepository"
        private const val BACKUPS_COLLECTION = "backups"
        private const val BACKUP_DATA_COLLECTION = "backup_data"
    }
    
    private fun getCurrentUserId(): String {
        val user = auth.currentUser
        if (user == null) {
            throw IllegalStateException("User not authenticated")
        }
        return user.uid
    }
    
    private fun getBackupsCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection(BACKUPS_COLLECTION)
    
    private fun getBackupDataCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection(BACKUP_DATA_COLLECTION)
    
    /**
     * Load available backups for the current user
     */
    suspend fun loadBackups() {
        try {
            _isLoading.value = true
            
            val snapshot = getBackupsCollection()
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val backups = snapshot.documents.mapNotNull { doc ->
                doc.toObject(BackupEntry::class.java)?.copy(id = doc.id)
            }
            
            _backupEntries.value = backups
            Log.d(TAG, "Loaded ${backups.size} backup entries")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading backups: ${e.message}")
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Create a new backup of all user data
     */
    suspend fun createBackup(description: String = "Manual backup"): String {
        try {
            _isLoading.value = true
            
            val userId = getCurrentUserId()
            val backupId = UUID.randomUUID().toString()
            
            // Collect all user data
            val backupData = collectAllUserData()
            
            // Calculate data size (approximate)
            val dataSize = calculateDataSize(backupData)
            
            // Create backup entry
            val backupEntry = BackupEntry(
                id = backupId,
                userId = userId,
                timestamp = Timestamp.now(),
                description = description,
                dataSize = dataSize,
                backupType = "manual",
                status = "completed"
            )
            
            // Save backup data
            getBackupDataCollection()
                .document(backupId)
                .set(backupData)
                .await()
            
            // Save backup entry
            getBackupsCollection()
                .document(backupId)
                .set(backupEntry)
                .await()
            
            // Refresh backup list
            loadBackups()
            
            Log.d(TAG, "Backup created successfully: $backupId")
            return backupId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup: ${e.message}")
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Restore data from a specific backup
     */
    suspend fun restoreBackup(backupId: String) {
        try {
            _isLoading.value = true
            
            // Get backup data
            val backupDataDoc = getBackupDataCollection()
                .document(backupId)
                .get()
                .await()
            
            if (!backupDataDoc.exists()) {
                throw Exception("Backup data not found")
            }
            
            val backupData = backupDataDoc.toObject(BackupData::class.java)
                ?: throw Exception("Failed to parse backup data")
            
            // Restore all data collections
            restoreAllUserData(backupData)
            
            Log.d(TAG, "Backup restored successfully: $backupId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup: ${e.message}")
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    /**
     * Delete a backup
     */
    suspend fun deleteBackup(backupId: String) {
        try {
            _isLoading.value = true
            
            // Delete backup data
            getBackupDataCollection()
                .document(backupId)
                .delete()
                .await()
            
            // Delete backup entry
            getBackupsCollection()
                .document(backupId)
                .delete()
                .await()
            
            // Refresh backup list
            loadBackups()
            
            Log.d(TAG, "Backup deleted successfully: $backupId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting backup: ${e.message}")
            throw e
        } finally {
            _isLoading.value = false
        }
    }
    
    private suspend fun collectAllUserData(): BackupData {
        val userId = getCurrentUserId()
        val userDocRef = firestore.collection("users").document(userId)
        
        // Collect data from all collections
        val customers = userDocRef.collection("customers").get().await()
            .documents.mapNotNull { it.toObject(Customer::class.java) }
        
        val orders = userDocRef.collection("orders").get().await()
            .documents.mapNotNull { it.toObject(Order::class.java) }
        
        val cylinderStock = userDocRef.collection("cylinder_stock").get().await()
            .documents.mapNotNull { it.toObject(CylinderStock::class.java) }
        
        val stockTransactions = userDocRef.collection("stock_transactions").get().await()
            .documents.mapNotNull { it.toObject(StockTransaction::class.java) }
        
        val expenses = userDocRef.collection("expenses").get().await()
            .documents.mapNotNull { it.toObject(ExpenseEntry::class.java) }
        
        val cashDeposits = userDocRef.collection("cash_deposits").get().await()
            .documents.mapNotNull { it.toObject(CashDepositEntry::class.java) }
        
        val recentActivities = userDocRef.collection("recent_activities").get().await()
            .documents.mapNotNull { it.toObject(RecentActivityEntry::class.java) }
        
        return BackupData(
            customers = customers,
            orders = orders,
            cylinderStock = cylinderStock,
            stockTransactions = stockTransactions,
            expenses = expenses,
            cashDeposits = cashDeposits,
            recentActivities = recentActivities
        )
    }
    
    private suspend fun restoreAllUserData(backupData: BackupData) {
        val userId = getCurrentUserId()
        val userDocRef = firestore.collection("users").document(userId)
        val batch = firestore.batch()
        
        // Clear existing data and restore from backup
        
        // Restore customers
        backupData.customers.forEach { customer ->
            val docRef = userDocRef.collection("customers").document(customer.id)
            batch.set(docRef, customer)
        }
        
        // Restore orders
        backupData.orders.forEach { order ->
            val docRef = userDocRef.collection("orders").document(order.id)
            batch.set(docRef, order)
        }
        
        // Restore cylinder stock
        backupData.cylinderStock.forEach { stock ->
            val docRef = userDocRef.collection("cylinder_stock").document(stock.id)
            batch.set(docRef, stock)
        }
        
        // Restore stock transactions
        backupData.stockTransactions.forEach { transaction ->
            val docRef = userDocRef.collection("stock_transactions").document(transaction.id)
            batch.set(docRef, transaction)
        }
        
        // Restore expenses
        backupData.expenses.forEach { expense ->
            val docRef = userDocRef.collection("expenses").document(expense.id)
            batch.set(docRef, expense)
        }
        
        // Restore cash deposits
        backupData.cashDeposits.forEach { deposit ->
            val docRef = userDocRef.collection("cash_deposits").document(deposit.id)
            batch.set(docRef, deposit)
        }
        
        // Restore recent activities
        backupData.recentActivities.forEach { activity ->
            val docRef = userDocRef.collection("recent_activities").document(activity.id)
            batch.set(docRef, activity)
        }
        
        // Commit all changes
        batch.commit().await()
    }
    
    private fun calculateDataSize(backupData: BackupData): Long {
        // Approximate calculation based on number of records
        return (backupData.customers.size +
                backupData.orders.size +
                backupData.cylinderStock.size +
                backupData.stockTransactions.size +
                backupData.expenses.size +
                backupData.cashDeposits.size +
                backupData.recentActivities.size).toLong() * 1024 // Approximate 1KB per record
    }
}
