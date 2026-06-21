package com.firebase.loginauth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CylinderStockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CylinderStockRepository(application.applicationContext)
    
    val stockItems: StateFlow<List<CylinderStock>> = repository.stockItems
    val stockTransactions: StateFlow<List<StockTransaction>> = repository.stockTransactions
    val isLoading: StateFlow<Boolean> = repository.isLoading
    val error: StateFlow<String?> = repository.error
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    
    private val _selectedStock = MutableStateFlow<CylinderStock?>(null)
    val selectedStock = _selectedStock.asStateFlow()
    
    private val _showAddStockDialog = MutableStateFlow(false)
    val showAddStockDialog = _showAddStockDialog.asStateFlow()
    
    private val _showEditStockDialog = MutableStateFlow(false)
    val showEditStockDialog = _showEditStockDialog.asStateFlow()
    
    private val _showStockDetailsDialog = MutableStateFlow(false)
    val showStockDetailsDialog = _showStockDetailsDialog.asStateFlow()
    
    private val _showAdjustStockDialog = MutableStateFlow(false)
    val showAdjustStockDialog = _showAdjustStockDialog.asStateFlow()

    init {
        loadStockItems()
        loadStockTransactions()
    }
    
    fun loadStockItems() {
        viewModelScope.launch {
            repository.loadStockItems()
        }
    }
    
    fun loadStockTransactions() {
        viewModelScope.launch {
            repository.loadStockTransactions()
        }
    }
    
    fun addStockItem(stock: CylinderStock) {
        viewModelScope.launch {
            println("DEBUG: ViewModel addStockItem called with: $stock")
            try {
                val result = repository.addStockItem(stock)
                if (result.isSuccess) {
                    println("DEBUG: Stock added successfully, closing dialog")
                    _showAddStockDialog.value = false
                    
                    // Log activity for Recent Activity panel
                    ActivityLogger.logStockAdd(
                        cylinderType = stock.cylinderType,
                        brand = stock.brand,
                        quantity = stock.totalQuantity,
                        pricePerUnit = stock.pricePerUnit
                    )
                } else {
                    println("DEBUG: Failed to add stock: ${result.exceptionOrNull()?.message}")
                    // Error is already handled in repository, dialog stays open for retry
                }
            } catch (e: Exception) {
                println("DEBUG: Exception in ViewModel addStockItem: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun updateStockItem(stock: CylinderStock) {
        viewModelScope.launch {
            println("DEBUG: ViewModel updateStockItem called with: $stock")
            try {
                val result = repository.updateStockItem(stock)
                if (result.isSuccess) {
                    println("DEBUG: Stock updated successfully, closing dialog")
                    _showEditStockDialog.value = false
                    _selectedStock.value = null
                } else {
                    println("DEBUG: Failed to update stock: ${result.exceptionOrNull()?.message}")
                    // Error is already handled in repository, dialog stays open for retry
                }
            } catch (e: Exception) {
                println("DEBUG: Exception in ViewModel updateStockItem: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    fun deleteStockItem(stockId: String) {
        viewModelScope.launch {
            repository.deleteStockItem(stockId)
        }
    }
    
    fun adjustStock(
        stockId: String,
        quantityChange: Int,
        reason: String,
        orderId: String? = null,
        customerId: String? = null,
        notes: String = ""
    ) {
        viewModelScope.launch {
            repository.adjustStock(stockId, quantityChange, reason, orderId, customerId, notes)
            _showAdjustStockDialog.value = false
            _selectedStock.value = null
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectStock(stock: CylinderStock) {
        _selectedStock.value = stock
    }
    
    fun clearSelectedStock() {
        _selectedStock.value = null
    }
    
    fun showAddStockDialog() {
        _showAddStockDialog.value = true
    }
    
    fun hideAddStockDialog() {
        _showAddStockDialog.value = false
    }
    
    fun showEditStockDialog(stock: CylinderStock) {
        _selectedStock.value = stock
        _showEditStockDialog.value = true
    }
    
    fun hideEditStockDialog() {
        _showEditStockDialog.value = false
        _selectedStock.value = null
    }
    
    fun showStockDetailsDialog(stock: CylinderStock) {
        _selectedStock.value = stock
        _showStockDetailsDialog.value = true
    }
    
    fun hideStockDetailsDialog() {
        _showStockDetailsDialog.value = false
        _selectedStock.value = null
    }
    
    fun showAdjustStockDialog(stock: CylinderStock) {
        _selectedStock.value = stock
        _showAdjustStockDialog.value = true
    }
    
    fun hideAdjustStockDialog() {
        _showAdjustStockDialog.value = false
        _selectedStock.value = null
    }
    
    fun clearError() {
        repository.clearError()
    }
    
    // Helper functions for UI
    fun getFilteredStockItems(): List<CylinderStock> {
        val stocks: List<CylinderStock> = stockItems.value
        val query: String = searchQuery.value
        return if (query.isBlank()) {
            stocks
        } else {
            repository.searchStockItems(query)
        }
    }
    
    fun getLowStockItems(): List<CylinderStock> {
        val stocks: List<CylinderStock> = stockItems.value
        return stocks.filter { stock: CylinderStock -> 
            stock.availableQuantity <= stock.lowStockThreshold 
        }
    }
    
    fun getStockStatistics(): StockStatistics {
        val stocks: List<CylinderStock> = stockItems.value
        val totalItems: Int = stocks.size
        
        var lowStockCount = 0
        var criticalStockCount = 0
        var totalValue = 0.0
        var totalQuantity = 0
        var totalSoldQuantity = 0
        
        for (stock: CylinderStock in stocks) {
            if (stock.availableQuantity <= stock.lowStockThreshold) {
                lowStockCount++
            }
            if (stock.availableQuantity <= 5) {
                criticalStockCount++
            }
            totalValue += stock.availableQuantity * stock.pricePerUnit
            totalQuantity += stock.availableQuantity
            totalSoldQuantity += stock.soldQuantity
        }
        
        return StockStatistics(
            totalItems = totalItems,
            totalValue = totalValue,
            lowStockCount = lowStockCount,
            criticalStockCount = criticalStockCount,
            totalQuantity = totalQuantity,
            totalSoldQuantity = totalSoldQuantity
        )
    }
}

data class StockStatistics(
    val totalItems: Int = 0,
    val totalValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val criticalStockCount: Int = 0,
    val totalQuantity: Int = 0,
    val totalSoldQuantity: Int = 0
)
