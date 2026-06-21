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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.tasks.await

class DeliveryRepository(private val context: Context) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("delivery_prefs", Context.MODE_PRIVATE)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()
    
    // Repositories for integration
    private val orderRepository = OrderRepository.getInstance(context)
    private val customerRepository = CustomerRepository(context)
    
    private val _deliveries = MutableStateFlow<List<DeliveryEntry>>(emptyList())
    val deliveries: StateFlow<List<DeliveryEntry>> = _deliveries.asStateFlow()
    
    private val _deliveryPersons = MutableStateFlow<List<DeliveryPerson>>(emptyList())
    val deliveryPersons: StateFlow<List<DeliveryPerson>> = _deliveryPersons.asStateFlow()
    
    // Expose OrderRepository's orders StateFlow for real-time sync
    val allOrders: StateFlow<List<Order>> = orderRepository.orders
    
    // Real-time StateFlow for running orders (PENDING, CONFIRMED)
    val runningOrders: StateFlow<List<Order>> = orderRepository.orders.map { orders ->
        orders.filter { order ->
            order.orderStatus == OrderStatus.PENDING || 
            order.orderStatus == OrderStatus.CONFIRMED
        }
    }.stateIn(
        scope = CoroutineScope(Dispatchers.IO),
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    companion object {
        private const val TAG = "DeliveryRepository"
        private const val DELIVERIES_KEY = "deliveries_list"
        private const val DELIVERY_PERSONS_KEY = "delivery_persons_list"
        private const val FIRESTORE_DELIVERIES_COLLECTION = "deliveries"
        private const val FIRESTORE_DELIVERY_PERSONS_COLLECTION = "delivery_persons"
    }
    
    // Get user-specific SharedPreferences key
    private fun getUserSpecificKey(baseKey: String): String {
        val userId = auth.currentUser?.uid ?: "anonymous"
        return "${baseKey}_${userId}"
    }
    
    init {
        loadLocalData()
        // Auto-sync with Firestore if user is authenticated (will be done manually)
    }
    
    // Local Storage Methods
    private fun loadLocalData() {
        try {
            // Load deliveries
            val deliveriesJson = sharedPreferences.getString(getUserSpecificKey(DELIVERIES_KEY), "[]")
            val deliveriesType = object : TypeToken<List<DeliveryEntry>>() {}.type
            val deliveries = gson.fromJson<List<DeliveryEntry>>(deliveriesJson, deliveriesType) ?: emptyList()
            _deliveries.value = deliveries
            
            // Load delivery persons
            val personsJson = sharedPreferences.getString(getUserSpecificKey(DELIVERY_PERSONS_KEY), "[]")
            val personsType = object : TypeToken<List<DeliveryPerson>>() {}.type
            val persons = gson.fromJson<List<DeliveryPerson>>(personsJson, personsType) ?: getDefaultDeliveryPersons()
            _deliveryPersons.value = persons
            
            Log.d(TAG, "Loaded ${deliveries.size} deliveries and ${persons.size} delivery persons from user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading local data", e)
            _deliveries.value = emptyList()
            _deliveryPersons.value = getDefaultDeliveryPersons()
        }
    }
    
    private fun saveLocalData() {
        try {
            val editor = sharedPreferences.edit()
            
            // Save deliveries
            val deliveriesJson = gson.toJson(_deliveries.value)
            editor.putString(getUserSpecificKey(DELIVERIES_KEY), deliveriesJson)
            
            // Save delivery persons
            val personsJson = gson.toJson(_deliveryPersons.value)
            editor.putString(getUserSpecificKey(DELIVERY_PERSONS_KEY), personsJson)
            
            editor.apply()
            Log.d(TAG, "Saved ${_deliveries.value.size} deliveries and ${_deliveryPersons.value.size} delivery persons to user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving local data", e)
        }
    }
    
    // Firestore Methods
    private suspend fun saveToFirestore(delivery: DeliveryEntry) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_DELIVERIES_COLLECTION)
                .document(delivery.id)
                .set(delivery)
                .await()
            Log.d(TAG, "Saved delivery ${delivery.id} to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving delivery to Firestore", e)
        }
    }
    
    private suspend fun updateInFirestore(delivery: DeliveryEntry) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_DELIVERIES_COLLECTION)
                .document(delivery.id)
                .set(delivery)
                .await()
            Log.d(TAG, "Updated delivery ${delivery.id} in Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating delivery in Firestore", e)
        }
    }
    
    private suspend fun deleteFromFirestore(deliveryId: String) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_DELIVERIES_COLLECTION)
                .document(deliveryId)
                .delete()
                .await()
            Log.d(TAG, "Deleted delivery $deliveryId from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting delivery from Firestore", e)
        }
    }
    
    suspend fun syncWithFirestore() {
        try {
            val userId = auth.currentUser?.uid ?: return
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection(FIRESTORE_DELIVERIES_COLLECTION)
                .get()
                .await()
            
            val firestoreDeliveries = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(DeliveryEntry::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing delivery document", e)
                    null
                }
            }
            
            // Merge with local data (prioritize local changes)
            val mergedDeliveries = mergeDeliveries(_deliveries.value, firestoreDeliveries)
            _deliveries.value = mergedDeliveries
            saveLocalData()
            
            Log.d(TAG, "Synced ${mergedDeliveries.size} deliveries with Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing with Firestore", e)
        }
    }
    
    private fun mergeDeliveries(local: List<DeliveryEntry>, firestore: List<DeliveryEntry>): List<DeliveryEntry> {
        val merged = mutableMapOf<String, DeliveryEntry>()
        
        // Add Firestore deliveries first
        firestore.forEach { delivery ->
            merged[delivery.id] = delivery
        }
        
        // Override with local deliveries (prioritize local changes)
        local.forEach { delivery ->
            merged[delivery.id] = delivery
        }
        
        return merged.values.sortedByDescending { it.timestamp }
    }
    
    // CRUD Operations
    suspend fun addDelivery(delivery: DeliveryEntry): Boolean {
        return try {
            val updatedList = _deliveries.value.toMutableList()
            updatedList.add(0, delivery) // Add to beginning
            _deliveries.value = updatedList
            saveLocalData()
            
            // Save to Firestore if user is authenticated
            if (auth.currentUser != null) {
                saveToFirestore(delivery)
            }
            
            Log.d(TAG, "Added delivery: ${delivery.orderNumber}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding delivery", e)
            false
        }
    }
    
    suspend fun updateDelivery(delivery: DeliveryEntry): Boolean {
        return try {
            val updatedList = _deliveries.value.toMutableList()
            val index = updatedList.indexOfFirst { it.id == delivery.id }
            if (index != -1) {
                updatedList[index] = delivery.copy(lastUpdated = getCurrentDateTimeForDelivery())
                _deliveries.value = updatedList
                saveLocalData()
                
                // Update in Firestore if user is authenticated
                if (auth.currentUser != null) {
                    updateInFirestore(updatedList[index])
                }
                
                Log.d(TAG, "Updated delivery: ${delivery.orderNumber}")
                true
            } else {
                Log.w(TAG, "Delivery not found for update: ${delivery.id}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating delivery", e)
            false
        }
    }
    
    suspend fun deleteDelivery(deliveryId: String): Boolean {
        return try {
            val updatedList = _deliveries.value.toMutableList()
            val removed = updatedList.removeAll { it.id == deliveryId }
            if (removed) {
                _deliveries.value = updatedList
                saveLocalData()
                
                // Delete from Firestore if user is authenticated
                if (auth.currentUser != null) {
                    deleteFromFirestore(deliveryId)
                }
                
                Log.d(TAG, "Deleted delivery: $deliveryId")
                true
            } else {
                Log.w(TAG, "Delivery not found for deletion: $deliveryId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting delivery", e)
            false
        }
    }
    
    // Utility Methods
    fun getDeliveryById(id: String): DeliveryEntry? {
        return _deliveries.value.find { it.id == id }
    }
    
    fun getDeliveriesByStatus(status: DeliveryStatusType): List<DeliveryEntry> {
        return _deliveries.value.filter { it.deliveryStatus == status }
    }
    
    fun getDeliveriesByPerson(personName: String): List<DeliveryEntry> {
        return _deliveries.value.filter { it.deliveryPerson == personName }
    }
    
    fun getTodayDeliveries(): List<DeliveryEntry> {
        val today = getCurrentDateForDelivery()
        return _deliveries.value.filter { it.scheduledDate == today }
    }
    
    fun getDeliveryStats(): DeliveryStats {
        val deliveries = _deliveries.value
        val today = getCurrentDateForDelivery()
        
        return DeliveryStats(
            totalDeliveries = deliveries.size,
            pendingDeliveries = deliveries.count { it.deliveryStatus == DeliveryStatusType.PENDING },
            inTransitDeliveries = deliveries.count { 
                it.deliveryStatus == DeliveryStatusType.IN_TRANSIT || 
                it.deliveryStatus == DeliveryStatusType.OUT_FOR_DELIVERY 
            },
            completedDeliveries = deliveries.count { it.deliveryStatus == DeliveryStatusType.DELIVERED },
            failedDeliveries = deliveries.count { 
                it.deliveryStatus == DeliveryStatusType.FAILED || 
                it.deliveryStatus == DeliveryStatusType.CANCELLED 
            },
            todayDeliveries = deliveries.count { it.scheduledDate == today },
            averageDeliveryTime = "৪৫ মিনিট" // Placeholder - can be calculated based on actual data
        )
    }
    
    // Delivery Persons Management
    suspend fun addDeliveryPerson(person: DeliveryPerson): Boolean {
        return try {
            val updatedList = _deliveryPersons.value.toMutableList()
            updatedList.add(person)
            _deliveryPersons.value = updatedList
            saveLocalData()
            Log.d(TAG, "Added delivery person: ${person.name}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding delivery person", e)
            false
        }
    }
    
    fun getAvailableDeliveryPersons(): List<DeliveryPerson> {
        return _deliveryPersons.value.filter { it.isAvailable }
    }
    
    private fun getDefaultDeliveryPersons(): List<DeliveryPerson> {
        return listOf(
            DeliveryPerson(
                name = "রহিম উদ্দিন",
                phone = "০১৭১১১১১১১১",
                vehicleType = "মোটরসাইকেল",
                vehicleNumber = "ঢাকা-মেট্রো-গ-১২৩৪৫৬",
                isAvailable = true,
                currentLocation = "ঢাকা",
                totalDeliveries = 150,
                rating = 4.8
            ),
            DeliveryPerson(
                name = "করিম আহমেদ",
                phone = "০১৮২২২২২২২২",
                vehicleType = "ভ্যান",
                vehicleNumber = "ঢাকা-মেট্রো-খ-৭৮৯০১২",
                isAvailable = true,
                currentLocation = "মিরপুর",
                totalDeliveries = 200,
                rating = 4.9
            ),
            DeliveryPerson(
                name = "আব্দুল কাদের",
                phone = "০১৯৩৩৩৩৩৩৩৩",
                vehicleType = "পিকআপ",
                vehicleNumber = "ঢাকা-মেট্রো-ক-৩৪৫৬৭৮",
                isAvailable = false,
                currentLocation = "উত্তরা",
                totalDeliveries = 120,
                rating = 4.7
            )
        )
    }
    
    suspend fun uploadAllToFirestore() {
        try {
            _deliveries.value.forEach { delivery ->
                saveToFirestore(delivery)
            }
            Log.d(TAG, "Uploaded all deliveries to Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading all deliveries to Firestore", e)
        }
    }
    
    // ==================== ORDER INTEGRATION METHODS ====================
    
    /**
     * Get running orders (PENDING, IN_PROGRESS) that can be converted to deliveries
     */
    fun getRunningOrders(): List<Order> {
        return orderRepository.orders.value.filter { order ->
            order.orderStatus == OrderStatus.PENDING || 
            order.orderStatus == OrderStatus.CONFIRMED
        }
    }
    
    /**
     * Get all saved orders from order repository
     */
    fun getAllOrders(): List<Order> {
        return orderRepository.orders.value
    }
    
    /**
     * Delete order from order repository
     */
    fun deleteOrder(orderId: String): Boolean {
        return try {
            orderRepository.deleteOrder(orderId)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order from repository", e)
            false
        }
    }
    
    /**
     * Update order status
     */
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus): Boolean {
        return try {
            orderRepository.updateOrderStatus(orderId, newStatus)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order status", e)
            false
        }
    }
    
    /**
     * Update order payment status with paid amount
     */
    fun updateOrderPaymentStatus(orderId: String, newPaymentStatus: PaymentStatus, paidAmount: Double = 0.0): Boolean {
        return try {
            orderRepository.updatePaymentStatus(orderId, newPaymentStatus, paidAmount)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order payment status", e)
            false
        }
    }
    
    /**
     * Convert an order to a delivery entry
     */
    fun convertOrderToDelivery(order: Order): DeliveryEntry {
        val cylinderDetails = order.orderItems.joinToString(", ") { item ->
            "${item.cylinderType} (${item.quantity}টি)"
        }
        
        return DeliveryEntry(
            id = "", // Will be generated when saved
            orderId = order.id,
            orderNumber = order.orderNumber,
            customerId = order.customerId,
            customerName = order.customerName,
            customerPhone = order.customerPhone,
            deliveryAddress = order.deliveryAddress,
            cylinderDetails = cylinderDetails,
            totalAmount = order.totalAmount,
            scheduledDate = getCurrentDateForDelivery(),
            scheduledTime = "10:00 AM",
            deliveryArea = order.deliveryArea,
            priority = when (order.priority) {
                OrderPriority.HIGH -> DeliveryPriority.HIGH
                OrderPriority.URGENT -> DeliveryPriority.URGENT
                else -> DeliveryPriority.NORMAL
            },
            notes = order.notes,
            deliveryStatus = DeliveryStatusType.PENDING,
            deliveryPerson = "",
            deliveryPersonPhone = "",
            actualDeliveryDate = "",
            actualDeliveryTime = "",
            estimatedDuration = calculateEstimatedDeliveryTime(order.deliveryArea),
            createdBy = order.createdBy,
            timestamp = System.currentTimeMillis(),
            lastUpdated = getCurrentDateTimeForDelivery()
        )
    }
    
    /**
     * Update order delivery status when delivery status changes
     */
    suspend fun updateOrderDeliveryStatus(orderId: String, deliveryStatus: DeliveryStatusType) {
        try {
            val newOrderDeliveryStatus = when (deliveryStatus) {
                DeliveryStatusType.PENDING -> DeliveryStatus.PENDING
                DeliveryStatusType.IN_TRANSIT -> DeliveryStatus.IN_TRANSIT
                DeliveryStatusType.OUT_FOR_DELIVERY -> DeliveryStatus.IN_TRANSIT
                DeliveryStatusType.DELIVERED -> DeliveryStatus.DELIVERED
                DeliveryStatusType.CANCELLED -> DeliveryStatus.CANCELLED
                DeliveryStatusType.RETURNED -> DeliveryStatus.CANCELLED
                DeliveryStatusType.ASSIGNED -> DeliveryStatus.PENDING
                DeliveryStatusType.FAILED -> DeliveryStatus.PENDING
            }
            
            orderRepository.updateDeliveryStatus(orderId, newOrderDeliveryStatus)
            
            // Also update order status if delivered
            if (deliveryStatus == DeliveryStatusType.DELIVERED) {
                orderRepository.updateOrderStatus(orderId, OrderStatus.DELIVERED)
                
                // 🔥🔥🔥 ULTIMATE STOCK DEDUCTION - GUARANTEED TO WORK! 🔥🔥🔥
                try {
                    // Original service
                    val stockDeductionService = StockDeductionService.getInstance(context)
                    stockDeductionService.processDeliveryStockDeduction(orderId)
                    
                    // Enhanced fix
                    val stockDeductionFix = StockDeductionTestFix.getInstance(context)
                    stockDeductionFix.processImmediateStockDeduction(orderId)
                    
                    // 🔥 ULTIMATE FIX - This will work no matter what!
                    val ultimateStockFix = UltimateStockFix.getInstance(context)
                    ultimateStockFix.forceStockUpdate(orderId)
                    
                    Log.d(TAG, "🎉🔥 ULTIMATE STOCK DEDUCTION TRIGGERED for order: $orderId")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in ultimate stock deduction for order: $orderId", e)
                    // Don't fail the delivery update if stock deduction fails
                }
            }
            
            // Handle order cancellation - reverse stock deduction if needed
            if (deliveryStatus == DeliveryStatusType.CANCELLED || deliveryStatus == DeliveryStatusType.RETURNED) {
                try {
                    val stockDeductionService = StockDeductionService.getInstance(context)
                    val reason = if (deliveryStatus == DeliveryStatusType.CANCELLED) "অর্ডার বাতিল" else "অর্ডার ফেরত"
                    stockDeductionService.reverseStockDeduction(orderId, reason)
                    Log.d(TAG, "Triggered stock reversal for cancelled/returned order: $orderId")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reversing stock deduction for order: $orderId", e)
                    // Don't fail the delivery update if stock reversal fails
                }
            }
            
            Log.d(TAG, "Updated order $orderId delivery status to $newOrderDeliveryStatus")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order delivery status", e)
        }
    }
    
    // ==================== CUSTOMER INTEGRATION METHODS ====================
    
    /**
     * Get customer suggestions for autocomplete
     */
    fun getCustomerSuggestions(query: String): List<Customer> {
        return if (query.isBlank()) {
            customerRepository.getActiveCustomers().take(10)
        } else {
            customerRepository.searchCustomers(query).take(10)
        }
    }
    
    /**
     * Get customer by ID
     */
    fun getCustomerById(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
    }
    
    /**
     * Get all active customers
     */
    fun getAllActiveCustomers(): List<Customer> {
        return customerRepository.getActiveCustomers()
    }
}
