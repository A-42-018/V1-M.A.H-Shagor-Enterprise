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
 * ViewModel for managing customer data and UI state
 * Follows MVVM pattern consistent with existing AuthViewModel and CashDepositViewModel
 */
class CustomerViewModel(application: Application) : AndroidViewModel(application) {
    
    private val customerRepository = CustomerRepository(application)
    
    // UI State
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCustomerType = MutableStateFlow<CustomerType?>(null)
    val selectedCustomerType: StateFlow<CustomerType?> = _selectedCustomerType.asStateFlow()
    
    private val _filteredCustomers = MutableStateFlow<List<Customer>>(emptyList())
    val filteredCustomers: StateFlow<List<Customer>> = _filteredCustomers.asStateFlow()
    
    // Repository data
    val customers = customerRepository.customers
    val customerStats = customerRepository.customerStats
    
    companion object {
        private const val TAG = "CustomerViewModel"
    }
    
    init {
        // Initialize with all customers
        viewModelScope.launch {
            customers.collect { customerList ->
                applyFilters(customerList)
            }
        }
        
        // Removed automatic sample data injection for production
        // addSampleCustomersIfEmpty()
    }
    
    /**
     * Add a new customer (simplified version with only name mandatory)
     */
    fun addCustomer(customer: Customer) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Validate input - only name is mandatory
                if (customer.name.isBlank()) {
                    _errorMessage.value = "গ্রাহকের নাম প্রয়োজন"
                    return@launch
                }
                
                val success = customerRepository.addCustomer(customer)
                if (success) {
                    _successMessage.value = "গ্রাহক সফলভাবে যোগ করা হয়েছে"
                    Log.d(TAG, "Customer added successfully: ${customer.name}")
                    
                    // Log activity for Recent Activity panel
                    ActivityLogger.logCustomerCreate(
                        customerName = customer.name,
                        phone = customer.phone.ifBlank { "ফোন নেই" }
                    )
                } else {
                    _errorMessage.value = "গ্রাহক যোগ করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "গ্রাহক যোগ করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error adding customer", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Add a new customer (full version for customer management screen)
     */
    fun addCustomer(
        name: String,
        phone: String,
        address: String,
        area: String,
        customerType: CustomerType,
        creditLimit: Double = 0.0,
        email: String = "",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                // Validate input
                if (name.isBlank()) {
                    _errorMessage.value = "গ্রাহকের নাম প্রয়োজন"
                    return@launch
                }
                
                if (phone.isBlank()) {
                    _errorMessage.value = "ফোন নম্বর প্রয়োজন"
                    return@launch
                }
                
                if (phone.length < 11) {
                    _errorMessage.value = "সঠিক ফোন নম্বর দিন"
                    return@launch
                }
                
                val customer = Customer(
                    name = name.trim(),
                    phone = phone.trim(),
                    address = address.trim(),
                    area = area.trim(),
                    customerType = customerType,
                    creditLimit = creditLimit,
                    email = email.trim(),
                    notes = notes.trim()
                )
                
                val success = customerRepository.addCustomer(customer)
                if (success) {
                    _successMessage.value = "গ্রাহক সফলভাবে যোগ করা হয়েছে"
                    Log.d(TAG, "Customer added successfully: ${customer.name}")
                    
                    // Log activity for Recent Activity panel
                    ActivityLogger.logCustomerCreate(
                        customerName = customer.name,
                        phone = customer.phone
                    )
                } else {
                    _errorMessage.value = "এই ফোন নম্বরের গ্রাহক ইতিমধ্যে আছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "গ্রাহক যোগ করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error adding customer", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Update an existing customer
     */
    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = customerRepository.updateCustomer(customer)
                if (success) {
                    _successMessage.value = "গ্রাহকের তথ্য আপডেট করা হয়েছে"
                    Log.d(TAG, "Customer updated successfully: ${customer.name}")
                } else {
                    _errorMessage.value = "গ্রাহক আপডেট করতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "গ্রাহক আপডেট করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error updating customer", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Delete a customer
     */
    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            try {
                val success = customerRepository.deleteCustomer(customerId)
                if (success) {
                    _successMessage.value = "গ্রাহক মুছে ফেলা হয়েছে"
                    Log.d(TAG, "Customer deleted successfully: $customerId")
                } else {
                    _errorMessage.value = "গ্রাহক মুছতে সমস্যা হয়েছে"
                }
                
            } catch (e: Exception) {
                _errorMessage.value = "গ্রাহক মুছতে সমস্যা হয়েছে"
                Log.e(TAG, "Error deleting customer", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Search customers
     */
    fun searchCustomers(query: String) {
        _searchQuery.value = query
        applyFilters(customers.value)
    }
    
    /**
     * Filter customers by type
     */
    fun filterByCustomerType(type: CustomerType?) {
        _selectedCustomerType.value = type
        applyFilters(customers.value)
    }
    
    /**
     * Apply search and filter to customer list
     */
    private fun applyFilters(customerList: List<Customer>) {
        var filtered = customerList
        
        // Apply search query
        if (_searchQuery.value.isNotBlank()) {
            filtered = customerRepository.searchCustomers(_searchQuery.value)
        }
        
        // Apply customer type filter
        _selectedCustomerType.value?.let { type ->
            filtered = filtered.filter { it.customerType == type }
        }
        
        _filteredCustomers.value = filtered
    }
    
    /**
     * Get customer by ID
     */
    fun getCustomerById(customerId: String): Customer? {
        return customerRepository.getCustomerById(customerId)
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
     * Clear all filters
     */
    fun clearFilters() {
        _searchQuery.value = ""
        _selectedCustomerType.value = null
        _filteredCustomers.value = customers.value
    }
    
    /**
     * Add sample customers if the list is empty (for testing)
     */
    private fun addSampleCustomersIfEmpty() {
        viewModelScope.launch {
            if (customers.value.isEmpty()) {
                customerRepository.addSampleCustomers()
                Log.d(TAG, "Added sample customers for testing")
            }
        }
    }
    
    /**
     * Refresh customer data
     */
    fun refreshCustomers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Force reload from repository
                applyFilters(customers.value)
                Log.d(TAG, "Customer data refreshed")
            } catch (e: Exception) {
                _errorMessage.value = "ডেটা রিফ্রেশ করতে সমস্যা হয়েছে"
                Log.e(TAG, "Error refreshing customers", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get customers by area
     */
    fun getCustomersByArea(area: String): List<Customer> {
        return customerRepository.getCustomersByArea(area)
    }
    
    /**
     * Get active customers only
     */
    fun getActiveCustomers(): List<Customer> {
        return customerRepository.getActiveCustomers()
    }
}
