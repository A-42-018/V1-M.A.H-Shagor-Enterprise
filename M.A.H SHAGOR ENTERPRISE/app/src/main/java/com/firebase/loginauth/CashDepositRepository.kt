package com.firebase.loginauth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class CashDepositRepository(context: Context) {
    // Local storage components
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("cash_deposit_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Firebase components
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // StateFlow for reactive UI updates
    private val _cashDeposits = MutableStateFlow<List<CashDepositEntry>>(emptyList())
    val cashDeposits: StateFlow<List<CashDepositEntry>> = _cashDeposits.asStateFlow()
    
    companion object {
        private const val TAG = "CashDepositRepository"
    }
    
    init {
        // Load user-specific data if authenticated, otherwise load empty state
        if (auth.currentUser != null) {
            coroutineScope.launch {
                // Prioritize Firebase data over local storage for user isolation
                syncWithFirestore()
            }
        } else {
            _cashDeposits.value = emptyList()
        }
    }
    
    /**
     * Get user-specific SharedPreferences key
     */
    private fun getUserSpecificKey(baseKey: String): String {
        val userId = auth.currentUser?.uid ?: "anonymous"
        return "${baseKey}_${userId}"
    }
    
    private fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    private fun getUserCashDepositCollection() = getCurrentUserId()?.let { userId ->
        firestore.collection("users").document(userId).collection("cash_deposits")
    }
    
    // ==================== LOCAL STORAGE METHODS ====================
    
    /**
     * Load cash deposits from local storage
     */
    private fun loadLocalData() {
        try {
            val depositsJson = sharedPreferences.getString(getUserSpecificKey("cash_deposits_list"), "[]")
            val depositsType = object : TypeToken<List<CashDepositEntry>>() {}.type
            val deposits: List<CashDepositEntry> = gson.fromJson(depositsJson, depositsType) ?: emptyList()
            _cashDeposits.value = deposits.sortedByDescending { it.createdAt }
            
            Log.d(TAG, "Loaded ${deposits.size} cash deposits from user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local cash deposits", e)
            _cashDeposits.value = emptyList()
        }
    }
    
    /**
     * Save cash deposits to local storage
     */
    private fun saveLocalData() {
        try {
            val depositsJson = gson.toJson(_cashDeposits.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey("cash_deposits_list"), depositsJson)
                .apply()
            Log.d(TAG, "Saved ${_cashDeposits.value.size} cash deposits to user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cash deposits locally", e)
        }
    }
    
    // Save cash deposit data (saves to both local and cloud)
    suspend fun saveCashDepositData(cylinderCount: String, amount: String): Result<String> {
        return try {
            val depositId = UUID.randomUUID().toString()
            val currentTime = System.currentTimeMillis()
            
            // Create cash deposit entry
            val depositEntry = CashDepositEntry(
                id = depositId,
                cylinderCount = cylinderCount,
                amount = amount,
                createdAt = currentTime,
                updatedAt = currentTime,
                date = getCurrentDateString(),
                time = getCurrentTimeString()
            )
            
            // Save to local storage first
            val updatedDeposits = _cashDeposits.value.toMutableList()
            updatedDeposits.add(0, depositEntry) // Add to beginning for latest first
            _cashDeposits.value = updatedDeposits
            saveLocalData()
            
            Log.d(TAG, "Cash deposit saved locally: $depositId, Cylinders: $cylinderCount, Amount: $amount")
            
            // Save to Firestore asynchronously
            saveToFirestore(depositEntry)
            
            Result.success(depositId)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving cash deposit data: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // Update existing cash deposit data (for auto-save) - updates both local and cloud
    suspend fun updateCashDepositData(depositId: String, cylinderCount: String, amount: String): Result<String> {
        return try {
            // Update local storage first
            val updatedDeposits = _cashDeposits.value.toMutableList()
            val index = updatedDeposits.indexOfFirst { it.id == depositId }
            
            if (index != -1) {
                val updatedEntry = updatedDeposits[index].copy(
                    cylinderCount = cylinderCount,
                    amount = amount,
                    updatedAt = System.currentTimeMillis(),
                    time = getCurrentTimeString()
                )
                updatedDeposits[index] = updatedEntry
                _cashDeposits.value = updatedDeposits
                saveLocalData()
                
                Log.d(TAG, "Cash deposit updated locally: $depositId, Cylinders: $cylinderCount, Amount: $amount")
                
                // Update in Firestore asynchronously
                updateInFirestore(updatedEntry)
                
                Result.success(depositId)
            } else {
                Log.w(TAG, "Cash deposit not found for update: $depositId")
                Result.failure(Exception("Cash deposit not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cash deposit data: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    private fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
    
    private fun getCurrentTimeString(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
    
    // ==================== FIRESTORE METHODS ====================
    
    /**
     * Save cash deposit entry to Firestore
     */
    private fun saveToFirestore(depositEntry: CashDepositEntry) {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val cashDepositCollection = getUserCashDepositCollection() ?: return@launch
                
                val depositData = hashMapOf(
                    "id" to depositEntry.id,
                    "cylinderCount" to depositEntry.cylinderCount,
                    "amount" to depositEntry.amount,
                    "createdAt" to depositEntry.createdAt,
                    "updatedAt" to depositEntry.updatedAt,
                    "userId" to userId,
                    "date" to depositEntry.date,
                    "time" to depositEntry.time
                )
                
                cashDepositCollection.document(depositEntry.id).set(depositData).await()
                Log.d(TAG, "Cash deposit saved to Firestore: ${depositEntry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving cash deposit to Firestore: ${e.message}", e)
            }
        }
    }
    
    /**
     * Update cash deposit entry in Firestore
     */
    private fun updateInFirestore(depositEntry: CashDepositEntry) {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val cashDepositCollection = getUserCashDepositCollection() ?: return@launch
                
                val updateData = hashMapOf(
                    "cylinderCount" to depositEntry.cylinderCount,
                    "amount" to depositEntry.amount,
                    "updatedAt" to depositEntry.updatedAt,
                    "time" to depositEntry.time
                )
                
                cashDepositCollection.document(depositEntry.id).update(updateData as Map<String, Any>).await()
                Log.d(TAG, "Cash deposit updated in Firestore: ${depositEntry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating cash deposit in Firestore: ${e.message}", e)
            }
        }
    }
    
    /**
     * Sync with Firestore - load cloud data and merge with local
     */
    internal fun syncWithFirestore() {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val cashDepositCollection = getUserCashDepositCollection() ?: return@launch
                
                val snapshot = cashDepositCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val cloudDeposits = snapshot.documents.mapNotNull { doc ->
                    try {
                        CashDepositEntry(
                            id = doc.getString("id") ?: "",
                            cylinderCount = doc.getString("cylinderCount") ?: "",
                            amount = doc.getString("amount") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L,
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing cash deposit document: ${doc.id}", e)
                        null
                    }
                }
                
                // Merge cloud data with local data (prioritize local changes)
                val localDeposits = _cashDeposits.value
                val mergedDeposits = mergeDeposits(localDeposits, cloudDeposits)
                
                _cashDeposits.value = mergedDeposits.sortedByDescending { it.createdAt }
                saveLocalData()
                
                Log.d(TAG, "Synced ${cloudDeposits.size} cash deposits from Firestore, merged to ${mergedDeposits.size} total")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing with Firestore", e)
            }
        }
    }
    
    /**
     * Merge local and cloud cash deposits (prioritize local changes)
     */
    private fun mergeDeposits(localDeposits: List<CashDepositEntry>, cloudDeposits: List<CashDepositEntry>): List<CashDepositEntry> {
        val mergedMap = mutableMapOf<String, CashDepositEntry>()
        
        // Add cloud deposits first
        cloudDeposits.forEach { deposit ->
            mergedMap[deposit.id] = deposit
        }
        
        // Override with local deposits (prioritize local changes)
        localDeposits.forEach { deposit ->
            val existing = mergedMap[deposit.id]
            if (existing == null || deposit.updatedAt >= existing.updatedAt) {
                mergedMap[deposit.id] = deposit
            }
        }
        
        return mergedMap.values.toList()
    }
    
    /**
     * Upload all local cash deposits to Firestore
     */
    fun uploadAllToFirestore() {
        coroutineScope.launch {
            try {
                val localDeposits = _cashDeposits.value
                Log.d(TAG, "Uploading ${localDeposits.size} cash deposits to Firestore")
                
                localDeposits.forEach { deposit ->
                    saveToFirestore(deposit)
                }
                
                Log.d(TAG, "All cash deposits uploaded to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading cash deposits to Firestore", e)
            }
        }
    }
    
    /**
     * Delete cash deposit entry
     */
    suspend fun deleteCashDeposit(depositId: String): Result<String> {
        return try {
            // Remove from local storage first
            val updatedDeposits = _cashDeposits.value.toMutableList()
            val index = updatedDeposits.indexOfFirst { it.id == depositId }
            
            if (index != -1) {
                updatedDeposits.removeAt(index)
                _cashDeposits.value = updatedDeposits
                saveLocalData()
                
                Log.d(TAG, "Cash deposit deleted locally: $depositId")
                
                // Delete from Firestore asynchronously
                deleteFromFirestore(depositId)
                
                Result.success(depositId)
            } else {
                Log.w(TAG, "Cash deposit not found for deletion: $depositId")
                Result.failure(Exception("Cash deposit not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cash deposit: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete cash deposit from Firestore
     */
    private fun deleteFromFirestore(depositId: String) {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val cashDepositCollection = getUserCashDepositCollection() ?: return@launch
                
                cashDepositCollection.document(depositId).delete().await()
                Log.d(TAG, "Cash deposit deleted from Firestore: $depositId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting cash deposit from Firestore: ${e.message}", e)
            }
        }
    }
    
    /**
     * Get total cash deposit amount
     */
    fun getTotalCashAmount(): Double {
        return try {
            _cashDeposits.value.sumOf { 
                it.amount.toDoubleOrNull() ?: 0.0 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total cash amount: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Force sync with cloud data
     */
    fun syncWithCloud() {
        syncWithFirestore()
    }
    
    // Get all cash deposit entries for current user
    fun getAllCashDeposits(): Flow<List<CashDepositEntry>> = callbackFlow {
        val userId = getCurrentUserId()
        if (userId == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val cashDepositCollection = getUserCashDepositCollection()
        if (cashDepositCollection == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        
        val listener = cashDepositCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CashDepositRepository", "Error listening to cash deposits: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }
                
                val deposits = snapshot?.documents?.mapNotNull { document ->
                    try {
                        CashDepositEntry(
                            id = document.getString("id") ?: "",
                            cylinderCount = document.getString("cylinderCount") ?: "",
                            amount = document.getString("amount") ?: "",
                            createdAt = document.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = document.getLong("updatedAt") ?: System.currentTimeMillis(),
                            date = document.getString("date") ?: "",
                            time = document.getString("time") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e("CashDepositRepository", "Error parsing cash deposit entry: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                
                trySend(deposits)
            }
        
        awaitClose { listener.remove() }
    }
}

// Data class for cash deposit entries
data class CashDepositEntry(
    val id: String = "",
    val cylinderCount: String = "",
    val amount: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val date: String = "",
    val time: String = ""
)
