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

/**
 * Repository for managing customer data using dual storage (SharedPreferences + Firebase Firestore)
 * Follows the same pattern as CashDepositRepository for consistency
 */
class CustomerRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("customer_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    
    // StateFlow for reactive UI updates
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()
    
    private val _customerStats = MutableStateFlow(CustomerStats())
    val customerStats: StateFlow<CustomerStats> = _customerStats.asStateFlow()
    
    companion object {
        private const val TAG = "CustomerRepository"
        private const val FIRESTORE_COLLECTION = "customers"
    }
    
    init {
        // Load user-specific data if authenticated, otherwise load empty state
        if (auth.currentUser != null) {
            try {
                CoroutineScope(Dispatchers.IO).launch {
                    // Prioritize Firebase data over local storage for user isolation
                    syncWithFirestore()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Firestore sync", e)
                loadLocalData() // Fallback to local data
            }
        } else {
            _customers.value = emptyList()
            updateStats()
        }
    }
    
    /**
     * Get user-specific SharedPreferences key
     */
    private fun getUserSpecificKey(baseKey: String): String {
        val userId = auth.currentUser?.uid ?: "anonymous"
        return "${baseKey}_${userId}"
    }
    
    // Local Storage Methods
    private fun loadLocalData() {
        try {
            val customersJson = sharedPreferences.getString(getUserSpecificKey("customers_list"), "[]")
            val type = object : TypeToken<List<Customer>>() {}.type
            val customersList: List<Customer> = gson.fromJson(customersJson, type) ?: emptyList()
            
            _customers.value = customersList
            updateStats()
            
            Log.d(TAG, "Loaded ${customersList.size} customers from user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading customers", e)
            _customers.value = emptyList()
        }
    }
    
    private fun saveLocalData() {
        try {
            val customersJson = gson.toJson(_customers.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey("customers_list"), customersJson)
                .apply()
            
            updateStats()
            Log.d(TAG, "Saved ${_customers.value.size} customers to user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving customers", e)
        }
    }
    
    // Firestore Methods
    private suspend fun saveToFirestore(customer: Customer) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_COLLECTION)
                .document(customer.id)
                .set(customer)
                .await()
            Log.d(TAG, "Saved customer ${customer.id} to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving customer to Firestore", e)
        }
    }
    
    private suspend fun updateInFirestore(customer: Customer) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_COLLECTION)
                .document(customer.id)
                .set(customer)
                .await()
            Log.d(TAG, "Updated customer ${customer.id} in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating customer in Firestore", e)
        }
    }
    
    private suspend fun deleteFromFirestore(customerId: String) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_COLLECTION)
                .document(customerId)
                .delete()
                .await()
            Log.d(TAG, "Deleted customer $customerId from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting customer from Firestore", e)
        }
    }
    
    suspend fun syncWithFirestore() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_COLLECTION)
                .get()
                .await()
            
            val firestoreCustomers = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Customer::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing customer document", e)
                    null
                }
            }
            
            // Merge with local data (prioritize local changes)
            val mergedCustomers = mergeCustomers(_customers.value, firestoreCustomers)
            _customers.value = mergedCustomers
            saveLocalData()
            
            Log.d(TAG, "Synced ${mergedCustomers.size} customers with Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore", e)
        }
    }
    
    private fun mergeCustomers(localCustomers: List<Customer>, firestoreCustomers: List<Customer>): List<Customer> {
        val mergedMap = mutableMapOf<String, Customer>()
        
        // Add Firestore customers first
        firestoreCustomers.forEach { customer ->
            mergedMap[customer.id] = customer
        }
        
        // Override with local customers (prioritize local changes)
        localCustomers.forEach { customer ->
            mergedMap[customer.id] = customer
        }
        
        return mergedMap.values.toList()
    }
    
    suspend fun uploadAllToFirestore() {
        try {
            _customers.value.forEach { customer ->
                saveToFirestore(customer)
            }
            Log.d(TAG, "Uploaded all customers to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading customers to Firestore", e)
        }
    }
    
    /**
     * Add a new customer
     */
    fun addCustomer(customer: Customer): Boolean {
        return try {
            // Check if customer with same phone already exists
            val existingCustomer = _customers.value.find { it.phone == customer.phone }
            if (existingCustomer != null) {
                Log.w(TAG, "Customer with phone ${customer.phone} already exists")
                return false
            }
            
            val updatedList = _customers.value.toMutableList()
            updatedList.add(customer)
            _customers.value = updatedList
            saveLocalData()
            
            // Save to Firestore in background
            CoroutineScope(Dispatchers.IO).launch {
                saveToFirestore(customer)
            }
            
            Log.d(TAG, "Added new customer: ${customer.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding customer", e)
            false
        }
    }
    
    /**
     * Update an existing customer
     */
    fun updateCustomer(customer: Customer): Boolean {
        return try {
            val updatedList = _customers.value.toMutableList()
            val index = updatedList.indexOfFirst { it.id == customer.id }
            
            if (index != -1) {
                updatedList[index] = customer
                _customers.value = updatedList
                saveLocalData()
                
                // Update in Firestore in background
                CoroutineScope(Dispatchers.IO).launch {
                    updateInFirestore(customer)
                }
                
                Log.d(TAG, "Updated customer: ${customer.name}")
                true
            } else {
                Log.w(TAG, "Customer not found for update: ${customer.id}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating customer", e)
            false
        }
    }
    
    /**
     * Delete a customer
     */
    fun deleteCustomer(customerId: String): Boolean {
        return try {
            val updatedList = _customers.value.toMutableList()
            val removed = updatedList.removeAll { it.id == customerId }
            
            if (removed) {
                _customers.value = updatedList
                saveLocalData()
                
                // Delete from Firestore in background
                CoroutineScope(Dispatchers.IO).launch {
                    deleteFromFirestore(customerId)
                }
                
                Log.d(TAG, "Deleted customer: $customerId")
                true
            } else {
                Log.w(TAG, "Customer not found for deletion: $customerId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting customer", e)
            false
        }
    }
    
    /**
     * Get customer by ID
     */
    fun getCustomerById(customerId: String): Customer? {
        return _customers.value.find { it.id == customerId }
    }
    
    /**
     * Get customer by phone number
     */
    fun getCustomerByPhone(phone: String): Customer? {
        return _customers.value.find { it.phone == phone.trim() }
    }
    
    /**
     * Refresh customers from Firestore
     */
    suspend fun refreshCustomers() {
        try {
            syncWithFirestore()
            Log.d(TAG, "Customers refreshed from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing customers", e)
        }
    }
    
    /**
     * Search customers by name or phone
     */
    fun searchCustomers(query: String): List<Customer> {
        if (query.isBlank()) return _customers.value
        
        val searchQuery = query.lowercase()
        return _customers.value.filter { customer ->
            customer.name.lowercase().contains(searchQuery) ||
            customer.phone.contains(searchQuery) ||
            customer.area.lowercase().contains(searchQuery)
        }
    }
    
    /**
     * Filter customers by type
     */
    fun getCustomersByType(type: CustomerType): List<Customer> {
        return _customers.value.filter { it.customerType == type }
    }
    
    /**
     * Get customers by area
     */
    fun getCustomersByArea(area: String): List<Customer> {
        return _customers.value.filter { 
            it.area.lowercase().contains(area.lowercase()) 
        }
    }
    
    /**
     * Get active customers only
     */
    fun getActiveCustomers(): List<Customer> {
        return _customers.value.filter { it.isActive }
    }
    
    /**
     * Update customer statistics
     */
    private fun updateStats() {
        val customers = _customers.value
        val stats = CustomerStats(
            totalCustomers = customers.size,
            activeCustomers = customers.count { it.isActive },
            vipCustomers = customers.count { it.customerType == CustomerType.VIP },
            commercialCustomers = customers.count { it.customerType == CustomerType.COMMERCIAL },
            totalCredit = customers.sumOf { it.currentCredit },
            totalOrders = customers.sumOf { it.totalOrders }
        )
        _customerStats.value = stats
    }
    
    /**
     * Add sample customers for testing (can be removed in production)
     */
    fun addSampleCustomers() {
        if (_customers.value.isNotEmpty()) return
        
        val sampleCustomers = listOf(
            Customer(
                name = "মোহাম্মদ রহিম",
                phone = "01712345678",
                address = "বাড়ি নং ১২, রোড নং ৫",
                area = "ধানমন্ডি",
                customerType = CustomerType.REGULAR,
                creditLimit = 5000.0,
                currentCredit = 1200.0,
                totalOrders = 15
            ),
            Customer(
                name = "ফাতেমা খাতুন",
                phone = "01823456789",
                address = "প্লট নং ৮, ব্লক বি",
                area = "গুলশান",
                customerType = CustomerType.VIP,
                creditLimit = 10000.0,
                currentCredit = 0.0,
                totalOrders = 25
            ),
            Customer(
                name = "আহমেদ এন্টারপ্রাইজ",
                phone = "01934567890",
                address = "শপ নং ১৫, কমার্শিয়াল এরিয়া",
                area = "মতিঝিল",
                customerType = CustomerType.COMMERCIAL,
                creditLimit = 25000.0,
                currentCredit = 5000.0,
                totalOrders = 45
            )
        )
        
        sampleCustomers.forEach { customer ->
            addCustomer(customer)
        }
        
        Log.d(TAG, "Added sample customers for testing")
    }
    
    /**
     * Clear all customers (for testing purposes)
     */
    fun clearAllCustomers() {
        _customers.value = emptyList()
        saveLocalData()
        Log.d(TAG, "Cleared all customers")
    }
}
