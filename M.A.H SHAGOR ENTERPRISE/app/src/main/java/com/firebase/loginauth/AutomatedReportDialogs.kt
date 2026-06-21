package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenerateReportDialog(
    onDismiss: () -> Unit,
    onGenerate: (ReportType, String?, String?) -> Unit
) {
    var selectedReportType by remember { mutableStateOf(ReportType.DAILY) }
    var customStartDate by remember { mutableStateOf("") }
    var customEndDate by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "নতুন রিপোর্ট তৈরি করুন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "রিপোর্টের ধরন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Report Type Selection
                ReportType.values().forEach { reportType ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReportType == reportType,
                            onClick = { selectedReportType = reportType }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reportType.displayNameBn,
                            fontSize = 14.sp
                        )
                    }
                }
                
                // Custom date range for CUSTOM type
                if (selectedReportType == ReportType.CUSTOM) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = customStartDate,
                        onValueChange = { customStartDate = it },
                        label = { Text("শুরুর তারিখ (yyyy-mm-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = customEndDate,
                        onValueChange = { customEndDate = it },
                        label = { Text("শেষের তারিখ (yyyy-mm-dd)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val startDate = if (selectedReportType == ReportType.CUSTOM) customStartDate else null
                            val endDate = if (selectedReportType == ReportType.CUSTOM) customEndDate else null
                            onGenerate(selectedReportType, startDate, endDate)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("তৈরি করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportDetailsDialog(
    report: AutomatedReport,
    onDismiss: () -> Unit,
    onExport: (ExportFormat) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${report.reportType.displayNameBn} রিপোর্ট",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                
                Text(
                    text = report.reportDate,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Sales Summary
                    item {
                        ReportSectionCard(
                            title = "বিক্রয় সারসংক্ষেপ",
                            icon = Icons.Filled.Info,
                            color = Color(0xFF4CAF50)
                        ) {
                            ReportDetailItem("মোট বিক্রয়", "৳${String.format("%.0f", report.salesSummary.totalSales)}")
                            ReportDetailItem("মোট অর্ডার", "${report.salesSummary.totalOrders}")
                            ReportDetailItem("গড় অর্ডার মূল্য", "৳${String.format("%.0f", report.salesSummary.averageOrderValue)}")
                            ReportDetailItem("বিক্রয় বৃদ্ধি", "${String.format("%.1f", report.salesSummary.salesGrowth)}%")
                        }
                    }
                    
                    // Financial Summary
                    item {
                        ReportSectionCard(
                            title = "আর্থিক সারসংক্ষেপ",
                            icon = Icons.Filled.Info,
                            color = Color(0xFF2196F3)
                        ) {
                            ReportDetailItem("মোট আয়", "৳${String.format("%.0f", report.financialSummary.totalRevenue)}")
                            ReportDetailItem("মোট খরচ", "৳${String.format("%.0f", report.financialSummary.totalExpenses)}")
                            ReportDetailItem("নিট লাভ", "৳${String.format("%.0f", report.financialSummary.netProfit)}")
                            ReportDetailItem("লাভের মার্জিন", "${String.format("%.1f", report.financialSummary.profitMargin)}%")
                        }
                    }
                    
                    // Customer Summary
                    item {
                        ReportSectionCard(
                            title = "গ্রাহক সারসংক্ষেপ",
                            icon = Icons.Filled.Person,
                            color = Color(0xFF9C27B0)
                        ) {
                            ReportDetailItem("মোট গ্রাহক", "${report.customerSummary.totalCustomers}")
                            ReportDetailItem("সক্রিয় গ্রাহক", "${report.customerSummary.activeCustomers}")
                            ReportDetailItem("নতুন গ্রাহক", "${report.customerSummary.newCustomers}")
                            ReportDetailItem("গ্রাহক ধরে রাখার হার", "${String.format("%.1f", report.customerSummary.customerRetentionRate)}%")
                        }
                    }
                    
                    // Inventory Summary
                    item {
                        ReportSectionCard(
                            title = "ইনভেন্টরি সারসংক্ষেপ",
                            icon = Icons.Filled.Info,
                            color = Color(0xFFFF9800)
                        ) {
                            ReportDetailItem("মোট স্টক", "${report.inventorySummary.totalStock}")
                            ReportDetailItem("স্টক মূল্য", "৳${String.format("%.0f", report.inventorySummary.stockValue)}")
                            ReportDetailItem("কম স্টক আইটেম", "${report.inventorySummary.lowStockItems.size}")
                            ReportDetailItem("স্টক টার্নওভার", "${String.format("%.2f", report.inventorySummary.stockTurnoverRate)}")
                        }
                    }
                    
                    // Business Insights
                    if (report.insights.isNotEmpty()) {
                        item {
                            ReportSectionCard(
                                title = "ব্যবসায়িক অন্তর্দৃষ্টি",
                                icon = Icons.Filled.Info,
                                color = Color(0xFFF44336)
                            ) {
                                report.insights.take(3).forEach { insight ->
                                    InsightItem(insight)
                                }
                            }
                        }
                    }
                    
                    // Export Options
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "রিপোর্ট এক্সপোর্ট করুন",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExportFormat.values().forEach { format ->
                                        Button(
                                            onClick = { onExport(format) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF607D8B)
                                            )
                                        ) {
                                            Text(
                                                text = format.displayName,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = color
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun ReportDetailItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun InsightItem(insight: BusinessInsight) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (insight.impact) {
                InsightImpact.HIGH, InsightImpact.CRITICAL -> Color(0xFFFF5722).copy(alpha = 0.1f)
                InsightImpact.MEDIUM -> Color(0xFFFF9800).copy(alpha = 0.1f)
                InsightImpact.LOW -> Color(0xFF4CAF50).copy(alpha = 0.1f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = insight.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                text = insight.description,
                fontSize = 12.sp,
                color = Color.Gray
            )
            if (insight.recommendation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "সুপারিশ: ${insight.recommendation}",
                    fontSize = 12.sp,
                    color = Color(0xFF2196F3),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun ReportConfigurationDialog(
    configs: List<ReportConfig>,
    onDismiss: () -> Unit,
    onSaveConfig: (ReportConfig) -> Unit
) {
    var selectedReportType by remember { mutableStateOf(ReportType.DAILY) }
    var isEnabled by remember { mutableStateOf(true) }
    var autoGenerate by remember { mutableStateOf(true) }
    var generateTime by remember { mutableStateOf("09:00") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "রিপোর্ট কনফিগারেশন",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Report Type Selection
                Text(
                    text = "রিপোর্টের ধরন",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ReportType.values().take(3).forEach { reportType -> // Only show DAILY, WEEKLY, MONTHLY
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReportType == reportType,
                            onClick = { selectedReportType = reportType }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reportType.displayNameBn,
                            fontSize = 14.sp
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Enable/Disable
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("সক্রিয় করুন")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Auto Generate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Switch(
                        checked = autoGenerate,
                        onCheckedChange = { autoGenerate = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("স্বয়ংক্রিয় তৈরি")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Generate Time
                OutlinedTextField(
                    value = generateTime,
                    onValueChange = { generateTime = it },
                    label = { Text("তৈরির সময় (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val config = ReportConfig(
                                reportType = selectedReportType,
                                isEnabled = isEnabled,
                                autoGenerate = autoGenerate,
                                generateTime = generateTime
                            )
                            onSaveConfig(config)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("সংরক্ষণ করুন")
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsStatisticsContent(
    reports: List<AutomatedReport>,
    summaryStats: ReportSummaryStats
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "রিপোর্ট পরিসংখ্যান",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("মোট রিপোর্ট", "${summaryStats.totalReports}", Color(0xFF4CAF50))
                        StatItem("দৈনিক", "${summaryStats.dailyReports}", Color(0xFF2196F3))
                        StatItem("সাপ্তাহিক", "${summaryStats.weeklyReports}", Color(0xFF9C27B0))
                        StatItem("মাসিক", "${summaryStats.monthlyReports}", Color(0xFFFF9800))
                    }
                }
            }
        }
    }
}

@Composable
fun StatItem(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun ReportsConfigurationContent(
    configs: List<ReportConfig>,
    onSaveConfig: (ReportConfig) -> Unit
) {
    LazyColumn(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(configs) { config ->
            ConfigCard(
                config = config,
                onUpdate = onSaveConfig
            )
        }
    }
}

@Composable
fun ConfigCard(
    config: ReportConfig,
    onUpdate: (ReportConfig) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = config.reportType.displayNameBn,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Switch(
                    checked = config.isEnabled,
                    onCheckedChange = { enabled ->
                        onUpdate(config.copy(isEnabled = enabled))
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "তৈরির সময়: ${config.generateTime}",
                fontSize = 14.sp,
                color = Color.Gray
            )
            
            Text(
                text = "স্বয়ংক্রিয়: ${if (config.autoGenerate) "হ্যাঁ" else "না"}",
                fontSize = 14.sp,
                color = Color.Gray
            )
        }
    }
}
