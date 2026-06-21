package com.firebase.loginauth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BackupViewModel(context: Context) : ViewModel() {
    private val backupRepository = BackupRepository(context)
    
    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()
    
    val isLoading = backupRepository.isLoading
    val backupEntries = backupRepository.backupEntries
    
    init {
        loadBackups()
    }
    
    fun loadBackups() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null)
                backupRepository.loadBackups()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "ব্যাকআপ লোড করতে ব্যর্থ: ${e.message}"
                )
            }
        }
    }
    
    fun createBackup(description: String = "Manual backup") {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null, successMessage = null)
                val backupId = backupRepository.createBackup(description)
                _uiState.value = _uiState.value.copy(
                    successMessage = "ব্যাকআপ সফলভাবে তৈরি হয়েছে"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "ব্যাকআপ তৈরি করতে ব্যর্থ: ${e.message}"
                )
            }
        }
    }
    
    fun restoreBackup(backupId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null, successMessage = null)
                backupRepository.restoreBackup(backupId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "ব্যাকআপ সফলভাবে পুনরুদ্ধার হয়েছে"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "ব্যাকআপ পুনরুদ্ধার করতে ব্যর্থ: ${e.message}"
                )
            }
        }
    }
    
    fun deleteBackup(backupId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(error = null, successMessage = null)
                backupRepository.deleteBackup(backupId)
                _uiState.value = _uiState.value.copy(
                    successMessage = "ব্যাকআপ সফলভাবে মুছে ফেলা হয়েছে"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "ব্যাকআপ মুছতে ব্যর্থ: ${e.message}"
                )
            }
        }
    }
    
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }
}

data class BackupUiState(
    val error: String? = null,
    val successMessage: String? = null
)
