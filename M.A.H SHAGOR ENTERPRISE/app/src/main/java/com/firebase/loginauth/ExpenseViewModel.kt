package com.firebase.loginauth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.*

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {
    private val expenseRepository = ExpenseRepository(application.applicationContext)
    
    private val _saveStatus = MutableStateFlow<ExpenseSaveStatus>(ExpenseSaveStatus.Idle)
    val saveStatus: StateFlow<ExpenseSaveStatus> = _saveStatus.asStateFlow()
    
    // Use repository's StateFlow for expenses
    val expenses: StateFlow<List<ExpenseEntry>> = expenseRepository.expenses
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Repository handles loading automatically
        Log.d("ExpenseViewModel", "ExpenseViewModel initialized with dual storage")
    }
    
    // Save expense entry
    fun saveExpense(
        expenseType: String,
        description: String,
        amount: String,
        paymentMethod: String,
        vendor: String = "",
        location: String = "",
        notes: String = ""
    ) {
        if (expenseType.isBlank() || description.isBlank() || amount.isBlank() || paymentMethod.isBlank()) {
            _saveStatus.value = ExpenseSaveStatus.Error("খরচের ধরন, বিবরণ, পরিমাণ এবং পেমেন্ট পদ্ধতি পূরণ করুন")
            return
        }
        
        viewModelScope.launch {
            _saveStatus.value = ExpenseSaveStatus.Saving
            _isLoading.value = true
            
            try {
                val expenseId = UUID.randomUUID().toString()
                val expenseEntry = ExpenseEntry(
                    id = expenseId,
                    expenseType = expenseType,
                    description = description,
                    amount = amount,
                    paymentMethod = paymentMethod,
                    vendor = vendor.ifBlank { "অজানা" },
                    location = location.ifBlank { "অনির্দিষ্ট" },
                    notes = notes,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                
                Log.d("ExpenseViewModel", "Saving expense: $description, Amount: $amount")
                val result = expenseRepository.addExpense(expenseEntry)
                if (result.isSuccess) {
                    Log.d("ExpenseViewModel", "Expense saved successfully")
                    
                    // Log activity for recent activity panel
                    ActivityLogger.logExpense(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        expenseType = expenseType,
                        description = description,
                        vendor = vendor.ifBlank { null }
                    )
                    
                    _saveStatus.value = ExpenseSaveStatus.Saved
                    delay(2000)
                    _saveStatus.value = ExpenseSaveStatus.Idle
                } else {
                    Log.e("ExpenseViewModel", "Failed to save expense: ${result.exceptionOrNull()?.message}")
                    _saveStatus.value = ExpenseSaveStatus.Error(result.exceptionOrNull()?.message ?: "সংরক্ষণে ত্রুটি")
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Exception during save: ${e.message}", e)
                _saveStatus.value = ExpenseSaveStatus.Error(e.message ?: "অজানা ত্রুটি")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Edit expense entry
    fun editExpense(
        expenseId: String,
        expenseType: String,
        description: String,
        amount: String,
        paymentMethod: String,
        vendor: String = "",
        location: String = "",
        notes: String = ""
    ) {
        if (expenseType.isBlank() || description.isBlank() || amount.isBlank() || paymentMethod.isBlank()) {
            _saveStatus.value = ExpenseSaveStatus.Error("খরচের ধরন, বিবরণ, পরিমাণ এবং পেমেন্ট পদ্ধতি পূরণ করুন")
            return
        }
        
        viewModelScope.launch {
            _saveStatus.value = ExpenseSaveStatus.Saving
            _isLoading.value = true
            
            try {
                // Find the existing expense
                val existingExpense = expenses.value.find { it.id == expenseId }
                if (existingExpense != null) {
                    val updatedExpense = existingExpense.copy(
                        expenseType = expenseType,
                        description = description,
                        amount = amount,
                        paymentMethod = paymentMethod,
                        vendor = vendor.ifBlank { "অজানা" },
                        location = location.ifBlank { "অনির্দিষ্ট" },
                        notes = notes,
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    Log.d("ExpenseViewModel", "Editing expense: $expenseId")
                    val result = expenseRepository.updateExpense(updatedExpense)
                    if (result.isSuccess) {
                        Log.d("ExpenseViewModel", "Expense updated successfully")
                        _saveStatus.value = ExpenseSaveStatus.Saved
                        delay(2000)
                        _saveStatus.value = ExpenseSaveStatus.Idle
                    } else {
                        Log.e("ExpenseViewModel", "Failed to update expense: ${result.exceptionOrNull()?.message}")
                        _saveStatus.value = ExpenseSaveStatus.Error(result.exceptionOrNull()?.message ?: "আপডেটে ত্রুটি")
                    }
                } else {
                    _saveStatus.value = ExpenseSaveStatus.Error("খরচের তথ্য পাওয়া যায়নি")
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Exception during edit: ${e.message}", e)
                _saveStatus.value = ExpenseSaveStatus.Error(e.message ?: "অজানা ত্রুটি")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Delete expense entry
    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                Log.d("ExpenseViewModel", "Deleting expense: $expenseId")
                val result = expenseRepository.deleteExpense(expenseId)
                if (result.isSuccess) {
                    Log.d("ExpenseViewModel", "Expense deleted successfully")
                    // No success message for delete operation
                    _saveStatus.value = ExpenseSaveStatus.Idle
                } else {
                    Log.e("ExpenseViewModel", "Failed to delete expense: ${result.exceptionOrNull()?.message}")
                    _saveStatus.value = ExpenseSaveStatus.Error(result.exceptionOrNull()?.message ?: "মুছে ফেলায় ত্রুটি")
                }
            } catch (e: Exception) {
                Log.e("ExpenseViewModel", "Exception during delete: ${e.message}", e)
                _saveStatus.value = ExpenseSaveStatus.Error(e.message ?: "অজানা ত্রুটি")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Clear save status
    fun clearSaveStatus() {
        _saveStatus.value = ExpenseSaveStatus.Idle
    }
    
    // Get total expense amount
    fun getTotalExpenseAmount(): Double {
        return expenseRepository.getTotalExpenseAmount()
    }
    
    // Force sync with cloud data
    fun syncWithCloud() {
        expenseRepository.syncWithCloud()
    }
    
    // Upload all local data to Firestore
    fun uploadAllToFirestore() {
        expenseRepository.uploadAllToFirestore()
    }
}

// Save status sealed class
sealed class ExpenseSaveStatus {
    object Idle : ExpenseSaveStatus()
    object Saving : ExpenseSaveStatus()
    object Saved : ExpenseSaveStatus()
    data class Error(val message: String) : ExpenseSaveStatus()
}
