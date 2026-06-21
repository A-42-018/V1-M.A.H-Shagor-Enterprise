package com.firebase.loginauth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Sealed class for save status
sealed class DueSaveStatus {
    object Idle : DueSaveStatus()
    object Saving : DueSaveStatus()
    data class Success(val message: String) : DueSaveStatus()
    data class Error(val message: String) : DueSaveStatus()
}

class DueAccountViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DueAccountRepository(application)
    
    // StateFlow for UI
    val dueEntries = repository.dueEntries
    val dueSummary = repository.dueSummary
    
    private val _saveStatus = MutableStateFlow<DueSaveStatus>(DueSaveStatus.Idle)
    val saveStatus: StateFlow<DueSaveStatus> = _saveStatus.asStateFlow()
    
    private val _selectedFilter = MutableStateFlow(DueAccountFilter.ALL)
    val selectedFilter: StateFlow<DueAccountFilter> = _selectedFilter.asStateFlow()
    
    private val _filteredEntries = MutableStateFlow<List<DueAccountEntry>>(emptyList())
    val filteredEntries: StateFlow<List<DueAccountEntry>> = _filteredEntries.asStateFlow()
    
    private val _customerBalances = MutableStateFlow<List<CustomerDueBalance>>(emptyList())
    val customerBalances: StateFlow<List<CustomerDueBalance>> = _customerBalances.asStateFlow()

    init {
        // Initialize filtered entries and customer balances
        viewModelScope.launch {
            dueEntries.collect { entries ->
                updateFilteredEntries()
                updateCustomerBalances()
            }
        }
    }

    // Add new due entry
    fun addDueEntry(
        customerId: String,
        customerName: String,
        customerPhone: String,
        transactionType: DueTransactionType,
        amount: Double,
        description: String,
        orderId: String? = null,
        paymentMethod: String = "ক্যাশ",
        notes: String = ""
    ) {
        if (customerId.isBlank() || customerName.isBlank() || amount <= 0 || description.isBlank()) {
            _saveStatus.value = DueSaveStatus.Error("সব বাধ্যতামূলক ক্ষেত্র পূরণ করুন")
            return
        }

        viewModelScope.launch {
            _saveStatus.value = DueSaveStatus.Saving
            
            val entry = DueAccountEntry(
                customerId = customerId,
                customerName = customerName,
                customerPhone = customerPhone,
                transactionType = transactionType,
                amount = amount,
                description = description,
                orderId = orderId,
                paymentMethod = paymentMethod,
                notes = notes
            )
            
            val result = repository.addDueEntry(entry)
            _saveStatus.value = if (result.isSuccess) {
                DueSaveStatus.Success(result.getOrNull() ?: "সফলভাবে যোগ করা হয়েছে")
            } else {
                DueSaveStatus.Error(result.exceptionOrNull()?.message ?: "যোগ করতে সমস্যা হয়েছে")
            }
        }
    }

    // Update due entry
    fun updateDueEntry(entry: DueAccountEntry) {
        if (entry.customerId.isBlank() || entry.customerName.isBlank() || 
            entry.amount <= 0 || entry.description.isBlank()) {
            _saveStatus.value = DueSaveStatus.Error("সব বাধ্যতামূলক ক্ষেত্র পূরণ করুন")
            return
        }

        viewModelScope.launch {
            _saveStatus.value = DueSaveStatus.Saving
            
            val result = repository.updateDueEntry(entry)
            _saveStatus.value = if (result.isSuccess) {
                DueSaveStatus.Success(result.getOrNull() ?: "সফলভাবে আপডেট করা হয়েছে")
            } else {
                DueSaveStatus.Error(result.exceptionOrNull()?.message ?: "আপডেট করতে সমস্যা হয়েছে")
            }
        }
    }

    // Delete due entry
    fun deleteDueEntry(entryId: String) {
        viewModelScope.launch {
            _saveStatus.value = DueSaveStatus.Saving
            
            val result = repository.deleteDueEntry(entryId)
            _saveStatus.value = if (result.isSuccess) {
                DueSaveStatus.Success(result.getOrNull() ?: "সফলভাবে মুছে ফেলা হয়েছে")
            } else {
                DueSaveStatus.Error(result.exceptionOrNull()?.message ?: "মুছতে সমস্যা হয়েছে")
            }
        }
    }

    // Set filter and update filtered entries
    fun setFilter(filter: DueAccountFilter) {
        _selectedFilter.value = filter
        updateFilteredEntries()
    }

    // Update filter and refresh filtered entries
    fun updateFilter(filter: DueAccountFilter) {
        _selectedFilter.value = filter
        updateFilteredEntries()
    }
    
    // Integrate sales due data for comprehensive dashboard calculation
    fun integrateSalesDueData(salesWithDue: List<Order>) {
        repository.integrateSalesDueData(salesWithDue)
    }

    // Update filtered entries based on selected filter
    private fun updateFilteredEntries() {
        val filter = _selectedFilter.value
        val filtered = repository.filterEntriesByPeriod(filter)
        _filteredEntries.value = filtered.sortedByDescending { it.date }
    }

    // Update customer balances
    private fun updateCustomerBalances() {
        val balances = repository.getAllCustomerDueBalances()
        _customerBalances.value = balances
    }

    // Get customer due balance
    fun getCustomerDueBalance(customerId: String): CustomerDueBalance? {
        return repository.getCustomerDueBalance(customerId)
    }

    // Clear save status
    fun clearSaveStatus() {
        _saveStatus.value = DueSaveStatus.Idle
    }

    // Get total due amount for dashboard
    fun getTotalDueAmount(): Double {
        return repository.getTotalDueAmount()
    }

    // Validation functions
    fun validateAmount(amountText: String): String? {
        return when {
            amountText.isBlank() -> "পরিমাণ লিখুন"
            amountText.toDoubleOrNull() == null -> "সঠিক পরিমাণ লিখুন"
            amountText.toDouble() <= 0 -> "পরিমাণ ০ এর চেয়ে বেশি হতে হবে"
            else -> null
        }
    }

    fun validateDescription(description: String): String? {
        return when {
            description.isBlank() -> "বিবরণ লিখুন"
            description.length < 3 -> "বিবরণ কমপক্ষে ৩ অক্ষরের হতে হবে"
            else -> null
        }
    }

    fun validateCustomerName(name: String): String? {
        return when {
            name.isBlank() -> "কাস্টমারের নাম লিখুন"
            name.length < 2 -> "নাম কমপক্ষে ২ অক্ষরের হতে হবে"
            else -> null
        }
    }

    fun validateCustomerPhone(phone: String): String? {
        return when {
            phone.isBlank() -> "ফোন নম্বর লিখুন"
            phone.length < 11 -> "সঠিক ফোন নম্বর লিখুন"
            !phone.all { it.isDigit() || it == '+' || it == '-' || it == ' ' } -> "সঠিক ফোন নম্বর লিখুন"
            else -> null
        }
    }
}
