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
import java.text.SimpleDateFormat
import java.util.*

/**
 * Repository for managing order data using dual storage:
 * - SharedPreferences for local/offline storage
 * - Firebase Firestore for cloud storage and sync
 * 
 * Singleton pattern ensures all ViewModels share the same instance and StateFlow
 */
class OrderRepository private constructor(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("order_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Firebase components
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // StateFlow for reactive UI updates
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _orderStats = MutableStateFlow(OrderStats())
    val orderStats: StateFlow<OrderStats> = _orderStats.asStateFlow()
    
    companion object {
        private const val TAG = "OrderRepository"
        
        @Volatile
        private var INSTANCE: OrderRepository? = null
        
        fun getInstance(context: Context): OrderRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: OrderRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        // Load user-specific data if authenticated, otherwise load empty state
        if (auth.currentUser != null) {
            // Load local data first for immediate availability
            loadOrders()
            
            // Then sync with Firebase in background
            coroutineScope.launch {
                syncWithFirestore()
            }
        } else {
            _orders.value = emptyList()
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
    
    /**
     * Get current user ID for Firestore operations
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get Firestore collection for current user's orders
     */
    private fun getOrdersCollection() = getCurrentUserId()?.let { userId ->
        firestore.collection("users").document(userId).collection("orders")
    }
    
    /**
     * Load orders from SharedPreferences
     */
    private fun loadOrders() {
        try {
            val ordersJson = sharedPreferences.getString(getUserSpecificKey("orders_list"), "[]")
            val type = object : TypeToken<List<Order>>() {}.type
            val ordersList: List<Order> = gson.fromJson(ordersJson, type) ?: emptyList()
            
            _orders.value = ordersList.sortedByDescending { it.orderDate }
            updateStats()
            
            Log.d(TAG, "Loaded ${ordersList.size} orders from user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading orders", e)
            _orders.value = emptyList()
        }
    }
    
    /**
     * Save orders to SharedPreferences
     */
    private fun saveOrders() {
        try {
            val ordersJson = gson.toJson(_orders.value)
            sharedPreferences.edit()
                .putString(getUserSpecificKey("orders_list"), ordersJson)
                .apply()
            
            updateStats()
            Log.d(TAG, "Saved ${_orders.value.size} orders to user-specific storage")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving orders", e)
        }
    }
    
    /**
     * Add a new order (saves to both local and cloud)
     */
    fun addOrder(order: Order): Boolean {
        return try {
            val updatedList = _orders.value.toMutableList()
            updatedList.add(0, order) // Add to beginning for latest first
            _orders.value = updatedList
            saveOrders() // Save locally first
            
            // Save to Firestore asynchronously
            saveToFirestore(order)
            
            Log.d(TAG, "Added new order: ${order.orderNumber}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding order", e)
            false
        }
    }
    
    /**
     * Update an existing order (saves to both local and cloud)
     */
    fun updateOrder(order: Order): Boolean {
        return try {
            val updatedList = _orders.value.toMutableList()
            val index = updatedList.indexOfFirst { it.id == order.id }
            
            if (index != -1) {
                val updatedOrder = order.copy(lastUpdated = getCurrentDateTime())
                updatedList[index] = updatedOrder
                _orders.value = updatedList
                saveOrders() // Save locally first
                
                // Update in Firestore asynchronously
                saveToFirestore(updatedOrder)
                
                Log.d(TAG, "Updated order: ${order.orderNumber}")
                true
            } else {
                Log.w(TAG, "Order not found for update: ${order.id}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating order", e)
            false
        }
    }
    
    /**
     * Delete an order (removes from both local and cloud)
     */
    fun deleteOrder(orderId: String): Boolean {
        return try {
            val updatedList = _orders.value.toMutableList()
            val removed = updatedList.removeIf { it.id == orderId }
            
            if (removed) {
                _orders.value = updatedList
                saveOrders() // Save locally first
                
                // Delete from Firestore asynchronously
                deleteFromFirestore(orderId)
                
                Log.d(TAG, "Deleted order: $orderId")
                true
            } else {
                Log.w(TAG, "Order not found for deletion: $orderId")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting order", e)
            false
        }
    }
    
    /**
     * Get order by ID
     */
    fun getOrderById(orderId: String): Order? {
        return _orders.value.find { it.id == orderId }
    }
    
    /**
     * Get orders by customer ID
     */
    fun getOrdersByCustomerId(customerId: String): List<Order> {
        return _orders.value.filter { it.customerId == customerId }
    }
    
    /**
     * Search orders with filters
     */
    fun searchOrders(filter: OrderFilter): List<Order> {
        var filteredOrders = _orders.value
        
        // Apply search query
        if (filter.searchQuery.isNotBlank()) {
            val query = filter.searchQuery.lowercase()
            filteredOrders = filteredOrders.filter { order ->
                order.orderNumber.lowercase().contains(query) ||
                order.customerName.lowercase().contains(query) ||
                order.customerPhone.contains(query) ||
                order.deliveryArea.lowercase().contains(query)
            }
        }
        
        // Apply status filters
        filter.orderStatus?.let { status ->
            filteredOrders = filteredOrders.filter { it.orderStatus == status }
        }
        
        filter.paymentStatus?.let { status ->
            filteredOrders = filteredOrders.filter { it.paymentStatus == status }
        }
        
        filter.deliveryStatus?.let { status ->
            filteredOrders = filteredOrders.filter { it.deliveryStatus == status }
        }
        
        filter.priority?.let { priority ->
            filteredOrders = filteredOrders.filter { it.priority == priority }
        }
        
        // Apply customer filter
        if (filter.customerId.isNotBlank()) {
            filteredOrders = filteredOrders.filter { it.customerId == filter.customerId }
        }
        
        // Apply area filter
        if (filter.deliveryArea.isNotBlank()) {
            filteredOrders = filteredOrders.filter { 
                it.deliveryArea.lowercase().contains(filter.deliveryArea.lowercase()) 
            }
        }
        
        return filteredOrders
    }
    
    /**
     * Get orders by status
     */
    fun getOrdersByStatus(status: OrderStatus): List<Order> {
        return _orders.value.filter { it.orderStatus == status }
    }
    
    /**
     * Get pending orders
     */
    fun getPendingOrders(): List<Order> {
        return _orders.value.filter { 
            it.orderStatus == OrderStatus.PENDING || 
            it.orderStatus == OrderStatus.CONFIRMED ||
            it.orderStatus == OrderStatus.PROCESSING
        }
    }
    
    /**
     * Get today's orders
     */
    fun getTodaysOrders(): List<Order> {
        val today = getCurrentDate()
        return _orders.value.filter { order ->
            order.orderDate.contains(today.split(",")[0]) // Match date part only
        }
    }
    
    /**
     * Update order status
     */
    fun updateOrderStatus(orderId: String, status: OrderStatus): Boolean {
        val order = getOrderById(orderId)
        return if (order != null) {
            val updatedOrder = order.copy(
                orderStatus = status,
                completedDate = if (status == OrderStatus.DELIVERED) getCurrentDateTime() else order.completedDate,
                lastUpdated = getCurrentDateTime()
            )
            updateOrder(updatedOrder)
        } else {
            false
        }
    }
    
    /**
     * Update payment status
     */
    fun updatePaymentStatus(orderId: String, paymentStatus: PaymentStatus, paidAmount: Double = 0.0): Boolean {
        val order = getOrderById(orderId)
        return if (order != null) {
            val newPaidAmount = if (paidAmount > 0) paidAmount else order.paidAmount
            val updatedOrder = order.copy(
                paymentStatus = paymentStatus,
                paidAmount = newPaidAmount,
                remainingAmount = order.totalAmount - newPaidAmount,
                lastUpdated = getCurrentDateTime()
            )
            updateOrder(updatedOrder)
        } else {
            false
        }
    }
    
    /**
     * Update delivery status
     */
    fun updateDeliveryStatus(orderId: String, deliveryStatus: DeliveryStatus, deliveryDate: String = ""): Boolean {
        val order = getOrderById(orderId)
        return if (order != null) {
            val updatedOrder = order.copy(
                deliveryStatus = deliveryStatus,
                deliveryDate = if (deliveryDate.isNotEmpty()) deliveryDate else order.deliveryDate,
                lastUpdated = getCurrentDateTime()
            )
            val success = updateOrder(updatedOrder)
            
            // Automatically deduct stock when order is delivered
            if (success && deliveryStatus == DeliveryStatus.DELIVERED) {
                try {
                    val stockDeductionService = StockDeductionService.getInstance(context)
                    stockDeductionService.processDeliveryStockDeduction(orderId)
                    Log.d("OrderRepository", "Stock deduction triggered for delivered order: $orderId")
                } catch (e: Exception) {
                    Log.e("OrderRepository", "Failed to trigger stock deduction for order: $orderId", e)
                    // Don't fail the delivery status update if stock deduction fails
                }
            }
            
            success
        } else {
            false
        }
    }
    
    /**
     * Update order statistics
     */
    private fun updateStats() {
        val orders = _orders.value
        val today = getCurrentDate()
        val thisWeek = getThisWeekStart()
        val thisMonth = getThisMonthStart()
        
        val stats = OrderStats(
            totalOrders = orders.size,
            pendingOrders = orders.count { 
                it.orderStatus == OrderStatus.PENDING || 
                it.orderStatus == OrderStatus.CONFIRMED ||
                it.orderStatus == OrderStatus.PROCESSING
            },
            confirmedOrders = orders.count { it.orderStatus == OrderStatus.CONFIRMED },
            deliveredOrders = orders.count { it.orderStatus == OrderStatus.DELIVERED },
            cancelledOrders = orders.count { it.orderStatus == OrderStatus.CANCELLED },
            totalRevenue = orders.filter { it.orderStatus == OrderStatus.DELIVERED }.sumOf { it.totalAmount },
            pendingPayments = orders.filter { it.paymentStatus != PaymentStatus.PAID }.sumOf { it.remainingAmount },
            todaysOrders = orders.count { it.orderDate.contains(today.split(",")[0]) },
            thisWeekOrders = orders.count { isOrderFromThisWeek(it.orderDate, thisWeek) },
            thisMonthOrders = orders.count { isOrderFromThisMonth(it.orderDate, thisMonth) }
        )
        _orderStats.value = stats
    }
    
    /**
     * Add sample orders for testing
     */
    fun addSampleOrders() {
        if (_orders.value.isNotEmpty()) return
        
        val sampleOrders = listOf(
            Order(
                customerId = "sample-customer-1",
                customerName = "মোহাম্মদ রহিম",
                customerPhone = "01712345678",
                deliveryAddress = "বাড়ি নং ১২, রোড নং ৫, ধানমন্ডি",
                deliveryArea = "ধানমন্ডি",
                orderItems = listOf(
                    OrderItem(
                        cylinderType = CylinderType.KG_12,
                        quantity = 2,
                        unitPrice = 1200.0,
                        totalPrice = 2400.0
                    )
                ),
                totalAmount = 2400.0,
                paidAmount = 1200.0,
                remainingAmount = 1200.0,
                orderStatus = OrderStatus.CONFIRMED,
                paymentStatus = PaymentStatus.PARTIAL,
                deliveryStatus = DeliveryStatus.PENDING,
                priority = OrderPriority.NORMAL,
                notes = "সকাল ১০টার মধ্যে ডেলিভারি দিতে হবে"
            ),
            Order(
                customerId = "sample-customer-2",
                customerName = "ফাতেমা খাতুন",
                customerPhone = "01823456789",
                deliveryAddress = "প্লট নং ৮, ব্লক বি, গুলশান",
                deliveryArea = "গুলশান",
                orderItems = listOf(
                    OrderItem(
                        cylinderType = CylinderType.KG_12,
                        quantity = 1,
                        unitPrice = 1200.0,
                        totalPrice = 1200.0
                    ),
                    OrderItem(
                        cylinderType = CylinderType.KG_25,
                        quantity = 1,
                        unitPrice = 600.0,
                        totalPrice = 600.0
                    )
                ),
                totalAmount = 1800.0,
                paidAmount = 1800.0,
                remainingAmount = 0.0,
                orderStatus = OrderStatus.DELIVERED,
                paymentStatus = PaymentStatus.PAID,
                deliveryStatus = DeliveryStatus.DELIVERED,
                priority = OrderPriority.HIGH,
                completedDate = getCurrentDateTime()
            ),
            Order(
                customerId = "sample-customer-3",
                customerName = "আহমেদ এন্টারপ্রাইজ",
                customerPhone = "01934567890",
                deliveryAddress = "শপ নং ১৫, কমার্শিয়াল এরিয়া, মতিঝিল",
                deliveryArea = "মতিঝিল",
                orderItems = listOf(
                    OrderItem(
                        cylinderType = CylinderType.KG_12,
                        quantity = 5,
                        unitPrice = 3500.0,
                        totalPrice = 17500.0
                    )
                ),
                totalAmount = 17500.0,
                paidAmount = 0.0,
                remainingAmount = 17500.0,
                orderStatus = OrderStatus.PROCESSING,
                paymentStatus = PaymentStatus.PENDING,
                deliveryStatus = DeliveryStatus.PENDING,
                priority = OrderPriority.URGENT,
                notes = "বাল্ক অর্ডার - বিশেষ ছাড় প্রযোজ্য"
            )
        )
        
        sampleOrders.forEach { order ->
            addOrder(order)
        }
        
        Log.d(TAG, "Added sample orders for testing")
    }
    
    /**
     * Clear all orders (for testing purposes)
     */
    fun clearAllOrders() {
        _orders.value = emptyList()
        saveOrders()
        Log.d(TAG, "Cleared all orders")
    }
    
    // Helper functions
    private fun getCurrentDateTime(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD"))
        return sdf.format(Date())
    }
    
    private fun getThisWeekStart(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
        return sdf.format(calendar.time)
    }
    
    private fun getThisMonthStart(): String {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
        return sdf.format(calendar.time)
    }
    
    private fun isOrderFromThisWeek(orderDate: String, weekStart: String): Boolean {
        // Simple date comparison - can be enhanced with proper date parsing
        return orderDate.contains(getCurrentDate().split(" ")[1]) // Same month check
    }
    
    private fun isOrderFromThisMonth(orderDate: String, monthStart: String): Boolean {
        // Simple date comparison - can be enhanced with proper date parsing
        return orderDate.contains(getCurrentDate().split(" ")[1]) // Same month check
    }
    
    // ==================== FIREBASE FIRESTORE METHODS ====================
    
    /**
     * Save order to Firestore
     */
    private fun saveToFirestore(order: Order) {
        coroutineScope.launch {
            try {
                getOrdersCollection()?.document(order.id)?.set(order)?.await()
                Log.d(TAG, "Order saved to Firestore: ${order.orderNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving order to Firestore: ${order.orderNumber}", e)
            }
        }
    }
    
    /**
     * Delete order from Firestore
     */
    private fun deleteFromFirestore(orderId: String) {
        coroutineScope.launch {
            try {
                getOrdersCollection()?.document(orderId)?.delete()?.await()
                Log.d(TAG, "Order deleted from Firestore: $orderId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting order from Firestore: $orderId", e)
            }
        }
    }
    
    /**
     * Sync with Firestore - load orders from cloud and merge with local data
     */
    internal fun syncWithFirestore() {
        coroutineScope.launch {
            try {
                val collection = getOrdersCollection()
                if (collection != null) {
                    val snapshot = collection
                        .orderBy("orderDate", Query.Direction.DESCENDING)
                        .get()
                        .await()
                    
                    val firestoreOrders = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Order::class.java)
                    }
                    
                    // Merge with local data (prioritize local changes)
                    val localOrders = _orders.value
                    val mergedOrders = mutableListOf<Order>()
                    
                    // Add all local orders first
                    mergedOrders.addAll(localOrders)
                    
                    // Add Firestore orders that don't exist locally
                    firestoreOrders.forEach { firestoreOrder ->
                        if (localOrders.none { it.id == firestoreOrder.id }) {
                            mergedOrders.add(firestoreOrder)
                        }
                    }
                    
                    // Sort by date and update state
                    val sortedOrders = mergedOrders.sortedByDescending { it.orderDate }
                    _orders.value = sortedOrders
                    
                    // Save merged data locally
                    saveOrders()
                    
                    Log.d(TAG, "Synced with Firestore: ${firestoreOrders.size} cloud orders, ${sortedOrders.size} total orders")
                } else {
                    Log.w(TAG, "Cannot sync with Firestore: User not authenticated")
                }
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
     * Upload all local orders to Firestore (useful for initial sync)
     */
    fun uploadAllToFirestore() {
        coroutineScope.launch {
            try {
                val collection = getOrdersCollection()
                if (collection != null) {
                    _orders.value.forEach { order ->
                        collection.document(order.id).set(order).await()
                    }
                    Log.d(TAG, "Uploaded all ${_orders.value.size} orders to Firestore")
                } else {
                    Log.w(TAG, "Cannot upload: User not authenticated")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading orders to Firestore", e)
            }
        }
    }
}
