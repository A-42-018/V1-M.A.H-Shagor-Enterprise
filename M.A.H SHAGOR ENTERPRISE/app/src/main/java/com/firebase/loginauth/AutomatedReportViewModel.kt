package com.firebase.loginauth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * ViewModel for managing automated reports
 */
class AutomatedReportViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = AutomatedReportRepository(application.applicationContext)
    
    // UI State
    private val _uiState = MutableStateFlow(AutomatedReportUiState())
    val uiState: StateFlow<AutomatedReportUiState> = _uiState.asStateFlow()
    
    // Reports list
    private val _reports = MutableStateFlow<List<AutomatedReport>>(emptyList())
    val reports: StateFlow<List<AutomatedReport>> = _reports.asStateFlow()
    
    // Current report
    private val _currentReport = MutableStateFlow<AutomatedReport?>(null)
    val currentReport: StateFlow<AutomatedReport?> = _currentReport.asStateFlow()
    
    // Report configurations
    private val _reportConfigs = MutableStateFlow<List<ReportConfig>>(emptyList())
    val reportConfigs: StateFlow<List<ReportConfig>> = _reportConfigs.asStateFlow()
    
    init {
        loadReports()
        loadReportConfigs()
    }
    
    /**
     * Load all reports
     */
    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val reportsList = repository.getAllReports()
                _reports.value = reportsList
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
                Log.d("AutomatedReportViewModel", "Loaded ${reportsList.size} reports")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "রিপোর্ট লোড করতে ব্যর্থ: ${e.message}"
                )
                Log.e("AutomatedReportViewModel", "Error loading reports", e)
            }
        }
    }
    
    /**
     * Load reports by type
     */
    fun loadReportsByType(reportType: ReportType) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val reportsList = repository.getReportsByType(reportType)
                _reports.value = reportsList
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "রিপোর্ট লোড করতে ব্যর্থ: ${e.message}"
                )
                Log.e("AutomatedReportViewModel", "Error loading reports by type", e)
            }
        }
    }
    
    /**
     * Generate new automated report
     */
    fun generateReport(
        reportType: ReportType,
        customStartDate: String? = null,
        customEndDate: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGenerating = true,
                generationProgress = 0
            )
            
            try {
                // Simulate progress updates
                updateGenerationProgress(10, "ডেটা সংগ্রহ করা হচ্ছে...")
                
                val result = repository.generateAutomatedReport(reportType, customStartDate, customEndDate)
                
                updateGenerationProgress(50, "বিশ্লেষণ করা হচ্ছে...")
                
                if (result.isSuccess) {
                    val report = result.getOrNull()
                    updateGenerationProgress(80, "রিপোর্ট তৈরি করা হচ্ছে...")
                    
                    _currentReport.value = report
                    updateGenerationProgress(100, "সম্পন্ন!")
                    
                    // Refresh reports list
                    loadReports()
                    
                    _uiState.value = _uiState.value.copy(
                        isGenerating = false,
                        generationProgress = 0,
                        generationMessage = "",
                        successMessage = "রিপোর্ট সফলভাবে তৈরি হয়েছে!",
                        errorMessage = null
                    )
                    
                    Log.d("AutomatedReportViewModel", "Report generated successfully")
                } else {
                    throw result.exceptionOrNull() ?: Exception("Unknown error")
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGenerating = false,
                    generationProgress = 0,
                    generationMessage = "",
                    errorMessage = "রিপোর্ট তৈরি করতে ব্যর্থ: ${e.message}"
                )
                Log.e("AutomatedReportViewModel", "Error generating report", e)
            }
        }
    }
    
    /**
     * Update generation progress
     */
    private fun updateGenerationProgress(progress: Int, message: String) {
        _uiState.value = _uiState.value.copy(
            generationProgress = progress,
            generationMessage = message
        )
    }
    
    /**
     * Delete report
     */
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                val success = repository.deleteReport(reportId)
                if (success) {
                    loadReports() // Refresh list
                    _uiState.value = _uiState.value.copy(
                        successMessage = "রিপোর্ট মুছে ফেলা হয়েছে",
                        errorMessage = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "রিপোর্ট মুছতে ব্যর্থ"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "রিপোর্ট মুছতে ব্যর্থ: ${e.message}"
                )
                Log.e("AutomatedReportViewModel", "Error deleting report", e)
            }
        }
    }
    
    /**
     * Load report configurations
     */
    fun loadReportConfigs() {
        viewModelScope.launch {
            try {
                val configs = repository.getReportConfigs()
                _reportConfigs.value = configs
            } catch (e: Exception) {
                Log.e("AutomatedReportViewModel", "Error loading report configs", e)
            }
        }
    }
    
    /**
     * Save report configuration
     */
    fun saveReportConfig(config: ReportConfig) {
        viewModelScope.launch {
            try {
                repository.saveReportConfig(config)
                loadReportConfigs() // Refresh configs
                _uiState.value = _uiState.value.copy(
                    successMessage = "কনফিগারেশন সংরক্ষিত হয়েছে",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "কনফিগারেশন সংরক্ষণ করতে ব্যর্থ: ${e.message}"
                )
                Log.e("AutomatedReportViewModel", "Error saving report config", e)
            }
        }
    }
    
    /**
     * Schedule automatic report generation
     */
    fun scheduleAutomaticReports() {
        viewModelScope.launch {
            try {
                val configs = _reportConfigs.value.filter { it.isEnabled && it.autoGenerate }
                
                for (config in configs) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime >= config.nextGeneration) {
                        // Generate report
                        generateReport(config.reportType)
                        
                        // Update next generation time
                        val nextGeneration = calculateNextGenerationTime(config)
                        val updatedConfig = config.copy(
                            lastGenerated = currentTime,
                            nextGeneration = nextGeneration
                        )
                        repository.saveReportConfig(updatedConfig)
                    }
                }
                
                loadReportConfigs() // Refresh configs
                
            } catch (e: Exception) {
                Log.e("AutomatedReportViewModel", "Error scheduling automatic reports", e)
            }
        }
    }
    
    /**
     * Calculate next generation time based on report type
     */
    private fun calculateNextGenerationTime(config: ReportConfig): Long {
        val calendar = Calendar.getInstance()
        
        when (config.reportType) {
            ReportType.DAILY -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            ReportType.WEEKLY -> {
                calendar.add(Calendar.WEEK_OF_YEAR, 1)
            }
            ReportType.MONTHLY -> {
                calendar.add(Calendar.MONTH, 1)
            }
            ReportType.QUARTERLY -> {
                calendar.add(Calendar.MONTH, 3)
            }
            ReportType.YEARLY -> {
                calendar.add(Calendar.YEAR, 1)
            }
            ReportType.CUSTOM -> {
                calendar.add(Calendar.DAY_OF_MONTH, 1) // Default to daily for custom
            }
        }
        
        // Set time to configured generation time
        val timeParts = config.generateTime.split(":")
        if (timeParts.size == 2) {
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toIntOrNull() ?: 9)
            calendar.set(Calendar.MINUTE, timeParts[1].toIntOrNull() ?: 0)
            calendar.set(Calendar.SECOND, 0)
        }
        
        return calendar.timeInMillis
    }
    
    /**
     * Get report summary statistics
     */
    fun getReportSummaryStats(): ReportSummaryStats {
        val reportsList = _reports.value
        
        val totalReports = reportsList.size
        val dailyReports = reportsList.count { it.reportType == ReportType.DAILY }
        val weeklyReports = reportsList.count { it.reportType == ReportType.WEEKLY }
        val monthlyReports = reportsList.count { it.reportType == ReportType.MONTHLY }
        
        val lastGeneratedReport = reportsList.maxByOrNull { it.generatedAt }
        val lastGeneratedTime = lastGeneratedReport?.generatedAt ?: 0L
        
        return ReportSummaryStats(
            totalReports = totalReports,
            dailyReports = dailyReports,
            weeklyReports = weeklyReports,
            monthlyReports = monthlyReports,
            lastGeneratedTime = lastGeneratedTime
        )
    }
    
    /**
     * Clear messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            successMessage = null,
            errorMessage = null
        )
    }
    
    /**
     * Set current report for viewing
     */
    fun setCurrentReport(report: AutomatedReport) {
        _currentReport.value = report
    }
    
    /**
     * Clear current report
     */
    fun clearCurrentReport() {
        _currentReport.value = null
    }
    
    /**
     * Filter reports by date range
     */
    fun filterReportsByDateRange(startDate: String, endDate: String) {
        viewModelScope.launch {
            try {
                val allReports = repository.getAllReports()
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val start = sdf.parse(startDate)
                val end = sdf.parse(endDate)
                
                val filteredReports = allReports.filter { report ->
                    val reportDate = Date(report.generatedAt)
                    reportDate.after(start) && reportDate.before(end)
                }
                
                _reports.value = filteredReports
                
            } catch (e: Exception) {
                Log.e("AutomatedReportViewModel", "Error filtering reports", e)
            }
        }
    }
    
    /**
     * Export report data (placeholder for future implementation)
     */
    fun exportReport(report: AutomatedReport, format: ExportFormat) {
        viewModelScope.launch {
            try {
                // TODO: Implement export functionality
                _uiState.value = _uiState.value.copy(
                    successMessage = "রিপোর্ট এক্সপোর্ট করা হয়েছে (${format.displayName})",
                    errorMessage = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "রিপোর্ট এক্সপোর্ট করতে ব্যর্থ: ${e.message}"
                )
                Log.e("AutomatedReportViewModel", "Error exporting report", e)
            }
        }
    }
}

/**
 * UI State for Automated Reports
 */
data class AutomatedReportUiState(
    val isLoading: Boolean = false,
    val isGenerating: Boolean = false,
    val generationProgress: Int = 0,
    val generationMessage: String = "",
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val selectedReportType: ReportType = ReportType.DAILY,
    val selectedDateRange: Pair<String, String>? = null
)

/**
 * Report Summary Statistics
 */
data class ReportSummaryStats(
    val totalReports: Int = 0,
    val dailyReports: Int = 0,
    val weeklyReports: Int = 0,
    val monthlyReports: Int = 0,
    val lastGeneratedTime: Long = 0L
)

/**
 * Export formats
 */
enum class ExportFormat(val displayName: String) {
    PDF("PDF"),
    EXCEL("Excel"),
    CSV("CSV"),
    JSON("JSON")
}
