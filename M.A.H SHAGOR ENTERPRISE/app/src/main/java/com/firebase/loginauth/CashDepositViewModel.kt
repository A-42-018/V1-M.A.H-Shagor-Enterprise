package com.firebase.loginauth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CashDepositViewModel(application: Application) : AndroidViewModel(application) {
    private val cashDepositRepository = CashDepositRepository(application.applicationContext)
    
    private val _saveStatus = MutableStateFlow<CashDepositSaveStatus>(CashDepositSaveStatus.Idle)
    val saveStatus: StateFlow<CashDepositSaveStatus> = _saveStatus.asStateFlow()
    
    // Use repository's StateFlow for cash deposits
    val cashDeposits: StateFlow<List<CashDepositEntry>> = cashDepositRepository.cashDeposits
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        // Repository handles loading automatically
        Log.d("CashDepositViewModel", "CashDepositViewModel initialized with dual storage")
    }
    
    // Manual save cash deposit data
    fun saveCashDeposit(cylinderCount: String, amount: String) {
        if (cylinderCount.isBlank() || amount.isBlank()) {
            _saveStatus.value = CashDepositSaveStatus.Error("সিলিন্ডার সংখ্যা এবং পরিমাণ পূরণ করুন")
            return
        }
        
        viewModelScope.launch {
            _saveStatus.value = CashDepositSaveStatus.Saving
            _isLoading.value = true
            
            try {
                Log.d("CashDepositViewModel", "Saving cash deposit: cylinders='$cylinderCount', amount='$amount'")
                val result = cashDepositRepository.saveCashDepositData(cylinderCount, amount)
                if (result.isSuccess) {
                    Log.d("CashDepositViewModel", "Cash deposit saved successfully")
                    
                    // Log activity for recent activity panel
                    ActivityLogger.logCashDeposit(
                        amount = amount.toDoubleOrNull() ?: 0.0,
                        cylinderCount = cylinderCount.toIntOrNull() ?: 0,
                        description = "ক্যাশ জমা"
                    )
                    
                    _saveStatus.value = CashDepositSaveStatus.Saved
                    // Reset status after showing success
                    delay(2000)
                    _saveStatus.value = CashDepositSaveStatus.Idle
                } else {
                    Log.e("CashDepositViewModel", "Failed to save cash deposit: ${result.exceptionOrNull()?.message}")
                    _saveStatus.value = CashDepositSaveStatus.Error(result.exceptionOrNull()?.message ?: "সংরক্ষণে ত্রুটি")
                }
            } catch (e: Exception) {
                Log.e("CashDepositViewModel", "Exception during save: ${e.message}", e)
                _saveStatus.value = CashDepositSaveStatus.Error(e.message ?: "অজানা ত্রুটি")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Edit cash deposit entry
    fun editCashDeposit(depositId: String, cylinderCount: String, amount: String) {
        if (cylinderCount.isBlank() || amount.isBlank()) {
            _saveStatus.value = CashDepositSaveStatus.Error("সিলিন্ডার সংখ্যা এবং পরিমাণ পূরণ করুন")
            return
        }
        
        viewModelScope.launch {
            _saveStatus.value = CashDepositSaveStatus.Saving
            _isLoading.value = true
            
            try {
                Log.d("CashDepositViewModel", "Editing cash deposit: $depositId")
                val result = cashDepositRepository.updateCashDepositData(depositId, cylinderCount, amount)
                if (result.isSuccess) {
                    Log.d("CashDepositViewModel", "Cash deposit updated successfully")
                    _saveStatus.value = CashDepositSaveStatus.Saved
                    delay(2000)
                    _saveStatus.value = CashDepositSaveStatus.Idle
                } else {
                    Log.e("CashDepositViewModel", "Failed to update cash deposit: ${result.exceptionOrNull()?.message}")
                    _saveStatus.value = CashDepositSaveStatus.Error(result.exceptionOrNull()?.message ?: "আপডেটে ত্রুটি")
                }
            } catch (e: Exception) {
                Log.e("CashDepositViewModel", "Exception during edit: ${e.message}", e)
                _saveStatus.value = CashDepositSaveStatus.Error(e.message ?: "অজানা ত্রুটি")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Delete cash deposit entry
    fun deleteCashDeposit(depositId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                Log.d("CashDepositViewModel", "Deleting cash deposit: $depositId")
                val result = cashDepositRepository.deleteCashDeposit(depositId)
                if (result.isSuccess) {
                    Log.d("CashDepositViewModel", "Cash deposit deleted successfully")
                    // No success message for delete operation
                    _saveStatus.value = CashDepositSaveStatus.Idle
                } else {
                    Log.e("CashDepositViewModel", "Failed to delete cash deposit: ${result.exceptionOrNull()?.message}")
                    _saveStatus.value = CashDepositSaveStatus.Error(result.exceptionOrNull()?.message ?: "মুছে ফেলায় ত্রুটি")
                }
            } catch (e: Exception) {
                Log.e("CashDepositViewModel", "Exception during delete: ${e.message}", e)
                _saveStatus.value = CashDepositSaveStatus.Error(e.message ?: "অজানা ত্রুটি")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Clear save status
    fun clearSaveStatus() {
        _saveStatus.value = CashDepositSaveStatus.Idle
    }
    
    // Load cash deposits from repository
    /**
     * Force sync with cloud data
     */
    fun syncWithCloud() {
        cashDepositRepository.syncWithCloud()
    }
    
    /**
     * Upload all local data to Firestore
     */
    fun uploadAllToFirestore() {
        cashDepositRepository.uploadAllToFirestore()
    }
}

// Save status sealed class
sealed class CashDepositSaveStatus {
    object Idle : CashDepositSaveStatus()
    object Saving : CashDepositSaveStatus()
    object Saved : CashDepositSaveStatus()
    object AutoSaving : CashDepositSaveStatus()
    object AutoSaved : CashDepositSaveStatus()
    data class Error(val message: String) : CashDepositSaveStatus()
}
