package com.firebase.loginauth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for managing order data and UI state
 * Follows MVVM pattern consistent with existing ViewModels
 */
class OrderViewModel(application: Application) : AndroidViewModel(application) {
    
    private val orderRepository = OrderRepository.getInstance(application)
    private val customerRepository = CustomerRepository(application)
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _orderFilter = MutableStateFlow(OrderFilter())
    val orderFilter: StateFlow<OrderFilter> = _orderFilter.asStateFlow()
    
    private val _filteredOrders = MutableStateFlow<List<Order>>(emptyList())
    val filteredOrders: StateFlow<List<Order>> = _filteredOrders.asStateFlow()
    
    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()
    
    private val _availableCustomers = MutableStateFlow<List<Customer>>(emptyList())
    val availableCustomers: StateFlow<List<Customer>> = _availableCustomers.asStateFlow()
    
    // Repository data
    val orders = orderRepository.orders
    val orderStats = orderRepository.orderStats
    val customers = customerRepository.customers
    
    companion object {
        private const val TAG = "OrderViewModel"
    }
    
    init {
        // Initialize with all orders
        viewModelScope.launch {
            orders.collect { orderList ->
                applyFilters(orderList)
            }
        }
        
        // Load customers for order creation
        viewModelScope.launch {
            customers.collect { customerList ->
                _availableCustomers.value = customerList.filter { it.isActive }
            }
        }
        
        // Removed automatic sample data injection for production
        // addSampleOrdersIfEmpty()
    }
    
    /**
     * Create a new order (overloaded for direct Order object - used for sales reports)
     */
    fun createOrder(
        order: Order,
        customerId: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = orderRepository.addOrder(order)
                if (success) {
                    _successMessage.value = "বিক্রির হিসাব সফলভাবে সংরক্ষিত হয়েছে"
                    Log.d(TAG, "Sales report created successfully: ${order.orderNumber}")
                    
                    // Log activity for Recent Activity panel
                    ActivityLogger.logOrderCreate(
                        customerName = order.customerName,
                        orderValue = order.totalAmount,
                        cylinderType = "বিক্রির হিসাব"
                    )
                } else {
                    _errorMessage.value = "বিক্রির হিসাব সংরক্ষণ করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "বিক্রির হিসাব সংরক্ষণ করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error creating sales report", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new order with customer details (enhanced method)
     */
    fun createOrderWithCustomerDetails(
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        orderItems: List<OrderItem>,
        priority: OrderPriority = OrderPriority.NORMAL,
        estimatedDeliveryTime: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Validate input
                if (customerName.isBlank()) {
                    _errorMessage.value = "গ্রাহকের নাম প্রয়োজন"
                    return@launch
                }
                
                if (customerPhone.isBlank()) {
                    _errorMessage.value = "গ্রাহকের ফোন নম্বর প্রয়োজন"
                    return@launch
                }
                
                if (orderItems.isEmpty()) {
                    _errorMessage.value = "অন্তত একটি পণ্য যোগ করুন"
                    return@launch
                }
                
                if (deliveryAddress.isBlank()) {
                    _errorMessage.value = "ডেলিভারি ঠিকানা প্রয়োজন"
                    return@launch
                }
                
                // Find or create customer
                var customer = customerRepository.getCustomerByPhone(customerPhone.trim())
                
                if (customer == null) {
                    // Create new customer
                    val newCustomer = Customer(
                        name = customerName.trim(),
                        phone = customerPhone.trim(),
                        address = deliveryAddress.trim(),
                        area = "" // Default area
                    )
                    
                    // Add customer to repository
                    val customerAdded = customerRepository.addCustomer(newCustomer)
                    if (customerAdded) {
                        customer = newCustomer
                        Log.d(TAG, "New customer created: ${newCustomer.name} (${newCustomer.id})")
                    } else {
                        _errorMessage.value = "গ্রাহক তৈরি করতে সমস্যা হয়েছে"
                        return@launch
                    }
                } else {
                    Log.d(TAG, "Existing customer found: ${customer.name} (${customer.id})")
                }
                
                // Calculate total amount
                val totalAmount = calculateOrderTotal(orderItems)
                
                val order = Order(
                    customerId = customer.id,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    deliveryAddress = deliveryAddress.trim(),
                    deliveryArea = customer.area,
                    orderItems = orderItems,
                    totalAmount = totalAmount,
                    remainingAmount = totalAmount,
                    priority = priority,
                    estimatedDeliveryTime = estimatedDeliveryTime.trim(),
                    notes = notes.trim()
                )
                
                val success = orderRepository.addOrder(order)
                if (success) {
                    _successMessage.value = "অর্ডার সফলভাবে তৈরি হয়েছে"
                    Log.d(TAG, "Order created successfully: ${order.orderNumber}")
                    
                    // Log activity for Recent Activity panel
                    ActivityLogger.logOrderCreate(
                        customerName = customer.name,
                        orderValue = totalAmount,
                        cylinderType = "${orderItems.sumOf { it.quantity }} টি সিলিন্ডার"
                    )
                } else {
                    _errorMessage.value = "অর্ডার তৈরি করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "অর্ডার তৈরি করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error creating order with customer details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Create a new order (original method for backward compatibility)
     */
    fun createOrder(
        customerId: String,
        deliveryAddress: String,
        orderItems: List<OrderItem>,
        priority: OrderPriority = OrderPriority.NORMAL,
        estimatedDeliveryTime: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Validate input
                if (customerId.isBlank()) {
                    _errorMessage.value = "গ্রাহক নির্বাচন করুন"
                    return@launch
                }
                
                if (orderItems.isEmpty()) {
                    _errorMessage.value = "অন্তত একটি পণ্য যোগ করুন"
                    return@launch
                }
                
                if (deliveryAddress.isBlank()) {
                    _errorMessage.value = "ডেলিভারি ঠিকানা প্রয়োজন"
                    return@launch
                }
                
                // Get customer details with retry logic
                var customer = customerRepository.getCustomerById(customerId)
                
                // If customer not found, try to refresh and search again
                if (customer == null) {
                    Log.w(TAG, "Customer not found with ID: $customerId, refreshing customer list...")
                    customerRepository.refreshCustomers()
                    
                    // Wait a bit and try again
                    kotlinx.coroutines.delay(500)
                    customer = customerRepository.getCustomerById(customerId)
                }
                
                if (customer == null) {
                    _errorMessage.value = "গ্রাহক খুঁজে পাওয়া যায়নি। দয়া করে গ্রাহকের তথ্য পুনরায় যাচাই করুন।"
                    Log.e(TAG, "Customer still not found after refresh. Customer ID: $customerId")
                    return@launch
                }
                
                // Calculate total amount
                val totalAmount = calculateOrderTotal(orderItems)
                
                val order = Order(
                    customerId = customerId,
                    customerName = customer.name,
                    customerPhone = customer.phone,
                    deliveryAddress = deliveryAddress.trim(),
                    deliveryArea = customer.area,
                    orderItems = orderItems,
                    totalAmount = totalAmount,
                    remainingAmount = totalAmount,
                    priority = priority,
                    estimatedDeliveryTime = estimatedDeliveryTime.trim(),
                    notes = notes.trim()
                )
                
                val success = orderRepository.addOrder(order)
                if (success) {
                    _successMessage.value = "অর্ডার সফলভাবে তৈরি হয়েছে"
                    Log.d(TAG, "Order created successfully: ${order.orderNumber}")
                    
                    // Log activity for Recent Activity panel
                    ActivityLogger.logOrderCreate(
                        customerName = customer.name,
                        orderValue = totalAmount,
                        cylinderType = "${orderItems.sumOf { it.quantity }} টি সিলিন্ডার"
                    )
                } else {
                    _errorMessage.value = "অর্ডার তৈরি করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "অর্ডার তৈরি করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error creating order", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update an existing order
     */
    fun updateOrder(order: Order) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = orderRepository.updateOrder(order)
                if (success) {
                    _successMessage.value = "অর্ডার আপডেট করা হয়েছে"
                    Log.d(TAG, "Order updated successfully: ${order.orderNumber}")
                } else {
                    _errorMessage.value = "অর্ডার আপডেট করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "অর্ডার আপডেট করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error updating order", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete an order
     */
    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = orderRepository.deleteOrder(orderId)
                if (success) {
                    _successMessage.value = "অর্ডার মুছে ফেলা হয়েছে"
                    Log.d(TAG, "Order deleted successfully: $orderId")
                } else {
                    _errorMessage.value = "অর্ডার মুছতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "অর্ডার মুছতে সমস্যা হয়েছে"
                Log.e(TAG, "Error deleting order", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update order status
     */
    fun updateOrderStatus(orderId: String, status: OrderStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = orderRepository.updateOrderStatus(orderId, status)
                if (success) {
                    _successMessage.value = "অর্ডারের অবস্থা আপডেট করা হয়েছে"
                    Log.d(TAG, "Order status updated: $orderId -> $status")
                } else {
                    _errorMessage.value = "অর্ডারের অবস্থা আপডেট করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "অর্ডারের অবস্থা আপডেট করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error updating order status", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update payment status
     */
    fun updatePaymentStatus(orderId: String, paymentStatus: PaymentStatus, paidAmount: Double = 0.0) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = orderRepository.updatePaymentStatus(orderId, paymentStatus, paidAmount)
                if (success) {
                    _successMessage.value = "পেমেন্ট স্ট্যাটাস আপডেট করা হয়েছে"
                    Log.d(TAG, "Payment status updated: $orderId -> $paymentStatus with amount: $paidAmount")
                } else {
                    _errorMessage.value = "পেমেন্ট স্ট্যাটাস আপডেট করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "পেমেন্ট স্ট্যাটাস আপডেট করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error updating payment status", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update delivery status
     */
    fun updateDeliveryStatus(orderId: String, deliveryStatus: DeliveryStatus, deliveryDate: String = "") {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = orderRepository.updateDeliveryStatus(orderId, deliveryStatus, deliveryDate)
                if (success) {
                    _successMessage.value = "ডেলিভারি স্ট্যাটাস আপডেট করা হয়েছে"
                    Log.d(TAG, "Delivery status updated: $orderId -> $deliveryStatus")
                } else {
                    _errorMessage.value = "ডেলিভারি স্ট্যাটাস আপডেট করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "ডেলিভারি স্ট্যাটাস আপডেট করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error updating delivery status", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Apply filters to order list
     */
    fun applyFilter(filter: OrderFilter) {
        _orderFilter.value = filter
        applyFilters(orders.value)
    }
    
    /**
     * Search orders
     */
    fun searchOrders(query: String) {
        val currentFilter = _orderFilter.value.copy(searchQuery = query)
        _orderFilter.value = currentFilter
        applyFilters(orders.value)
    }
    
    /**
     * Filter orders by status
     */
    fun filterByStatus(status: OrderStatus?) {
        val currentFilter = _orderFilter.value.copy(orderStatus = status)
        _orderFilter.value = currentFilter
        applyFilters(orders.value)
    }
    
    /**
     * Filter orders by priority
     */
    fun filterByPriority(priority: OrderPriority?) {
        val currentFilter = _orderFilter.value.copy(priority = priority)
        _orderFilter.value = currentFilter
        applyFilters(orders.value)
    }
    
    /**
     * Clear all filters
     */
    fun clearFilters() {
        _orderFilter.value = OrderFilter()
        _filteredOrders.value = orders.value
    }
    
    /**
     * Apply search and filter to order list
     */
    private fun applyFilters(orderList: List<Order>) {
        val filtered = orderRepository.searchOrders(_orderFilter.value)
        _filteredOrders.value = filtered
    }
    
    /**
     * Select an order for viewing/editing
     */
    fun selectOrder(order: Order?) {
        _selectedOrder.value = order
    }
    
    /**
     * Get order by ID
     */
    fun getOrderById(orderId: String): Order? {
        return orderRepository.getOrderById(orderId)
    }
    
    /**
     * Get orders by customer
     */
    fun getOrdersByCustomer(customerId: String): List<Order> {
        return orderRepository.getOrdersByCustomerId(customerId)
    }
    
    /**
     * Get pending orders
     */
    fun getPendingOrders(): List<Order> {
        return orderRepository.getPendingOrders()
    }
    
    /**
     * Get today's orders
     */
    fun getTodaysOrders(): List<Order> {
        return orderRepository.getTodaysOrders()
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    /**
     * Refresh order data
     */
    fun refreshOrders() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Force reload from repository
                applyFilters(orders.value)
                Log.d(TAG, "Order data refreshed")
            } catch (e: Exception) {
                _errorMessage.value = "ডেটা রিফ্রেশ করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error refreshing orders", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Add sample orders if the list is empty (for testing)
     */
    private fun addSampleOrdersIfEmpty() {
        viewModelScope.launch {
            if (orders.value.isEmpty()) {
                orderRepository.addSampleOrders()
                Log.d(TAG, "Added sample orders for testing")
            }
        }
    }
    
    /**
     * Calculate order item total
     */
    fun calculateItemTotal(quantity: Int, unitPrice: Double): Double {
        return quantity * unitPrice
    }
    
    /**
     * Validate order item
     */
    fun validateOrderItem(cylinderType: CylinderType?, quantity: String, unitPrice: String): String? {
        if (cylinderType == null) {
            return "সিলিন্ডারের ধরন নির্বাচন করুন"
        }
        
        val qty = quantity.toIntOrNull()
        if (qty == null || qty <= 0) {
            return "সঠিক পরিমাণ দিন"
        }
        
        val price = unitPrice.toDoubleOrNull()
        if (price == null || price <= 0) {
            return "সঠিক দাম দিন"
        }
        
        return null // No error
    }
    
    /**
     * Get customer by ID
     */
    fun getCustomerById(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
    }
}
