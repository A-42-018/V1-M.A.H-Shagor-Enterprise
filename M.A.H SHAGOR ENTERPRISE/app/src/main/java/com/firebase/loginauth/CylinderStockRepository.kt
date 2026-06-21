package com.firebase.loginauth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class CylinderStockRepository(context: Context) {
    // Local storage components
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("cylinder_stock_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Firebase components
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _stockItems = MutableStateFlow<List<CylinderStock>>(emptyList())
    val stockItems: StateFlow<List<CylinderStock>> = _stockItems
    
    private val _stockTransactions = MutableStateFlow<List<StockTransaction>>(emptyList())
    val stockTransactions: StateFlow<List<StockTransaction>> = _stockTransactions
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error
    
    companion object {
        private const val STOCK_ITEMS_KEY = "stock_items_list"
        private const val STOCK_TRANSACTIONS_KEY = "stock_transactions_list"
        private const val TAG = "CylinderStockRepository"
    }
    
    // Get user-specific key for SharedPreferences
    private fun getUserSpecificKey(baseKey: String): String {
        val user = auth.currentUser
        return if (user != null) {
            "${baseKey}_${user.uid}"
        } else {
            baseKey // Fallback to base key if no user
        }
    }
    
    init {
        loadLocalData()
        // Sync with Firestore if user is authenticated
        if (auth.currentUser != null) {
            syncWithFirestore()
        }
    }
    
    private fun getCurrentUserId(): String {
        val user = auth.currentUser
        if (user == null) {
            println("DEBUG: User not authenticated in CylinderStockRepository")
            throw IllegalStateException("User not authenticated")
        }
        println("DEBUG: Current user ID: ${user.uid}")
        return user.uid
    }
    
    private fun getStockCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("cylinder_stock")
    
    private fun getTransactionCollection() = firestore
        .collection("users")
        .document(getCurrentUserId())
        .collection("stock_transactions")
    
    // ==================== LOCAL STORAGE METHODS ====================
    
    /**
     * Load data from local storage
     */
    private fun loadLocalData() {
        try {
            // Load stock items
            val stockItemsJson = sharedPreferences.getString(getUserSpecificKey(STOCK_ITEMS_KEY), "[]")
            val stockItemsType = object : TypeToken<List<CylinderStock>>() {}.type
            val stockItems: List<CylinderStock> = gson.fromJson(stockItemsJson, stockItemsType) ?: emptyList()
            _stockItems.value = stockItems
            
            // Load stock transactions
            val transactionsJson = sharedPreferences.getString(getUserSpecificKey(STOCK_TRANSACTIONS_KEY), "[]")
            val transactionsType = object : TypeToken<List<StockTransaction>>() {}.type
            val transactions: List<StockTransaction> = gson.fromJson(transactionsJson, transactionsType) ?: emptyList()
            _stockTransactions.value = transactions
            
            Log.d(TAG, "Loaded ${stockItems.size} stock items and ${transactions.size} transactions from local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local data", e)
            _stockItems.value = emptyList()
            _stockTransactions.value = emptyList()
        }
    }
    
    /**
     * Save stock items to local storage
     */
    private fun saveStockItemsLocally() {
        try {
            val stockItemsJson = gson.toJson(_stockItems.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey(STOCK_ITEMS_KEY), stockItemsJson)
                .apply()
            Log.d(TAG, "Saved ${_stockItems.value.size} stock items to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving stock items locally", e)
        }
    }
    
    /**
     * Save transactions to local storage
     */
    private fun saveTransactionsLocally() {
        try {
            val transactionsJson = gson.toJson(_stockTransactions.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey(STOCK_TRANSACTIONS_KEY), transactionsJson)
                .apply()
            Log.d(TAG, "Saved ${_stockTransactions.value.size} transactions to local storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transactions locally", e)
        }
    }
    
    // Load all stock items (loads from local first, then syncs with Firestore)
    suspend fun loadStockItems(): Result<List<CylinderStock>> {
        return try {
            _isLoading.value = true
            _error.value = null
            
            // Return local data immediately
            val localStocks = _stockItems.value
            
            // Try to sync with Firestore in background
            try {
                val snapshot = getStockCollection()
                    .orderBy("cylinderType")
                    .orderBy("brand")
                    .get()
                    .await()
                
                val firestoreStocks = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(CylinderStock::class.java)?.copy(id = doc.id)
                }
                
                // Merge with local data (prioritize local changes)
                val mergedStocks = mergeStockData(localStocks, firestoreStocks)
                _stockItems.value = mergedStocks
                saveStockItemsLocally()
                
                Log.d(TAG, "Synced ${firestoreStocks.size} items from Firestore, total: ${mergedStocks.size}")
            } catch (e: Exception) {
                Log.w(TAG, "Could not sync with Firestore, using local data: ${e.message}")
            }
            
            Result.success(_stockItems.value)
        } catch (e: Exception) {
            _error.value = "স্টক লোড করতে সমস্যা হয়েছে: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    // Add new stock item (saves to both local and Firestore)
    suspend fun addStockItem(stock: CylinderStock): Result<String> {
        return try {
            println("DEBUG: Starting addStockItem with stock: $stock")
            _isLoading.value = true
            _error.value = null
            
            // Add to local storage first
            val updatedStocks = _stockItems.value.toMutableList()
            val stockWithId = stock.copy(id = stock.id.ifEmpty { generateStockId() })
            updatedStocks.add(stockWithId)
            _stockItems.value = updatedStocks
            saveStockItemsLocally()
            
            // Save to Firestore (required for cloud sync)
            try {
                val userId = getCurrentUserId()
                println("DEBUG: User authenticated successfully: $userId")
                
                val collection = getStockCollection()
                println("DEBUG: Got collection reference: ${collection.path}")
                
                println("DEBUG: Adding stock to Firestore...")
                collection.document(stockWithId.id).set(stockWithId).await()
                println("DEBUG: Stock added successfully to Firestore with ID: ${stockWithId.id}")
            } catch (e: Exception) {
                println("ERROR: Failed to save stock to Firestore: ${e.message}")
                e.printStackTrace()
                _error.value = "ক্লাউডে ডেটা সেভ করতে সমস্যা: ${e.message}"
                // Still continue with local save but show error
            }
            
            // Create initial stock transaction
            val transaction = StockTransaction(
                stockId = stockWithId.id,
                transactionType = "IN",
                quantity = stock.totalQuantity,
                previousQuantity = 0,
                newQuantity = stock.totalQuantity,
                reason = "Initial Stock",
                performedBy = auth.currentUser?.uid ?: "unknown",
                notes = "Initial stock entry"
            )
            
            println("DEBUG: Adding stock transaction...")
            addStockTransaction(transaction)
            
            println("DEBUG: addStockItem completed successfully")
            Result.success(stockWithId.id)
        } catch (e: Exception) {
            println("DEBUG: Error in addStockItem: ${e.message}")
            e.printStackTrace()
            _error.value = "স্টক যোগ করতে সমস্যা হয়েছে: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    // Update stock item
    suspend fun updateStockItem(stock: CylinderStock): Result<Unit> {
        return try {
            println("DEBUG: Starting updateStockItem with stock: $stock")
            _isLoading.value = true
            _error.value = null
            
            // Update local storage first
            val updatedStock = stock.copy(lastUpdated = Timestamp.now())
            val updatedStocks = _stockItems.value.toMutableList()
            println("DEBUG: Current stocks count: ${updatedStocks.size}")
            val index = updatedStocks.indexOfFirst { it.id == stock.id }
            println("DEBUG: Found stock at index: $index for ID: ${stock.id}")
            if (index != -1) {
                println("DEBUG: Updating stock at index $index")
                updatedStocks[index] = updatedStock
                _stockItems.value = updatedStocks
                saveStockItemsLocally()
                println("DEBUG: Stock updated locally successfully")
            } else {
                println("DEBUG: ERROR - Stock with ID ${stock.id} not found in local storage!")
                return Result.failure(Exception("Stock not found in local storage"))
            }
            
            // Update in Firestore (required for cloud sync)
            try {
                getStockCollection()
                    .document(stock.id)
                    .set(updatedStock)
                    .await()
                println("DEBUG: Stock updated successfully in Firestore")
            } catch (e: Exception) {
                println("ERROR: Failed to update stock in Firestore: ${e.message}")
                e.printStackTrace()
                _error.value = "ক্লাউডে ডেটা আপডেট করতে সমস্যা: ${e.message}"
                // Still continue with local update but show error
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "স্টক আপডেট করতে সমস্যা হয়েছে: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    // Delete stock item
    suspend fun deleteStockItem(stockId: String): Result<Unit> {
        return try {
            _isLoading.value = true
            _error.value = null
            
            // Remove from local storage first
            val updatedStocks = _stockItems.value.toMutableList()
            updatedStocks.removeAll { it.id == stockId }
            _stockItems.value = updatedStocks
            saveStockItemsLocally()
            
            // Delete from Firestore (required for cloud sync)
            try {
                getStockCollection().document(stockId).delete().await()
                println("DEBUG: Stock deleted successfully from Firestore")
            } catch (e: Exception) {
                println("ERROR: Failed to delete stock from Firestore: ${e.message}")
                e.printStackTrace()
                _error.value = "ক্লাউড থেকে ডেটা ডিলিট করতে সমস্যা: ${e.message}"
                // Still continue with local delete but show error
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "স্টক ডিলিট করতে সমস্যা হয়েছে: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    // Adjust stock quantity (for sales, purchases, damages, etc.)
    suspend fun adjustStock(
        stockId: String,
        quantityChange: Int,
        reason: String,
        orderId: String? = null,
        customerId: String? = null,
        notes: String = ""
    ): Result<Unit> {
        return try {
            println("DEBUG: Starting adjustStock with stockId: $stockId, quantityChange: $quantityChange")
            _isLoading.value = true
            _error.value = null
            
            // Find stock in local storage first
            val currentStocks = _stockItems.value.toMutableList()
            val stockIndex = currentStocks.indexOfFirst { it.id == stockId }
            
            if (stockIndex == -1) {
                println("DEBUG: Stock with ID $stockId not found in local storage")
                return Result.failure(Exception("Stock item not found"))
            }
            
            val currentStock = currentStocks[stockIndex]
            println("DEBUG: Found stock: ${currentStock.brand} - ${currentStock.cylinderType}, current quantity: ${currentStock.availableQuantity}")
            
            val newAvailableQuantity = currentStock.availableQuantity + quantityChange
            val newSoldQuantity = if (quantityChange < 0) {
                currentStock.soldQuantity + (-quantityChange)
            } else currentStock.soldQuantity
            
            println("DEBUG: New available quantity will be: $newAvailableQuantity")
            
            if (newAvailableQuantity < 0) {
                println("DEBUG: Not enough stock available")
                return Result.failure(Exception("পর্যাপ্ত স্টক নেই"))
            }
            
            val updatedStock = currentStock.copy(
                availableQuantity = newAvailableQuantity,
                soldQuantity = newSoldQuantity,
                lastUpdated = Timestamp.now()
            )
            
            // Update local storage first
            currentStocks[stockIndex] = updatedStock
            _stockItems.value = currentStocks
            saveStockItemsLocally()
            println("DEBUG: Stock updated locally successfully")
            
            // Update in Firestore (required for cloud sync)
            try {
                getStockCollection().document(stockId).set(updatedStock).await()
                println("DEBUG: Stock updated in Firestore successfully")
            } catch (e: Exception) {
                println("ERROR: Failed to update stock in Firestore: ${e.message}")
                e.printStackTrace()
                _error.value = "ক্লাউডে স্টক আপডেট করতে সমস্যা: ${e.message}"
                // Still continue with local update but show error
            }
            
            // Record transaction
            val transaction = StockTransaction(
                stockId = stockId,
                transactionType = if (quantityChange > 0) "IN" else "OUT",
                quantity = kotlin.math.abs(quantityChange),
                previousQuantity = currentStock.availableQuantity,
                newQuantity = newAvailableQuantity,
                reason = reason,
                orderId = orderId,
                customerId = customerId,
                performedBy = getCurrentUserId(),
                notes = notes
            )
            
            println("DEBUG: Adding stock transaction...")
            addStockTransaction(transaction)
            
            Result.success(Unit)
        } catch (e: Exception) {
            _error.value = "স্টক আপডেট করতে সমস্যা হয়েছে: ${e.message}"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }
    
    // Add stock transaction
    private suspend fun addStockTransaction(transaction: StockTransaction) {
        try {
            // Save to Firestore (required for cloud sync)
            getTransactionCollection().add(transaction).await()
            println("DEBUG: Stock transaction saved successfully to Firestore")
            loadStockTransactions()
        } catch (e: Exception) {
            println("ERROR: Failed to save stock transaction to Firestore: ${e.message}")
            e.printStackTrace()
            _error.value = "ক্লাউডে ট্রানজেকশন সেভ করতে সমস্যা: ${e.message}"
            // Log error but don't fail the main operation
            println("Failed to record stock transaction: ${e.message}")
        }
    }
    
    // Load stock transactions
    suspend fun loadStockTransactions(limit: Int = 50): Result<List<StockTransaction>> {
        return try {
            val snapshot = getTransactionCollection()
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val transactions = snapshot.documents.mapNotNull { doc ->
                doc.toObject(StockTransaction::class.java)?.copy(id = doc.id)
            }
            
            _stockTransactions.value = transactions
            Result.success(transactions)
        } catch (e: Exception) {
            _error.value = "ট্রানজেকশন লোড করতে সমস্যা হয়েছে: ${e.message}"
            Result.failure(e)
        }
    }
    
    // Get low stock items
    suspend fun getLowStockItems(): Result<List<CylinderStock>> {
        return try {
            val allStocks = _stockItems.value.ifEmpty { 
                loadStockItems().getOrThrow()
            }
            
            val lowStockItems = allStocks.filter { stock ->
                stock.availableQuantity <= stock.lowStockThreshold
            }
            
            Result.success(lowStockItems)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Get stock by ID
    suspend fun getStockById(stockId: String): Result<CylinderStock?> {
        return try {
            val doc = getStockCollection().document(stockId).get().await()
            val stock = doc.toObject(CylinderStock::class.java)?.copy(id = doc.id)
            Result.success(stock)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Search stock items
    fun searchStockItems(query: String): List<CylinderStock> {
        val searchQuery = query.lowercase()
        return _stockItems.value.filter { stock ->
            stock.cylinderType.lowercase().contains(searchQuery) ||
            stock.brand.lowercase().contains(searchQuery) ||
            stock.supplierName.lowercase().contains(searchQuery) ||
            stock.location.lowercase().contains(searchQuery)
        }
    }
    
    // Clear error
    fun clearError() {
        _error.value = null
    }
    
    // ==================== HELPER METHODS FOR DUAL STORAGE ====================
    
    /**
     * Generate unique stock ID
     */
    private fun generateStockId(): String {
        return "stock_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * Merge local and Firestore stock data
     */
    private fun mergeStockData(localStocks: List<CylinderStock>, firestoreStocks: List<CylinderStock>): List<CylinderStock> {
        val mergedMap = mutableMapOf<String, CylinderStock>()
        
        // Add all local stocks first
        localStocks.forEach { stock ->
            mergedMap[stock.id] = stock
        }
        
        // Override with Firestore data (Firestore is source of truth)
        firestoreStocks.forEach { stock ->
            mergedMap[stock.id] = stock
        }
        
        return mergedMap.values.toList()
    }
    
    private fun mergeTransactionData(localTransactions: List<StockTransaction>, firestoreTransactions: List<StockTransaction>): List<StockTransaction> {
        val mergedMap = mutableMapOf<String, StockTransaction>()
        
        // Add all local transactions first
        localTransactions.forEach { transaction ->
            mergedMap[transaction.id] = transaction
        }
        
        // Override with Firestore data (Firestore is source of truth)
        firestoreTransactions.forEach { transaction ->
            mergedMap[transaction.id] = transaction
        }
        
        return mergedMap.values.toList()
    }
    
    /**
     * Sync with Firestore - load data from cloud and merge with local data
     */
    suspend fun forceFirestoreSync() {
        try {
            _isLoading.value = true
            
            // Sync stock items
            val stockSnapshot = getStockCollection()
                .orderBy("cylinderType")
                .orderBy("brand")
                .get()
                .await()
            
            val firestoreStocks = stockSnapshot.documents.mapNotNull { doc ->
                doc.toObject(CylinderStock::class.java)?.copy(id = doc.id)
            }
            
            // Merge with local data
            val localStocks = _stockItems.value
            val mergedStocks = mergeStockData(localStocks, firestoreStocks)
            _stockItems.value = mergedStocks
            saveStockItemsLocally()
            
            // Sync transactions
            val transactionSnapshot = getTransactionCollection()
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .await()
            
            val firestoreTransactions = transactionSnapshot.documents.mapNotNull { doc ->
                doc.toObject(StockTransaction::class.java)?.copy(id = doc.id)
            }
            
            // Merge with local transactions
            val localTransactions = _stockTransactions.value
            val mergedTransactions = mergeTransactionData(localTransactions, firestoreTransactions)
            _stockTransactions.value = mergedTransactions
            saveTransactionsLocally()
            
            Log.d(TAG, "Force sync completed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during force sync: ${e.message}")
            _error.value = "Sync failed: ${e.message}"
        } finally {
            _isLoading.value = false
        }
    }
    
    private fun syncWithFirestore() {
        coroutineScope.launch {
            forceFirestoreSync()
        }
    }
    
    private fun syncWithFirestoreOld() {
        coroutineScope.launch {
            try {
                // Sync stock items
                val stockSnapshot = getStockCollection()
                    .orderBy("cylinderType")
                    .orderBy("brand")
                    .get()
                    .await()
                
                val firestoreStocks = stockSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(CylinderStock::class.java)?.copy(id = doc.id)
                }
                
                // Merge with local data
                val localStocks = _stockItems.value
                val mergedStocks = mergeStockData(localStocks, firestoreStocks)
                _stockItems.value = mergedStocks
                saveStockItemsLocally()
                
                // Sync transactions
                val transactionSnapshot = getTransactionCollection()
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(100)
                    .get()
                    .await()
                
                val firestoreTransactions = transactionSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(StockTransaction::class.java)?.copy(id = doc.id)
                }
                
                // Merge transactions
                val localTransactions = _stockTransactions.value
                val mergedTransactions = mutableListOf<StockTransaction>()
                mergedTransactions.addAll(localTransactions)
                
                firestoreTransactions.forEach { firestoreTransaction ->
                    if (localTransactions.none { it.id == firestoreTransaction.id }) {
                        mergedTransactions.add(firestoreTransaction)
                    }
                }
                
                _stockTransactions.value = mergedTransactions.distinctBy { it.id }
                saveTransactionsLocally()
                
                Log.d(TAG, "Synced with Firestore: ${firestoreStocks.size} stocks, ${firestoreTransactions.size} transactions")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing with Firestore", e)
            }
        }
    }
    
    /**
     * Force sync with Firestore (can be called manually)
     */
    fun syncWithCloud() {
        if (auth.currentUser != null) {
            syncWithFirestore()
        } else {
            Log.w(TAG, "Cannot sync: User not authenticated")
        }
    }
    
    /**
     * Upload all local data to Firestore (useful for initial sync)
     */
    fun uploadAllToFirestore() {
        coroutineScope.launch {
            try {
                val stockCollection = getStockCollection()
                val transactionCollection = getTransactionCollection()
                
                // Upload stock items
                _stockItems.value.forEach { stock ->
                    stockCollection.document(stock.id).set(stock).await()
                }
                
                // Upload transactions
                _stockTransactions.value.forEach { transaction ->
                    transactionCollection.document(transaction.id).set(transaction).await()
                }
                
                Log.d(TAG, "Uploaded all data to Firestore: ${_stockItems.value.size} stocks, ${_stockTransactions.value.size} transactions")
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading to Firestore", e)
            }
        }
    }
}
