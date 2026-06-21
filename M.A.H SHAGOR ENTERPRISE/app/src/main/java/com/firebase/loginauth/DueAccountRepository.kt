package com.firebase.loginauth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class DueAccountRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("due_account_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private val _dueEntries = MutableStateFlow<List<DueAccountEntry>>(emptyList())
    val dueEntries: StateFlow<List<DueAccountEntry>> = _dueEntries.asStateFlow()
    
    private val _dueSummary = MutableStateFlow(DueSummary())
    val dueSummary: StateFlow<DueSummary> = _dueSummary.asStateFlow()
    
    private val _salesDueEntries = MutableStateFlow<List<DueAccountEntry>>(emptyList())
    val salesDueEntries: StateFlow<List<DueAccountEntry>> = _salesDueEntries.asStateFlow()

    init {
        // Load Firebase data first if user is authenticated, then fallback to local
        auth.currentUser?.let {
            CoroutineScope(Dispatchers.IO).launch {
                syncWithFirestore()
            }
        } ?: run {
            // Load local data only if no user is authenticated
            loadLocalData()
        }
    }
    
    // Get user-specific SharedPreferences key
    private fun getUserSpecificKey(baseKey: String): String {
        val userId = auth.currentUser?.uid ?: "anonymous"
        return "${baseKey}_${userId}"
    }

    // Load data from SharedPreferences
    private fun loadLocalData() {
        try {
            val entriesJson = sharedPreferences.getString(getUserSpecificKey("due_entries"), "[]")
            val listType = object : TypeToken<List<DueAccountEntry>>() {}.type
            val entries: List<DueAccountEntry> = gson.fromJson(entriesJson, listType) ?: emptyList()
            _dueEntries.value = entries
            calculateSummary()
            Log.d("DueAccountRepository", "Loaded ${entries.size} due entries from user-specific storage")
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error loading local data: ${e.message}")
            _dueEntries.value = emptyList()
            calculateSummary()
        }
    }

    // Save data to SharedPreferences
    private fun saveLocalData() {
        try {
            val entriesJson = gson.toJson(_dueEntries.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey("due_entries"), entriesJson)
                .apply()
            Log.d("DueAccountRepository", "Saved ${_dueEntries.value.size} due entries to user-specific storage")
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error saving local data: ${e.message}")
        }
    }

    // Add new due account entry
    suspend fun addDueEntry(entry: DueAccountEntry): Result<String> {
        return try {
            val currentEntries = _dueEntries.value.toMutableList()
            currentEntries.add(entry)
            _dueEntries.value = currentEntries
            saveLocalData()
            calculateSummary()
            
            // Save to Firestore if user is authenticated
            auth.currentUser?.let {
                saveToFirestore(entry)
            }
            
            Result.success("বাকীর এন্ট্রি সফলভাবে যোগ করা হয়েছে")
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error adding due entry: ${e.message}")
            Result.failure(Exception("বাকীর এন্ট্রি যোগ করতে সমস্যা হয়েছে"))
        }
    }

    // Update due account entry
    suspend fun updateDueEntry(entry: DueAccountEntry): Result<String> {
        return try {
            val currentEntries = _dueEntries.value.toMutableList()
            val index = currentEntries.indexOfFirst { it.id == entry.id }
            if (index != -1) {
                currentEntries[index] = entry
                _dueEntries.value = currentEntries
                saveLocalData()
                calculateSummary()
                
                // Update in Firestore if user is authenticated
                auth.currentUser?.let {
                    updateInFirestore(entry)
                }
                
                Result.success("বাকীর এন্ট্রি সফলভাবে আপডেট করা হয়েছে")
            } else {
                Result.failure(Exception("এন্ট্রি খুঁজে পাওয়া যায়নি"))
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error updating due entry: ${e.message}")
            Result.failure(Exception("বাকীর এন্ট্রি আপডেট করতে সমস্যা হয়েছে"))
        }
    }

    // Delete due account entry
    suspend fun deleteDueEntry(entryId: String): Result<String> {
        return try {
            val currentEntries = _dueEntries.value.toMutableList()
            val removed = currentEntries.removeAll { it.id == entryId }
            if (removed) {
                _dueEntries.value = currentEntries
                saveLocalData()
                calculateSummary()
                
                // Delete from Firestore if user is authenticated
                auth.currentUser?.let {
                    deleteFromFirestore(entryId)
                }
                
                Result.success("বাকীর এন্ট্রি সফলভাবে মুছে ফেলা হয়েছে")
            } else {
                Result.failure(Exception("এন্ট্রি খুঁজে পাওয়া যায়নি"))
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error deleting due entry: ${e.message}")
            Result.failure(Exception("বাকীর এন্ট্রি মুছতে সমস্যা হয়েছে"))
        }
    }

    // Get customer due balance
    fun getCustomerDueBalance(customerId: String): CustomerDueBalance? {
        val customerEntries = _dueEntries.value.filter { it.customerId == customerId }
        if (customerEntries.isEmpty()) return null
        
        val customerName = customerEntries.first().customerName
        val customerPhone = customerEntries.first().customerPhone
        
        var totalDue = 0.0
        customerEntries.forEach { entry ->
            when (entry.transactionType) {
                DueTransactionType.CREDIT -> totalDue += entry.amount
                DueTransactionType.PAYMENT -> totalDue -= entry.amount
                DueTransactionType.ADJUSTMENT -> totalDue += entry.amount
            }
        }
        
        val lastTransactionDate = customerEntries.maxOfOrNull { it.date } ?: 0L
        
        return CustomerDueBalance(
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            totalDueAmount = totalDue,
            lastTransactionDate = lastTransactionDate,
            transactionCount = customerEntries.size
        )
    }

    // Get all customers with due balances
    fun getAllCustomerDueBalances(): List<CustomerDueBalance> {
        val customerIds = _dueEntries.value.map { it.customerId }.distinct()
        return customerIds.mapNotNull { getCustomerDueBalance(it) }
            .filter { it.totalDueAmount > 0 } // Only customers with positive due
            .sortedByDescending { it.totalDueAmount }
    }
    
    // Get integrated customer due balances (manual + sales due)
    private fun getAllCustomerDueBalancesIntegrated(): List<CustomerDueBalance> {
        val manualEntries = _dueEntries.value
        val salesEntries = _salesDueEntries.value
        val allEntries = manualEntries + salesEntries
        val customerMap = mutableMapOf<String, CustomerDueBalance>()
        
        allEntries.forEach { entry ->
            val existingBalance = customerMap[entry.customerId]
            if (existingBalance != null) {
                val newBalance = when (entry.transactionType) {
                    DueTransactionType.CREDIT -> existingBalance.totalDueAmount + entry.amount
                    DueTransactionType.PAYMENT -> existingBalance.totalDueAmount - entry.amount
                    DueTransactionType.ADJUSTMENT -> existingBalance.totalDueAmount + entry.amount
                }
                customerMap[entry.customerId] = existingBalance.copy(
                    totalDueAmount = newBalance,
                    lastTransactionDate = maxOf(existingBalance.lastTransactionDate, entry.date),
                    transactionCount = existingBalance.transactionCount + 1
                )
            } else {
                val balance = when (entry.transactionType) {
                    DueTransactionType.CREDIT -> entry.amount
                    DueTransactionType.PAYMENT -> -entry.amount
                    DueTransactionType.ADJUSTMENT -> entry.amount
                }
                customerMap[entry.customerId] = CustomerDueBalance(
                    customerId = entry.customerId,
                    customerName = entry.customerName,
                    customerPhone = entry.customerPhone,
                    totalDueAmount = balance,
                    lastTransactionDate = entry.date,
                    transactionCount = 1
                )
            }
        }
        
        return customerMap.values
            .filter { it.totalDueAmount > 0 }
            .sortedByDescending { it.totalDueAmount }
    }

    // Collect sales due data and integrate with manual due entries
    fun integrateSalesDueData(salesWithDue: List<Order>) {
        try {
            val pendingSales = salesWithDue.filter { it.remainingAmount > 0 }
            Log.d("DueAccountRepository", "Integrating ${pendingSales.size} pending sales with due data")
            
            // Convert pending sales to due entries for calculation purposes
            val salesDueEntries = pendingSales.map { order ->
                DueAccountEntry(
                    id = "sales_${order.id}",
                    customerId = order.customerId,
                    customerName = order.customerName,
                    customerPhone = order.customerPhone,
                    transactionType = DueTransactionType.CREDIT,
                    amount = order.remainingAmount,
                    description = "বিক্রয়ের বকেয়া - অর্ডার #${order.orderNumber}",
                    date = parseOrderDate(order.orderDate),
                    orderId = order.id,
                    paymentMethod = "ক্যাশ",
                    notes = "স্বয়ংক্রিয় গণনা থেকে"
                )
            }
            
            // Store sales due entries separately for dashboard calculation
            _salesDueEntries.value = salesDueEntries
            
            // Recalculate summary with integrated data
            calculateSummary()
            
            Log.d("DueAccountRepository", "Sales due integration completed")
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error integrating sales due data: ${e.message}")
        }
    }
    
    // Parse order date string to timestamp
    private fun parseOrderDate(dateString: String): Long {
        return try {
            val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            format.parse(dateString)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
    
    // Calculate summary statistics with integrated sales due data
    private fun calculateSummary() {
        val manualEntries = _dueEntries.value
        val salesEntries = _salesDueEntries.value
        val allEntries = manualEntries + salesEntries
        
        // Get customer balances from both manual and sales due data
        val customerBalances = getAllCustomerDueBalancesIntegrated()
        
        val totalDueAmount = customerBalances.sumOf { it.totalDueAmount }
        val totalCustomersWithDue = customerBalances.size
        
        val today = Calendar.getInstance()
        today.set(Calendar.HOUR_OF_DAY, 0)
        today.set(Calendar.MINUTE, 0)
        today.set(Calendar.SECOND, 0)
        today.set(Calendar.MILLISECOND, 0)
        val todayStart = today.timeInMillis
        
        val todayEntries = allEntries.filter { it.date >= todayStart }
        val totalPaymentsToday = todayEntries
            .filter { it.transactionType == DueTransactionType.PAYMENT }
            .sumOf { it.amount }
        val totalDueGivenToday = todayEntries
            .filter { it.transactionType == DueTransactionType.CREDIT }
            .sumOf { it.amount }
        
        val highestDueCustomer = customerBalances.maxByOrNull { it.totalDueAmount }
        
        _dueSummary.value = DueSummary(
            totalDueAmount = totalDueAmount,
            totalCustomersWithDue = totalCustomersWithDue,
            totalPaymentsToday = totalPaymentsToday,
            totalDueGivenToday = totalDueGivenToday,
            highestDueCustomer = highestDueCustomer?.customerName ?: "",
            highestDueAmount = highestDueCustomer?.totalDueAmount ?: 0.0
        )
        
        Log.d("DueAccountRepository", "Dashboard summary calculated - Total Due: ৳$totalDueAmount, Customers: $totalCustomersWithDue")
    }

    // Filter entries by time period
    fun filterEntriesByPeriod(filter: DueAccountFilter): List<DueAccountEntry> {
        val entries = _dueEntries.value
        val calendar = Calendar.getInstance()
        
        return when (filter) {
            DueAccountFilter.ALL -> entries
            DueAccountFilter.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val todayStart = calendar.timeInMillis
                entries.filter { it.date >= todayStart }
            }
            DueAccountFilter.WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val weekStart = calendar.timeInMillis
                entries.filter { it.date >= weekStart }
            }
            DueAccountFilter.MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val monthStart = calendar.timeInMillis
                entries.filter { it.date >= monthStart }
            }
            DueAccountFilter.YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val yearStart = calendar.timeInMillis
                entries.filter { it.date >= yearStart }
            }
        }
    }

    // Firebase Firestore operations
    private suspend fun saveToFirestore(entry: DueAccountEntry) {
        try {
            auth.currentUser?.let { user ->
                firestore.collection("users")
                    .document(user.uid)
                    .collection("due_accounts")
                    .document(entry.id)
                    .set(entry)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error saving to Firestore: ${e.message}")
        }
    }

    private suspend fun updateInFirestore(entry: DueAccountEntry) {
        try {
            auth.currentUser?.let { user ->
                firestore.collection("users")
                    .document(user.uid)
                    .collection("due_accounts")
                    .document(entry.id)
                    .set(entry)
                    .await()
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error updating in Firestore: ${e.message}")
        }
    }

    private suspend fun deleteFromFirestore(entryId: String) {
        try {
            auth.currentUser?.let { user ->
                firestore.collection("users")
                    .document(user.uid)
                    .collection("due_accounts")
                    .document(entryId)
                    .delete()
                    .await()
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error deleting from Firestore: ${e.message}")
        }
    }

    private suspend fun syncWithFirestore() {
        try {
            auth.currentUser?.let { user ->
                val snapshot = firestore.collection("users")
                    .document(user.uid)
                    .collection("due_accounts")
                    .get()
                    .await()
                
                val cloudEntries = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(DueAccountEntry::class.java)
                }
                
                Log.d("DueAccountRepository", "Loaded ${cloudEntries.size} due entries from Firebase")
                
                // Load local data for merging
                loadLocalData()
                
                // Merge local and cloud data
                val mergedEntries = mergeEntries(_dueEntries.value, cloudEntries)
                _dueEntries.value = mergedEntries
                saveLocalData()
                calculateSummary()
                
                Log.d("DueAccountRepository", "Due account sync completed. Total entries: ${mergedEntries.size}")
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error syncing with Firestore: ${e.message}")
            // Fallback to local data if Firebase sync fails
            loadLocalData()
        }
    }

    private fun mergeEntries(localEntries: List<DueAccountEntry>, cloudEntries: List<DueAccountEntry>): List<DueAccountEntry> {
        val mergedMap = mutableMapOf<String, DueAccountEntry>()
        
        // Add cloud entries first
        cloudEntries.forEach { entry ->
            mergedMap[entry.id] = entry
        }
        
        // Add local entries (will overwrite cloud entries with same ID, prioritizing local changes)
        localEntries.forEach { entry ->
            mergedMap[entry.id] = entry
        }
        
        return mergedMap.values.toList().sortedByDescending { it.date }
    }

    // Upload all local data to Firestore
    suspend fun uploadAllToFirestore() {
        try {
            auth.currentUser?.let { user ->
                _dueEntries.value.forEach { entry ->
                    firestore.collection("users")
                        .document(user.uid)
                        .collection("due_accounts")
                        .document(entry.id)
                        .set(entry)
                        .await()
                }
            }
        } catch (e: Exception) {
            Log.e("DueAccountRepository", "Error uploading all to Firestore: ${e.message}")
        }
    }

    // Get total due amount for dashboard
    fun getTotalDueAmount(): Double {
        return _dueSummary.value.totalDueAmount
    }
}
