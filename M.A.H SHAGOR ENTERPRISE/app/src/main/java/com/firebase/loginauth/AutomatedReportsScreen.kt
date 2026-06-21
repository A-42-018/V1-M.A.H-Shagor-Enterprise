package com.firebase.loginauth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.compose.ui.platform.LocalContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomatedReportsScreen(
    onBackClick: () -> Unit,
    viewModel: AutomatedReportViewModel = viewModel(
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(
            LocalContext.current.applicationContext as android.app.Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val currentReport by viewModel.currentReport.collectAsState()
    val reportConfigs by viewModel.reportConfigs.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var showReportDetails by remember { mutableStateOf(false) }
    
    // Clear messages after showing
    LaunchedEffect(uiState.successMessage, uiState.errorMessage) {
        if (uiState.successMessage != null || uiState.errorMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FA),
                        Color(0xFFE9ECEF)
                    )
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "স্বয়ংক্রিয় রিপোর্ট",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { showGenerateDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Generate Report")
                }
                IconButton(onClick = { showConfigDialog = true }) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White,
                titleContentColor = Color(0xFF2C3E50)
            )
        )
        
        // Success/Error Messages
        uiState.successMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        uiState.errorMessage?.let { message ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFF5722).copy(alpha = 0.1f))
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Generation Progress
        if (uiState.isGenerating) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "রিপোর্ট তৈরি করা হচ্ছে...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = uiState.generationProgress / 100f,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.generationMessage,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.padding(horizontal = 16.dp),
            containerColor = Color.White,
            contentColor = Color(0xFF2C3E50)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("সব রিপোর্ট") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("পরিসংখ্যান") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("কনফিগারেশন") }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Tab Content
        when (selectedTab) {
            0 -> ReportsListContent(
                reports = reports,
                isLoading = uiState.isLoading,
                onReportClick = { report ->
                    viewModel.setCurrentReport(report)
                    showReportDetails = true
                },
                onDeleteReport = { reportId ->
                    viewModel.deleteReport(reportId)
                },
                onFilterByType = { reportType ->
                    viewModel.loadReportsByType(reportType)
                }
            )
            1 -> ReportsStatisticsContent(
                reports = reports,
                summaryStats = viewModel.getReportSummaryStats()
            )
            2 -> ReportsConfigurationContent(
                configs = reportConfigs,
                onSaveConfig = { config ->
                    viewModel.saveReportConfig(config)
                }
            )
        }
    }
    
    // Generate Report Dialog
    if (showGenerateDialog) {
        GenerateReportDialog(
            onDismiss = { showGenerateDialog = false },
            onGenerate = { reportType, startDate, endDate ->
                viewModel.generateReport(reportType, startDate, endDate)
                showGenerateDialog = false
            }
        )
    }
    
    // Configuration Dialog
    if (showConfigDialog) {
        ReportConfigurationDialog(
            configs = reportConfigs,
            onDismiss = { showConfigDialog = false },
            onSaveConfig = { config ->
                viewModel.saveReportConfig(config)
                showConfigDialog = false
            }
        )
    }
    
    // Report Details Dialog
    if (showReportDetails && currentReport != null) {
        ReportDetailsDialog(
            report = currentReport!!,
            onDismiss = {
                showReportDetails = false
                viewModel.clearCurrentReport()
            },
            onExport = { format ->
                viewModel.exportReport(currentReport!!, format)
            }
        )
    }
}

@Composable
fun ReportsListContent(
    reports: List<AutomatedReport>,
    isLoading: Boolean,
    onReportClick: (AutomatedReport) -> Unit,
    onDeleteReport: (String) -> Unit,
    onFilterByType: (ReportType) -> Unit
) {
    var selectedFilter by remember { mutableStateOf<ReportType?>(null) }
    
    Column {
        // Filter Chips
        LazyRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    onClick = {
                        selectedFilter = null
                        onFilterByType(ReportType.DAILY) // Reset to show all
                    },
                    label = { Text("সব") },
                    selected = selectedFilter == null
                )
            }
            items(ReportType.values().toList()) { reportType ->
                FilterChip(
                    onClick = {
                        selectedFilter = reportType
                        onFilterByType(reportType)
                    },
                    label = { Text(reportType.displayNameBn) },
                    selected = selectedFilter == reportType
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF4CAF50))
            }
        } else if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "কোন রিপোর্ট পাওয়া যায়নি",
                        fontSize = 18.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reports) { report ->
                    ReportCard(
                        report = report,
                        onClick = { onReportClick(report) },
                        onDelete = { onDeleteReport(report.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportCard(
    report: AutomatedReport,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(getReportTypeColor(report.reportType)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getReportTypeIcon(report.reportType),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${report.reportType.displayNameBn} রিপোর্ট",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = report.reportPeriod.periodDescription,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                Row {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFFF5722)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Report Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ReportSummaryItem(
                    label = "বিক্রয়",
                    value = "৳${String.format("%.0f", report.salesSummary.totalSales)}",
                    color = Color(0xFF4CAF50)
                )
                ReportSummaryItem(
                    label = "অর্ডার",
                    value = "${report.orderSummary.totalOrders}",
                    color = Color(0xFF2196F3)
                )
                ReportSummaryItem(
                    label = "লাভ",
                    value = "৳${String.format("%.0f", report.financialSummary.netProfit)}",
                    color = Color(0xFF9C27B0)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDateTime(report.generatedAt),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = getStatusColor(report.status).copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = report.status.displayNameBn,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        color = getStatusColor(report.status),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("রিপোর্ট মুছুন") },
            text = { Text("আপনি কি এই রিপোর্টটি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("হ্যাঁ", color = Color(0xFFFF5722))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("না")
                }
            }
        )
    }
}

@Composable
fun ReportSummaryItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

// Helper functions
fun getReportTypeColor(reportType: ReportType): Color {
    return when (reportType) {
        ReportType.DAILY -> Color(0xFF4CAF50)
        ReportType.WEEKLY -> Color(0xFF2196F3)
        ReportType.MONTHLY -> Color(0xFF9C27B0)
        ReportType.QUARTERLY -> Color(0xFFFF9800)
        ReportType.YEARLY -> Color(0xFFF44336)
        ReportType.CUSTOM -> Color(0xFF607D8B)
    }
}

fun getReportTypeIcon(reportType: ReportType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (reportType) {
        ReportType.DAILY -> Icons.Filled.DateRange
        ReportType.WEEKLY -> Icons.Filled.DateRange
        ReportType.MONTHLY -> Icons.Filled.DateRange
        ReportType.QUARTERLY -> Icons.Filled.DateRange
        ReportType.YEARLY -> Icons.Filled.DateRange
        ReportType.CUSTOM -> Icons.Filled.Settings
    }
}

fun getStatusColor(status: ReportStatus): Color {
    return when (status) {
        ReportStatus.GENERATING -> Color(0xFFFF9800)
        ReportStatus.GENERATED -> Color(0xFF4CAF50)
        ReportStatus.SENT -> Color(0xFF2196F3)
        ReportStatus.FAILED -> Color(0xFFF44336)
    }
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale("bn", "BD"))
    return sdf.format(Date(timestamp))
}
