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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class ExpenseRepository(context: Context) {
    // Local storage components
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("expense_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Firebase components
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // StateFlow for reactive UI updates
    private val _expenses = MutableStateFlow<List<ExpenseEntry>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntry>> = _expenses.asStateFlow()
    
    private val _expenseSummary = MutableStateFlow(ExpenseSummary())
    val expenseSummary: StateFlow<ExpenseSummary> = _expenseSummary.asStateFlow()
    
    companion object {
        private const val TAG = "ExpenseRepository"
    }
    
    init {
        // Load user-specific data if authenticated, otherwise load empty state
        if (auth.currentUser != null) {
            coroutineScope.launch {
                // Prioritize Firebase data over local storage for user isolation
                syncWithFirestore()
            }
        } else {
            _expenses.value = emptyList()
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
    
    private fun getUserExpenseCollection() = getCurrentUserId()?.let { userId ->
        firestore.collection("users").document(userId).collection("expenses")
    }
    
    // ==================== LOCAL STORAGE METHODS ====================
    
    /**
     * Load expenses from local storage
     */
    private fun loadLocalData() {
        try {
            val expensesJson = sharedPreferences.getString(getUserSpecificKey("expenses_list"), "[]")
            val expensesType = object : TypeToken<List<ExpenseEntry>>() {}.type
            val expenses: List<ExpenseEntry> = gson.fromJson(expensesJson, expensesType) ?: emptyList()
            _expenses.value = expenses.sortedByDescending { it.createdAt }
            
            Log.d(TAG, "Loaded ${expenses.size} expenses from user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local expenses", e)
            _expenses.value = emptyList()
        }
    }
    
    /**
     * Save expenses to local storage
     */
    private fun saveLocalData() {
        try {
            val expensesJson = gson.toJson(_expenses.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey("expenses_list"), expensesJson)
                .apply()
            Log.d(TAG, "Saved ${_expenses.value.size} expenses to user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving expenses locally", e)
        }
    }
    
    // ==================== CRUD OPERATIONS ====================
    
    /**
     * Add new expense entry (saves to both local and cloud)
     */
    suspend fun addExpense(expenseEntry: ExpenseEntry): Result<String> {
        return try {
            // Save to local storage first
            val updatedExpenses = _expenses.value.toMutableList()
            updatedExpenses.add(0, expenseEntry) // Add to beginning for latest first
            _expenses.value = updatedExpenses
            saveLocalData()
            
            Log.d(TAG, "Expense saved locally: ${expenseEntry.id}")
            
            // Save to Firestore asynchronously
            saveToFirestore(expenseEntry)
            
            Result.success(expenseEntry.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding expense: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Update existing expense entry
     */
    suspend fun updateExpense(expenseEntry: ExpenseEntry): Result<String> {
        return try {
            // Update local storage first
            val updatedExpenses = _expenses.value.toMutableList()
            val index = updatedExpenses.indexOfFirst { it.id == expenseEntry.id }
            
            if (index != -1) {
                updatedExpenses[index] = expenseEntry
                _expenses.value = updatedExpenses
                saveLocalData()
                
                Log.d(TAG, "Expense updated locally: ${expenseEntry.id}")
                
                // Update in Firestore asynchronously
                updateInFirestore(expenseEntry)
                
                Result.success(expenseEntry.id)
            } else {
                Log.w(TAG, "Expense not found for update: ${expenseEntry.id}")
                Result.failure(Exception("Expense not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating expense: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Delete expense entry
     */
    suspend fun deleteExpense(expenseId: String): Result<String> {
        return try {
            // Remove from local storage first
            val updatedExpenses = _expenses.value.toMutableList()
            val index = updatedExpenses.indexOfFirst { it.id == expenseId }
            
            if (index != -1) {
                updatedExpenses.removeAt(index)
                _expenses.value = updatedExpenses
                saveLocalData()
                
                Log.d(TAG, "Expense deleted locally: $expenseId")
                
                // Delete from Firestore asynchronously
                deleteFromFirestore(expenseId)
                
                Result.success(expenseId)
            } else {
                Log.w(TAG, "Expense not found for deletion: $expenseId")
                Result.failure(Exception("Expense not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting expense: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    // ==================== FIRESTORE METHODS ====================
    
    /**
     * Save expense entry to Firestore
     */
    private fun saveToFirestore(expenseEntry: ExpenseEntry) {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val expenseCollection = getUserExpenseCollection() ?: return@launch
                
                val expenseData = hashMapOf(
                    "id" to expenseEntry.id,
                    "expenseType" to expenseEntry.expenseType,
                    "description" to expenseEntry.description,
                    "amount" to expenseEntry.amount,
                    "paymentMethod" to expenseEntry.paymentMethod,
                    "vendor" to expenseEntry.vendor,
                    "location" to expenseEntry.location,
                    "notes" to expenseEntry.notes,
                    "createdAt" to expenseEntry.createdAt,
                    "updatedAt" to expenseEntry.updatedAt,
                    "userId" to userId,
                    "date" to expenseEntry.date,
                    "time" to expenseEntry.time
                )
                
                expenseCollection.document(expenseEntry.id).set(expenseData).await()
                Log.d(TAG, "Expense saved to Firestore: ${expenseEntry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving expense to Firestore: ${e.message}", e)
            }
        }
    }
    
    /**
     * Update expense entry in Firestore
     */
    private fun updateInFirestore(expenseEntry: ExpenseEntry) {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val expenseCollection = getUserExpenseCollection() ?: return@launch
                
                val updateData = hashMapOf(
                    "expenseType" to expenseEntry.expenseType,
                    "description" to expenseEntry.description,
                    "amount" to expenseEntry.amount,
                    "paymentMethod" to expenseEntry.paymentMethod,
                    "vendor" to expenseEntry.vendor,
                    "location" to expenseEntry.location,
                    "notes" to expenseEntry.notes,
                    "updatedAt" to expenseEntry.updatedAt,
                    "time" to expenseEntry.time
                )
                
                expenseCollection.document(expenseEntry.id).update(updateData as Map<String, Any>).await()
                Log.d(TAG, "Expense updated in Firestore: ${expenseEntry.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating expense in Firestore: ${e.message}", e)
            }
        }
    }
    
    /**
     * Delete expense from Firestore
     */
    private fun deleteFromFirestore(expenseId: String) {
        coroutineScope.launch {
            try {
                val userId = getCurrentUserId() ?: return@launch
                val expenseCollection = getUserExpenseCollection() ?: return@launch
                
                expenseCollection.document(expenseId).delete().await()
                Log.d(TAG, "Expense deleted from Firestore: $expenseId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting expense from Firestore: ${e.message}", e)
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
                val expenseCollection = getUserExpenseCollection() ?: return@launch
                
                val snapshot = expenseCollection
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()
                
                val cloudExpenses = snapshot.documents.mapNotNull { doc ->
                    try {
                        ExpenseEntry(
                            id = doc.getString("id") ?: "",
                            expenseType = doc.getString("expenseType") ?: "",
                            description = doc.getString("description") ?: "",
                            amount = doc.getString("amount") ?: "",
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            vendor = doc.getString("vendor") ?: "",
                            location = doc.getString("location") ?: "",
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            updatedAt = doc.getLong("updatedAt") ?: 0L,
                            date = doc.getString("date") ?: "",
                            time = doc.getString("time") ?: ""
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing expense document: ${doc.id}", e)
                        null
                    }
                }
                
                // Merge cloud data with local data (prioritize local changes)
                val localExpenses = _expenses.value
                val mergedExpenses = mergeExpenses(localExpenses, cloudExpenses)
                
                _expenses.value = mergedExpenses.sortedByDescending { it.createdAt }
                saveLocalData()
                
                Log.d(TAG, "Synced ${cloudExpenses.size} expenses from Firestore, merged to ${mergedExpenses.size} total")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing with Firestore", e)
            }
        }
    }
    
    /**
     * Merge local and cloud expenses (prioritize local changes)
     */
    private fun mergeExpenses(localExpenses: List<ExpenseEntry>, cloudExpenses: List<ExpenseEntry>): List<ExpenseEntry> {
        val mergedMap = mutableMapOf<String, ExpenseEntry>()
        
        // Add cloud expenses first
        cloudExpenses.forEach { expense ->
            mergedMap[expense.id] = expense
        }
        
        // Override with local expenses (prioritize local changes)
        localExpenses.forEach { expense ->
            val existing = mergedMap[expense.id]
            if (existing == null || expense.updatedAt >= existing.updatedAt) {
                mergedMap[expense.id] = expense
            }
        }
        
        return mergedMap.values.toList()
    }
    
    /**
     * Get total expense amount
     */
    fun getTotalExpenseAmount(): Double {
        return try {
            _expenses.value.sumOf { 
                it.amount.toDoubleOrNull() ?: 0.0 
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating total expense amount: ${e.message}", e)
            0.0
        }
    }
    
    /**
     * Upload all local expenses to Firestore
     */
    fun uploadAllToFirestore() {
        coroutineScope.launch {
            try {
                val localExpenses = _expenses.value
                Log.d(TAG, "Uploading ${localExpenses.size} expenses to Firestore")
                
                localExpenses.forEach { expense ->
                    saveToFirestore(expense)
                }
                
                Log.d(TAG, "All expenses uploaded to Firestore")
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading expenses to Firestore", e)
            }
        }
    }
    
    /**
     * Force sync with cloud data
     */
    fun syncWithCloud() {
        syncWithFirestore()
    }
}
