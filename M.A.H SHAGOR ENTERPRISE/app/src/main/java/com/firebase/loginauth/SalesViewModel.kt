package com.firebase.loginauth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SalesViewModel(application: Application) : AndroidViewModel(application) {
    private val orderRepository = OrderRepository.getInstance(application)
    
    // StateFlow for UI
    val sales = orderRepository.orders
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadSales()
    }

    private fun loadSales() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sales data is automatically loaded through the repository's StateFlow
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Error loading sales data"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun refreshSales() {
        loadSales()
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}
