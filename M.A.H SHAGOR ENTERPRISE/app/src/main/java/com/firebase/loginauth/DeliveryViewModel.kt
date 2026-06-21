package com.firebase.loginauth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DeliveryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = DeliveryRepository(application.applicationContext)
    
    // StateFlow for deliveries
    val deliveries: StateFlow<List<DeliveryEntry>> = repository.deliveries
    
    // StateFlow for delivery persons
    val deliveryPersons: StateFlow<List<DeliveryPerson>> = repository.deliveryPersons
    
    // StateFlow for save status
    private val _saveStatus = MutableStateFlow<DeliverySaveStatus>(DeliverySaveStatus.Idle)
    val saveStatus: StateFlow<DeliverySaveStatus> = _saveStatus.asStateFlow()
    
    // StateFlow for delivery stats
    private val _deliveryStats = MutableStateFlow(DeliveryStats())
    val deliveryStats: StateFlow<DeliveryStats> = _deliveryStats.asStateFlow()
    
    // StateFlow for filtered deliveries
    private val _filteredDeliveries = MutableStateFlow<List<DeliveryEntry>>(emptyList())
    val filteredDeliveries: StateFlow<List<DeliveryEntry>> = _filteredDeliveries.asStateFlow()
    
    // StateFlow for current filter
    private val _currentFilter = MutableStateFlow(DeliveryFilter())
    val currentFilter: StateFlow<DeliveryFilter> = _currentFilter.asStateFlow()
    
    // StateFlow for running orders (real-time sync from OrderRepository)
    val runningOrders: StateFlow<List<Order>> = repository.runningOrders
    
    // StateFlow for all saved orders (real-time sync from OrderRepository)
    val allOrders: StateFlow<List<Order>> = repository.allOrders
    
    // StateFlow for customer suggestions
    private val _customerSuggestions = MutableStateFlow<List<Customer>>(emptyList())
    val customerSuggestions: StateFlow<List<Customer>> = _customerSuggestions.asStateFlow()
    
    companion object {
        private const val TAG = "DeliveryViewModel"
    }
    
    init {
        // Initialize data
        viewModelScope.launch {
            updateDeliveryStats()
            _filteredDeliveries.value = deliveries.value
            // loadRunningOrders() removed - now using real-time StateFlow
            // loadAllOrders() removed - now using real-time StateFlow
        }
        
        // Observe deliveries changes to update stats
        viewModelScope.launch {
            deliveries.collect { deliveriesList ->
                updateDeliveryStats()
                applyCurrentFilter()
            }
        }
    }
    
    // Add new delivery
    fun addDelivery(
        orderId: String,
        orderNumber: String,
        customerId: String,
        customerName: String,
        customerPhone: String,
        deliveryAddress: String,
        cylinderDetails: String,
        totalAmount: Double,
        scheduledDate: String,
        scheduledTime: String,
        deliveryArea: String,
        priority: DeliveryPriority = DeliveryPriority.NORMAL,
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val delivery = DeliveryEntry(
                    orderId = orderId,
                    orderNumber = orderNumber,
                    customerId = customerId,
                    customerName = customerName,
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    cylinderDetails = cylinderDetails,
                    totalAmount = totalAmount,
                    scheduledDate = scheduledDate,
                    scheduledTime = scheduledTime,
                    deliveryArea = deliveryArea,
                    priority = priority,
                    notes = notes,
                    estimatedDuration = calculateEstimatedDeliveryTime(deliveryArea)
                )
                
                val success = repository.addDelivery(delivery)
                
                if (success) {
                    _saveStatus.value = DeliverySaveStatus.Success("ডেলিভারি সফলভাবে যোগ করা হয়েছে")
                    Log.d(TAG, "Delivery added successfully: ${delivery.orderNumber}")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি যোগ করতে ব্যর্থ")
                    Log.e(TAG, "Failed to add delivery")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error adding delivery", e)
            }
        }
    }
    
    // Update delivery status
    fun updateDeliveryStatus(
        deliveryId: String,
        newStatus: DeliveryStatusType,
        deliveryPerson: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val delivery = deliveries.value.find { it.id == deliveryId }
                if (delivery != null) {
                    val updatedDelivery = delivery.copy(
                        deliveryStatus = newStatus,
                        deliveryPerson = if (deliveryPerson.isNotEmpty()) deliveryPerson else delivery.deliveryPerson,
                        notes = if (notes.isNotEmpty()) notes else delivery.notes,
                        actualDeliveryDate = if (newStatus == DeliveryStatusType.DELIVERED) getCurrentDateForDelivery() else delivery.actualDeliveryDate,
                        actualDeliveryTime = if (newStatus == DeliveryStatusType.DELIVERED) getCurrentDateTimeForDelivery() else delivery.actualDeliveryTime,
                        lastUpdated = getCurrentDateTimeForDelivery()
                    )
                    
                    val success = repository.updateDelivery(updatedDelivery)
                    
                    if (success) {
                        // Also update the associated order status
                        if (delivery.orderId.isNotEmpty()) {
                            updateOrderDeliveryStatus(delivery.orderId, newStatus)
                        }
                        
                        _saveStatus.value = DeliverySaveStatus.Success("ডেলিভারি স্ট্যাটাস আপডেট করা হয়েছে")
                        Log.d(TAG, "Delivery status updated: $deliveryId -> $newStatus")
                    } else {
                        _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি স্ট্যাটাস আপডেট করতে ব্যর্থ")
                        Log.e(TAG, "Failed to update delivery status")
                    }
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি খুঁজে পাওয়া যায়নি")
                    Log.e(TAG, "Delivery not found: $deliveryId")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error updating delivery status", e)
            }
        }
    }
    
    // Assign delivery person
    fun assignDeliveryPerson(deliveryId: String, personName: String, personPhone: String) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val existingDelivery = repository.getDeliveryById(deliveryId)
                if (existingDelivery != null) {
                    val updatedDelivery = existingDelivery.copy(
                        deliveryPerson = personName,
                        deliveryPersonPhone = personPhone,
                        deliveryStatus = if (existingDelivery.deliveryStatus == DeliveryStatusType.PENDING) 
                            DeliveryStatusType.ASSIGNED else existingDelivery.deliveryStatus,
                        lastUpdated = getCurrentDateTimeForDelivery()
                    )
                    
                    val success = repository.updateDelivery(updatedDelivery)
                    
                    if (success) {
                        _saveStatus.value = DeliverySaveStatus.Success("ডেলিভারি পার্সন নিযুক্ত হয়েছে")
                        Log.d(TAG, "Delivery person assigned: ${updatedDelivery.orderNumber}")
                    } else {
                        _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি পার্সন নিযুক্ত করতে ব্যর্থ")
                        Log.e(TAG, "Failed to assign delivery person")
                    }
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি খুঁজে পাওয়া যায়নি")
                    Log.e(TAG, "Delivery not found for person assignment: $deliveryId")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error assigning delivery person", e)
            }
        }
    }
    
    // Edit delivery
    fun editDelivery(delivery: DeliveryEntry) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val success = repository.updateDelivery(delivery)
                
                if (success) {
                    _saveStatus.value = DeliverySaveStatus.Success("ডেলিভারি সম্পাদনা সফল")
                    Log.d(TAG, "Delivery edited successfully: ${delivery.orderNumber}")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি সম্পাদনা ব্যর্থ")
                    Log.e(TAG, "Failed to edit delivery")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error editing delivery", e)
            }
        }
    }
    
    // Delete delivery
    fun deleteDelivery(deliveryId: String) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val success = repository.deleteDelivery(deliveryId)
                
                if (success) {
                    _saveStatus.value = DeliverySaveStatus.Success("ডেলিভারি মুছে ফেলা হয়েছে")
                    Log.d(TAG, "Delivery deleted successfully: $deliveryId")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি মুছতে ব্যর্থ")
                    Log.e(TAG, "Failed to delete delivery")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error deleting delivery", e)
            }
        }
    }
    
    // Filter deliveries
    fun filterDeliveries(filter: DeliveryFilter) {
        viewModelScope.launch {
            _currentFilter.value = filter
            applyCurrentFilter()
        }
    }
    
    private fun applyCurrentFilter() {
        val filter = _currentFilter.value
        var filtered = deliveries.value
        
        // Filter by status
        filter.status?.let { status ->
            filtered = filtered.filter { it.deliveryStatus == status }
        }
        
        // Filter by delivery person
        if (filter.deliveryPerson.isNotEmpty()) {
            filtered = filtered.filter { 
                it.deliveryPerson.contains(filter.deliveryPerson, ignoreCase = true) 
            }
        }
        
        // Filter by area
        if (filter.area.isNotEmpty()) {
            filtered = filtered.filter { 
                it.deliveryArea.contains(filter.area, ignoreCase = true) 
            }
        }
        
        // Filter by priority
        filter.priority?.let { priority ->
            filtered = filtered.filter { it.priority == priority }
        }
        
        // Filter by date range
        if (filter.dateFrom.isNotEmpty()) {
            filtered = filtered.filter { it.scheduledDate >= filter.dateFrom }
        }
        
        if (filter.dateTo.isNotEmpty()) {
            filtered = filtered.filter { it.scheduledDate <= filter.dateTo }
        }
        
        _filteredDeliveries.value = filtered
    }
    
    // Clear filters
    fun clearFilters() {
        viewModelScope.launch {
            _currentFilter.value = DeliveryFilter()
            _filteredDeliveries.value = deliveries.value
        }
    }
    
    // Get deliveries by status
    fun getDeliveriesByStatus(status: DeliveryStatusType): List<DeliveryEntry> {
        return repository.getDeliveriesByStatus(status)
    }
    
    // Get today's deliveries
    fun getTodayDeliveries(): List<DeliveryEntry> {
        return repository.getTodayDeliveries()
    }
    
    // Update delivery stats
    private fun updateDeliveryStats() {
        _deliveryStats.value = repository.getDeliveryStats()
    }
    
    // Get available delivery persons
    fun getAvailableDeliveryPersons(): List<DeliveryPerson> {
        return repository.getAvailableDeliveryPersons()
    }
    
    // Add delivery person
    fun addDeliveryPerson(person: DeliveryPerson) {
        viewModelScope.launch {
            try {
                val success = repository.addDeliveryPerson(person)
                if (success) {
                    _saveStatus.value = DeliverySaveStatus.Success("ডেলিভারি পার্সন যোগ করা হয়েছে")
                    Log.d(TAG, "Delivery person added: ${person.name}")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("ডেলিভারি পার্সন যোগ করতে ব্যর্থ")
                    Log.e(TAG, "Failed to add delivery person")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error adding delivery person", e)
            }
        }
    }
    
    // Reset save status
    fun resetSaveStatus() {
        _saveStatus.value = DeliverySaveStatus.Idle
    }
    
    // Sync with cloud
    fun syncWithCloud() {
        viewModelScope.launch {
            try {
                repository.syncWithFirestore()
                Log.d(TAG, "Synced with cloud successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing with cloud", e)
            }
        }
    }
    
    // ==================== ORDER INTEGRATION METHODS ====================
    
    /**
     * Load running orders from order management
     * @deprecated This function is no longer needed as we use real-time StateFlow
     */
    @Deprecated("Use runningOrders StateFlow for real-time updates")
    fun loadRunningOrders() {
        // No longer needed - runningOrders StateFlow provides real-time updates
        Log.d(TAG, "loadRunningOrders() called but using real-time StateFlow instead")
    }
    
    /**
     * Load all saved orders from order management
     * @deprecated This function is no longer needed as we use real-time StateFlow
     */
    @Deprecated("Use allOrders StateFlow for real-time updates")
    fun loadAllOrders() {
        // No longer needed - allOrders StateFlow provides real-time updates
        Log.d(TAG, "loadAllOrders() called but using real-time StateFlow instead")
    }
    
    /**
     * Convert order to delivery and add to delivery list
     */
    fun convertOrderToDelivery(order: Order) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val deliveryEntry = repository.convertOrderToDelivery(order)
                val success = repository.addDelivery(deliveryEntry)
                
                if (success) {
                    _saveStatus.value = DeliverySaveStatus.Success("অর্ডার সফলভাবে ডেলিভারিতে রূপান্তরিত হয়েছে")
                    Log.d(TAG, "Order converted to delivery: ${order.orderNumber}")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("অর্ডার ডেলিভারিতে রূপান্তর করতে ব্যর্থ")
                    Log.e(TAG, "Failed to convert order to delivery")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error converting order to delivery", e)
            }
        }
    }
    
    /**
     * Update order status when delivery status changes
     */
    fun updateOrderDeliveryStatus(orderId: String, deliveryStatus: DeliveryStatusType) {
        viewModelScope.launch {
            try {
                repository.updateOrderDeliveryStatus(orderId, deliveryStatus)
                Log.d(TAG, "Updated order delivery status: $orderId -> $deliveryStatus")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating order delivery status", e)
            }
        }
    }
    
    /**
     * Delete order from order management (for orders shown in delivery page)
     */
    fun deleteOrder(orderId: String) {
        viewModelScope.launch {
            try {
                // Delete from order repository
                val success = repository.deleteOrder(orderId)
                
                if (success) {
                    // Running orders list auto-updates via StateFlow from OrderRepository
                    
                    // All orders list auto-updates via StateFlow from OrderRepository
                    
                    // Also remove any associated delivery entries
                    val associatedDeliveries = deliveries.value.filter { it.orderId == orderId }
                    associatedDeliveries.forEach { delivery ->
                        repository.deleteDelivery(delivery.id)
                    }
                    
                    _saveStatus.value = DeliverySaveStatus.Success("অর্ডার সফলভাবে মুছে ফেলা হয়েছে")
                    Log.d(TAG, "Order deleted: $orderId")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("অর্ডার মুছতে ব্যর্থ")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("অর্ডার মুছতে ব্যর্থ: ${e.message}")
                Log.e(TAG, "Error deleting order", e)
            }
        }
    }
    
    /**
     * Update order status
     */
    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                
                val success = repository.updateOrderStatus(orderId, newStatus)
                
                if (success) {
                    // Refresh orders lists (allOrders auto-updates via StateFlow)
                    loadRunningOrders()
                    
                    _saveStatus.value = DeliverySaveStatus.Success("অর্ডার স্ট্যাটাস আপডেট হয়েছে")
                    Log.d(TAG, "Order status updated: $orderId -> $newStatus")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("অর্ডার স্ট্যাটাস আপডেট করতে ব্যর্থ")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "Error updating order status", e)
            }
        }
    }
    
    /**
     * Update order payment status with paid amount
     */
    fun updateOrderPaymentStatus(orderId: String, newPaymentStatus: PaymentStatus, paidAmount: Double = 0.0) {
        Log.d(TAG, "[DELIVERY] updateOrderPaymentStatus called with: orderId=$orderId, status=$newPaymentStatus, amount=$paidAmount")
        viewModelScope.launch {
            try {
                _saveStatus.value = DeliverySaveStatus.Saving
                Log.d(TAG, "[DELIVERY] Calling repository.updateOrderPaymentStatus...")
                
                val success = repository.updateOrderPaymentStatus(orderId, newPaymentStatus, paidAmount)
                Log.d(TAG, "[DELIVERY] Repository returned success: $success")
                
                if (success) {
                    _saveStatus.value = DeliverySaveStatus.Success("পেমেন্ট স্ট্যাটাস আপডেট হয়েছে")
                    Log.d(TAG, "[DELIVERY] Order payment status updated successfully: $orderId -> $newPaymentStatus with amount: $paidAmount")
                } else {
                    _saveStatus.value = DeliverySaveStatus.Error("পেমেন্ট স্ট্যাটাস আপডেট করতে ব্যর্থ")
                    Log.e(TAG, "[DELIVERY] Failed to update payment status for order: $orderId")
                }
            } catch (e: Exception) {
                _saveStatus.value = DeliverySaveStatus.Error("ত্রুটি: ${e.message}")
                Log.e(TAG, "[DELIVERY] Exception updating order payment status", e)
            }
        }
    }
    
    // ==================== CUSTOMER INTEGRATION METHODS ====================
    
    /**
     * Search customers for autocomplete suggestions
     */
    fun searchCustomers(query: String) {
        viewModelScope.launch {
            try {
                val suggestions = repository.getCustomerSuggestions(query)
                _customerSuggestions.value = suggestions
                Log.d(TAG, "Found ${suggestions.size} customer suggestions for query: $query")
            } catch (e: Exception) {
                Log.e(TAG, "Error searching customers", e)
                _customerSuggestions.value = emptyList()
            }
        }
    }
    
    /**
     * Get customer by ID
     */
    fun getCustomerById(customerId: String): Customer? {
        return repository.getCustomerById(customerId)
    }
    
    /**
     * Load all active customers for initial suggestions
     */
    fun loadActiveCustomers() {
        viewModelScope.launch {
            try {
                val customers = repository.getAllActiveCustomers()
                _customerSuggestions.value = customers.take(10) // Show top 10
                Log.d(TAG, "Loaded ${customers.size} active customers")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading active customers", e)
            }
        }
    }
    
    /**
     * Clear customer suggestions
     */
    fun clearCustomerSuggestions() {
        _customerSuggestions.value = emptyList()
    }
}

/**
 * Sealed class for delivery save status
 */
sealed class DeliverySaveStatus {
    object Idle : DeliverySaveStatus()
    object Saving : DeliverySaveStatus()
    data class Success(val message: String) : DeliverySaveStatus()
    data class Error(val message: String) : DeliverySaveStatus()
}
