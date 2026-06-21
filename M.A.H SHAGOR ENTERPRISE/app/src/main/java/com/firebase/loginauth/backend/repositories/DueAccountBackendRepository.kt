package com.firebase.loginauth.backend.repositories

import android.content.Context
import com.firebase.loginauth.database.AppDatabase
import com.firebase.loginauth.database.entities.DueAccountEntity
import com.firebase.loginauth.database.entities.SellEntity
import com.firebase.loginauth.database.entities.SyncQueueEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DueAccountBackendRepository @Inject constructor(
    private val context: Context,
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    private val database = AppDatabase.getDatabase(context)
    private val dueAccountDao = database.dueAccountDao()
    private val syncQueueDao = database.syncQueueDao()
    private val gson = Gson()
    
    private fun getCurrentUserId(): String = auth.currentUser?.uid ?: ""
    
    private fun getUserDueAccountCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("due_accounts")
    
    // **AUTO-CREATE DUE ACCOUNT FROM SELL**
    
    suspend fun createDueAccountFromSell(sell: SellEntity): Result<DueAccountEntity> {
        return try {
            if (sell.dueAmount <= 0) {
                return Result.failure(Exception("No due amount for this sell"))
            }
            
            val userId = getCurrentUserId()
            val currentTime = Date()
            
            // Calculate due date (30 days from sale date)
            val dueDate = Calendar.getInstance().apply {
                time = sell.saleDate
                add(Calendar.DAY_OF_MONTH, 30)
            }.time
            
            val dueAccount = DueAccountEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                customerName = sell.customerName,
                customerPhone = sell.customerPhone,
                totalDueAmount = sell.dueAmount,
                paidAmount = 0.0,
                remainingAmount = sell.dueAmount,
                dueDate = dueDate,
                originalOrderId = sell.orderId,
                status = "Pending",
                priority = determinePriority(sell.dueAmount),
                notes = "Auto-created from sell record",
                needsSync = true,
                isSynced = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Save to local database
            dueAccountDao.insertDueAccount(dueAccount)
            
            // Add to sync queue
            addToSyncQueue(dueAccount, "CREATE")
            
            // Try immediate sync
            trySyncDueAccount(dueAccount)
            
            Result.success(dueAccount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **MANUAL DUE ACCOUNT CREATION**
    
    suspend fun createDueAccount(
        customerName: String,
        customerPhone: String,
        customerAddress: String = "",
        dueAmount: Double,
        dueDate: Date,
        notes: String = "",
        priority: String = "Medium"
    ): Result<DueAccountEntity> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            
            val dueAccount = DueAccountEntity(
                id = UUID.randomUUID().toString(),
                userId = userId,
                customerName = customerName,
                customerPhone = customerPhone,
                customerAddress = customerAddress,
                totalDueAmount = dueAmount,
                paidAmount = 0.0,
                remainingAmount = dueAmount,
                dueDate = dueDate,
                status = "Pending",
                priority = priority,
                notes = notes,
                needsSync = true,
                isSynced = false,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            
            // Save to local database
            dueAccountDao.insertDueAccount(dueAccount)
            
            // Add to sync queue
            addToSyncQueue(dueAccount, "CREATE")
            
            // Try immediate sync
            trySyncDueAccount(dueAccount)
            
            Result.success(dueAccount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **PAYMENT PROCESSING**
    
    suspend fun makePayment(
        dueAccountId: String,
        paymentAmount: Double,
        paymentMethod: String = "Cash",
        notes: String = ""
    ): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val dueAccount = dueAccountDao.getDueAccountById(userId, dueAccountId)
                ?: return Result.failure(Exception("Due account not found"))
            
            if (paymentAmount <= 0) {
                return Result.failure(Exception("Payment amount must be greater than 0"))
            }
            
            if (paymentAmount > dueAccount.remainingAmount) {
                return Result.failure(Exception("Payment amount cannot exceed remaining due amount"))
            }
            
            val currentTime = Date()
            val newPaidAmount = dueAccount.paidAmount + paymentAmount
            val newRemainingAmount = dueAccount.totalDueAmount - newPaidAmount
            
            val newStatus = when {
                newRemainingAmount <= 0 -> "Cleared"
                newPaidAmount > 0 -> "Partial"
                else -> "Pending"
            }
            
            // Create payment history entry
            val paymentRecord = PaymentRecord(
                amount = paymentAmount,
                method = paymentMethod,
                date = currentTime,
                notes = notes
            )
            
            val updatedPaymentHistory = dueAccount.paymentHistory.toMutableList()
            updatedPaymentHistory.add(gson.toJson(paymentRecord))
            
            // Update local database
            dueAccountDao.updatePayment(
                userId = userId,
                dueId = dueAccountId,
                paidAmount = newPaidAmount,
                remainingAmount = newRemainingAmount,
                status = newStatus,
                lastPaymentDate = currentTime.time,
                updatedAt = currentTime.time
            )
            
            // Get updated due account for sync
            val updatedDueAccount = dueAccount.copy(
                paidAmount = newPaidAmount,
                remainingAmount = newRemainingAmount,
                status = newStatus,
                paymentHistory = updatedPaymentHistory,
                lastPaymentDate = currentTime,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Add to sync queue
            addToSyncQueue(updatedDueAccount, "UPDATE")
            
            // Try immediate sync
            trySyncDueAccount(updatedDueAccount)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **PARTIAL PAYMENT CLEARANCE**
    
    suspend fun clearPartialDue(dueAccountId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val dueAccount = dueAccountDao.getDueAccountById(userId, dueAccountId)
                ?: return Result.failure(Exception("Due account not found"))
            
            // Clear the remaining amount
            makePayment(
                dueAccountId = dueAccountId,
                paymentAmount = dueAccount.remainingAmount,
                paymentMethod = "Cash",
                notes = "Full clearance payment"
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DUE ACCOUNT STATUS MANAGEMENT**
    
    suspend fun updateDueAccountStatus(dueAccountId: String, newStatus: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val dueAccount = dueAccountDao.getDueAccountById(userId, dueAccountId)
                ?: return Result.failure(Exception("Due account not found"))
            
            val currentTime = Date()
            val updatedDueAccount = dueAccount.copy(
                status = newStatus,
                updatedAt = currentTime,
                needsSync = true,
                isSynced = false
            )
            
            // Update local database
            dueAccountDao.updateDueAccount(updatedDueAccount)
            
            // Add to sync queue
            addToSyncQueue(updatedDueAccount, "UPDATE")
            
            // Try immediate sync
            trySyncDueAccount(updatedDueAccount)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **OVERDUE MANAGEMENT**
    
    suspend fun markOverdueDueAccounts(): Result<Int> {
        return try {
            val userId = getCurrentUserId()
            val currentTime = Date()
            val overdueDueAccounts = dueAccountDao.getOverdueDueAccounts(userId, currentTime.time)
            
            var updatedCount = 0
            overdueDueAccounts.collect { dueAccounts ->
                for (dueAccount in dueAccounts) {
                    if (dueAccount.status != "Overdue" && dueAccount.status != "Cleared") {
                        updateDueAccountStatus(dueAccount.id, "Overdue")
                        updatedCount++
                    }
                }
            }
            
            Result.success(updatedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **FIREBASE SYNC OPERATIONS**
    
    private suspend fun trySyncDueAccount(dueAccount: DueAccountEntity) {
        try {
            val dueAccountData = mapOf(
                "id" to dueAccount.id,
                "userId" to dueAccount.userId,
                "customerName" to dueAccount.customerName,
                "customerPhone" to dueAccount.customerPhone,
                "customerAddress" to dueAccount.customerAddress,
                "totalDueAmount" to dueAccount.totalDueAmount,
                "paidAmount" to dueAccount.paidAmount,
                "remainingAmount" to dueAccount.remainingAmount,
                "dueDate" to dueAccount.dueDate.time,
                "originalOrderId" to dueAccount.originalOrderId,
                "paymentHistory" to dueAccount.paymentHistory,
                "status" to dueAccount.status,
                "priority" to dueAccount.priority,
                "notes" to dueAccount.notes,
                "lastPaymentDate" to dueAccount.lastPaymentDate?.time,
                "reminderDate" to dueAccount.reminderDate?.time,
                "createdAt" to dueAccount.createdAt.time,
                "updatedAt" to dueAccount.updatedAt.time
            )
            
            getUserDueAccountCollection()
                .document(dueAccount.id)
                .set(dueAccountData)
                .await()
            
            // Mark as synced in local database
            dueAccountDao.updateSyncStatus(dueAccount.userId, dueAccount.id, isSynced = true, needsSync = false)
            
            // Remove from sync queue
            removeSyncQueueItem("due_account", dueAccount.id)
            
        } catch (e: Exception) {
            println("Due account sync failed: ${e.message}")
        }
    }
    
    suspend fun syncAllDueAccounts(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val unsyncedDueAccounts = dueAccountDao.getUnsyncedDueAccounts(userId)
            
            for (dueAccount in unsyncedDueAccounts) {
                trySyncDueAccount(dueAccount)
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun syncFromFirebase(): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val snapshot = getUserDueAccountCollection().get().await()
            
            val firebaseDueAccounts = snapshot.documents.mapNotNull { doc ->
                try {
                    val paymentHistory = doc.get("paymentHistory") as? List<String> ?: emptyList()
                    
                    DueAccountEntity(
                        id = doc.getString("id") ?: "",
                        userId = doc.getString("userId") ?: "",
                        customerName = doc.getString("customerName") ?: "",
                        customerPhone = doc.getString("customerPhone") ?: "",
                        customerAddress = doc.getString("customerAddress") ?: "",
                        totalDueAmount = doc.getDouble("totalDueAmount") ?: 0.0,
                        paidAmount = doc.getDouble("paidAmount") ?: 0.0,
                        remainingAmount = doc.getDouble("remainingAmount") ?: 0.0,
                        dueDate = Date(doc.getLong("dueDate") ?: 0),
                        originalOrderId = doc.getString("originalOrderId") ?: "",
                        paymentHistory = paymentHistory,
                        status = doc.getString("status") ?: "Pending",
                        priority = doc.getString("priority") ?: "Medium",
                        notes = doc.getString("notes") ?: "",
                        lastPaymentDate = doc.getLong("lastPaymentDate")?.let { Date(it) },
                        reminderDate = doc.getLong("reminderDate")?.let { Date(it) },
                        isSynced = true,
                        needsSync = false,
                        createdAt = Date(doc.getLong("createdAt") ?: 0),
                        updatedAt = Date(doc.getLong("updatedAt") ?: 0)
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // Insert/update local database with Firebase data
            dueAccountDao.insertDueAccounts(firebaseDueAccounts)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // **DATA ACCESS METHODS**
    
    fun getAllDueAccounts(): Flow<List<DueAccountEntity>> {
        return dueAccountDao.getAllDueAccounts(getCurrentUserId())
    }
    
    fun getActiveDueAccounts(): Flow<List<DueAccountEntity>> {
        return dueAccountDao.getActiveDueAccounts(getCurrentUserId())
    }
    
    fun getDueAccountsByStatus(status: String): Flow<List<DueAccountEntity>> {
        return dueAccountDao.getDueAccountsByStatus(getCurrentUserId(), status)
    }
    
    fun getDueAccountsByCustomer(customerPhone: String): Flow<List<DueAccountEntity>> {
        return dueAccountDao.getDueAccountsByCustomer(getCurrentUserId(), customerPhone)
    }
    
    fun getOverdueDueAccounts(): Flow<List<DueAccountEntity>> {
        val currentTime = Date()
        return dueAccountDao.getOverdueDueAccounts(getCurrentUserId(), currentTime.time)
    }
    
    suspend fun getDueAccountById(dueAccountId: String): DueAccountEntity? {
        return dueAccountDao.getDueAccountById(getCurrentUserId(), dueAccountId)
    }
    
    suspend fun getTotalDueAmount(): Double {
        return dueAccountDao.getTotalDueAmount(getCurrentUserId()) ?: 0.0
    }
    
    suspend fun getActiveDueCount(): Int {
        return dueAccountDao.getActiveDueCount(getCurrentUserId())
    }
    
    suspend fun getOverdueCount(): Int {
        val currentTime = Date()
        return dueAccountDao.getOverdueCount(getCurrentUserId(), currentTime.time)
    }
    
    // **DUE ACCOUNT DELETION**
    
    suspend fun deleteDueAccount(dueAccountId: String): Result<Boolean> {
        return try {
            val userId = getCurrentUserId()
            val dueAccount = dueAccountDao.getDueAccountById(userId, dueAccountId)
                ?: return Result.failure(Exception("Due account not found"))
            
            // Delete from local database
            dueAccountDao.deleteDueAccount(dueAccount)
            
            // Add to sync queue for Firebase deletion
            addToSyncQueue(dueAccount, "DELETE")
            
            // Try immediate sync
            trySyncDueAccountDeletion(dueAccountId)
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun trySyncDueAccountDeletion(dueAccountId: String) {
        try {
            getUserDueAccountCollection()
                .document(dueAccountId)
                .delete()
                .await()
            
            // Remove from sync queue
            removeSyncQueueItem("due_account", dueAccountId)
            
        } catch (e: Exception) {
            println("Due account deletion sync failed: ${e.message}")
        }
    }
    
    // **HELPER METHODS**
    
    private fun determinePriority(dueAmount: Double): String {
        return when {
            dueAmount >= 10000 -> "Critical"
            dueAmount >= 5000 -> "High"
            dueAmount >= 1000 -> "Medium"
            else -> "Low"
        }
    }
    
    // **SYNC QUEUE MANAGEMENT**
    
    private suspend fun addToSyncQueue(dueAccount: DueAccountEntity, operation: String) {
        val syncItem = SyncQueueEntity(
            id = UUID.randomUUID().toString(),
            userId = dueAccount.userId,
            entityType = "due_account",
            entityId = dueAccount.id,
            operation = operation,
            data = gson.toJson(dueAccount),
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
}

// **DATA CLASS FOR PAYMENT RECORDS**
data class PaymentRecord(
    val amount: Double,
    val method: String,
    val date: Date,
    val notes: String
)
