package com.firebase.loginauth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DataSyncViewModel(context: Context) : ViewModel() {
    private val dataSyncService = DataSyncService(context)
    
    val syncStatus: StateFlow<SyncStatus> = dataSyncService.syncStatus
    
    fun startFullSync() {
        viewModelScope.launch {
            dataSyncService.performFullSync()
        }
    }
    
    fun getLastSyncTime(): String {
        return dataSyncService.getFormattedLastSyncTime()
    }
    
    fun isSyncNeeded(): Boolean {
        return dataSyncService.isSyncNeeded()
    }
    
    fun clearMessages() {
        dataSyncService.clearMessages()
    }
}
