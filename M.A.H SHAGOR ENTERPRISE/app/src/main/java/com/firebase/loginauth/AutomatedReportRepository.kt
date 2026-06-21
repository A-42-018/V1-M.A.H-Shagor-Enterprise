package com.firebase.loginauth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Repository for handling automated report operations
 */
class AutomatedReportRepository(private val context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Existing data repositories
    private val orderRepository by lazy { context?.let { OrderRepository.getInstance(it) } }
    private val customerRepository by lazy { context?.let { CustomerRepository(it) } }
    private val stockRepository by lazy { context?.let { CylinderStockRepository(it) } }
    private val expenseRepository by lazy { context?.let { ExpenseRepository(it) } }
    private val cashDepositRepository by lazy { context?.let { CashDepositRepository(it) } }
    
    /**
     * Get current user ID for Firestore operations
     */
    private fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }
    
    /**
     * Get user-specific reports collection
     */
    private fun getReportsCollection() = getCurrentUserId()?.let { userId ->
        firestore.collection("users").document(userId).collection("automated_reports")
    }
    
    /**
     * Get user-specific report configs collection
     */
    private fun getConfigCollection() = getCurrentUserId()?.let { userId ->
        firestore.collection("users").document(userId).collection("report_configs")
    }

    /**
     * Generate automated report for specified type and date range
     */
    suspend fun generateAutomatedReport(
        reportType: ReportType,
        customStartDate: String? = null,
        customEndDate: String? = null
    ): Result<AutomatedReport> {
        return try {
            Log.d("AutomatedReportRepository", "Generating $reportType report")
            
            val reportPeriod = if (reportType == ReportType.CUSTOM && customStartDate != null && customEndDate != null) {
                ReportPeriod(customStartDate, customEndDate, "কাস্টম পিরিয়ড")
            } else {
                getDateRangeForReportType(reportType)
            }
            
            // Aggregate data from all sources
            val salesSummary = generateSalesSummary(reportPeriod)
            val orderSummary = generateOrderSummary(reportPeriod)
            val customerSummary = generateCustomerSummary(reportPeriod)
            val inventorySummary = generateInventorySummary(reportPeriod)
            val expenseSummary = generateExpenseSummary(reportPeriod)
            val financialSummary = generateFinancialSummary(reportPeriod, salesSummary, expenseSummary)
            val topPerformers = generateTopPerformers(reportPeriod)
            val insights = generateBusinessInsights(salesSummary, orderSummary, customerSummary, inventorySummary, financialSummary)
            val recommendations = generateRecommendations(insights)
            
            val report = AutomatedReport(
                reportType = reportType,
                reportPeriod = reportPeriod,
                salesSummary = salesSummary,
                orderSummary = orderSummary,
                customerSummary = customerSummary,
                inventorySummary = inventorySummary,
                expenseSummary = expenseSummary,
                financialSummary = financialSummary,
                topPerformers = topPerformers,
                insights = insights,
                recommendations = recommendations,
                status = ReportStatus.GENERATED
            )
            
            // Save report to Firestore
            saveReport(report)
            
            Log.d("AutomatedReportRepository", "Report generated successfully: ${report.id}")
            Result.success(report)
            
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error generating report", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate sales summary for the report period
     */
    private suspend fun generateSalesSummary(period: ReportPeriod): SalesSummary {
        return try {
            // Use existing order repository data instead of direct Firestore queries
            val orders = orderRepository?.orders?.value?.filter { order ->
                order.orderDate >= period.startDate && order.orderDate <= period.endDate
            } ?: emptyList()
            
            Log.d("AutomatedReportRepository", "Found ${orders.size} orders for period ${period.startDate} to ${period.endDate}")
            
            val totalSales = orders.sumOf { it.totalAmount }
            val totalOrders = orders.size
            val averageOrderValue = if (totalOrders > 0) totalSales / totalOrders else 0.0
            
            // Calculate sales by payment method (simplified since paymentMethod is not in Order model)
            val salesByPaymentMethod = mapOf(
                "নগদ" to orders.filter { it.paidAmount > 0 }.sumOf { it.paidAmount },
                "বকেয়া" to orders.sumOf { it.remainingAmount }
            )
            
            // Calculate sales by customer type (simplified)
            val customerIds = orders.map { it.customerId }.distinct()
            
            // Simplified sales by customer type calculation
            val salesByCustomerType = mapOf(
                "নিয়মিত" to orders.sumOf { it.totalAmount } * 0.8,
                "নতুন" to orders.sumOf { it.totalAmount } * 0.2
            )
            
            // Calculate sales growth (simplified - compare with previous period)
            val salesGrowth = 0.0 // TODO: Implement proper growth calculation
            
            SalesSummary(
                totalSales = totalSales,
                totalOrders = totalOrders,
                averageOrderValue = averageOrderValue,
                salesByPaymentMethod = salesByPaymentMethod,
                salesByCustomerType = salesByCustomerType,
                salesGrowth = salesGrowth,
                salesTrend = when {
                    salesGrowth > 5 -> SalesTrend.INCREASING
                    salesGrowth < -5 -> SalesTrend.DECREASING
                    else -> SalesTrend.STABLE
                }
            )
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error generating sales summary", e)
            // Return empty summary on error
            SalesSummary()
        }
    }
    
    /**
     * Generate order summary for the report period
     */
    private suspend fun generateOrderSummary(period: ReportPeriod): OrderSummary {
        return try {
            // Use existing order repository data
            val orders = orderRepository?.orders?.value?.filter { order ->
                order.orderDate >= period.startDate && order.orderDate <= period.endDate
            } ?: emptyList()
            
            Log.d("AutomatedReportRepository", "Generating order summary for ${orders.size} orders")
            
            val totalOrders = orders.size
            val completedOrders = orders.count { it.orderStatus == OrderStatus.DELIVERED }
            val pendingOrders = orders.count { it.orderStatus == OrderStatus.PENDING || it.orderStatus == OrderStatus.CONFIRMED }
            val cancelledOrders = orders.count { it.orderStatus == OrderStatus.CANCELLED }
            
            val ordersByStatus = mapOf(
                "সম্পন্ন" to completedOrders,
                "প্রক্রিয়াধীন" to pendingOrders,
                "বাতিল" to cancelledOrders
            )
            
            // For priority, we'll use a simplified approach since priority isn't in the current Order model
            val ordersByPriority = mapOf(
                "উচ্চ" to (totalOrders * 0.2).toInt(),
                "মধ্যম" to (totalOrders * 0.6).toInt(),
                "কম" to (totalOrders * 0.2).toInt()
            )
            
            // Calculate average processing time (simplified)
            val averageOrderProcessingTime = 2.0 // Default value, can be enhanced later
            val orderCompletionRate = if (totalOrders > 0) {
                (completedOrders.toDouble() / totalOrders) * 100
            } else 0.0
            
            OrderSummary(
                totalOrders = totalOrders,
                completedOrders = completedOrders,
                pendingOrders = pendingOrders,
                cancelledOrders = cancelledOrders,
                ordersByStatus = ordersByStatus,
                ordersByPriority = ordersByPriority,
                averageOrderProcessingTime = averageOrderProcessingTime,
                orderCompletionRate = orderCompletionRate
            )
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error generating order summary", e)
            OrderSummary()
        }
    }
    
    /**
     * Generate customer summary for the report period
     */
    private suspend fun generateCustomerSummary(period: ReportPeriod): CustomerSummary {
        return try {
            // Use existing customer and order repository data
            val customers = customerRepository?.customers?.value ?: emptyList()
            val orders = orderRepository?.orders?.value?.filter { order ->
                order.orderDate >= period.startDate && order.orderDate <= period.endDate
            } ?: emptyList()
            
            Log.d("AutomatedReportRepository", "Generating customer summary for ${customers.size} customers and ${orders.size} orders")
            
            val totalCustomers = customers.size
            val activeCustomerIds = orders.map { it.customerId }.distinct()
            val activeCustomers = activeCustomerIds.size
            val newCustomersCount = customers.filter { customer ->
                customer.createdDate >= period.startDate && customer.createdDate <= period.endDate
            }.size
            
            // Calculate simple customer segments
            val customerSegments = mapOf(
                "নতুন গ্রাহক" to newCustomersCount,
                "নিয়মিত গ্রাহক" to (activeCustomers - newCustomersCount).coerceAtLeast(0),
                "নিষ্ক্রিয় গ্রাহক" to (totalCustomers - activeCustomers).coerceAtLeast(0)
            )
            
            // Calculate retention rate (simplified)
            val retentionRate = if (totalCustomers > 0) {
                (activeCustomers.toDouble() / totalCustomers) * 100.0
            } else 0.0
            
            // Calculate average customer lifetime value (simplified)
            val totalRevenue = orders.sumOf { it.totalAmount }
            val avgCustomerLifetimeValue = if (activeCustomers > 0) {
                totalRevenue / activeCustomers
            } else 0.0
            
            // Create simple top customers list
            val topCustomersByRevenue = orders.groupBy { it.customerId }
                .map { (customerId, customerOrders) ->
                    val revenue = customerOrders.sumOf { it.totalAmount }
                    val customerName = customerOrders.firstOrNull()?.customerName ?: "Unknown"
                    CustomerRevenue(
                        customerId = customerId,
                        customerName = customerName,
                        totalRevenue = revenue,
                        orderCount = customerOrders.size,
                        averageOrderValue = if (customerOrders.isNotEmpty()) revenue / customerOrders.size else 0.0
                    )
                }
                .sortedByDescending { it.totalRevenue }
                .take(5)
            
            CustomerSummary(
                totalCustomers = totalCustomers,
                activeCustomers = activeCustomers,
                newCustomers = newCustomersCount,
                customersByType = customerSegments,
                customerRetentionRate = retentionRate,
                averageCustomerValue = avgCustomerLifetimeValue,
                topCustomersByRevenue = topCustomersByRevenue
            )
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error generating customer summary", e)
            CustomerSummary()
        }
    }
    
    /**
     * Generate inventory summary for the report period
     */
    private suspend fun generateInventorySummary(period: ReportPeriod): InventorySummary {
        return try {
            val userId = getCurrentUserId() ?: return InventorySummary()
            
            // Fetch cylinder stock from user-specific Firebase collection
            val stockSnapshot = firestore.collection("users")
                .document(userId)
                .collection("cylinder_stock")
                .get().await()
            val stockItems = stockSnapshot.documents.mapNotNull { doc ->
                try {
                    Triple(
                        doc.getString("cylinderType") ?: "",
                        doc.getLong("quantity")?.toInt() ?: 0,
                        doc.getDouble("pricePerUnit") ?: 0.0
                    )
                } catch (e: Exception) {
                    Log.e("AutomatedReportRepository", "Error parsing stock item", e)
                    null
                }
            }
            
            val totalStock = stockItems.sumOf { it.second }
            
            val stockByType = stockItems.associate { (type, quantity, _) ->
                type to quantity
            }
            
            // Identify low stock items (less than 20 units)
            val lowStockItems = stockItems.filter { it.second < 20 }
                .map { "${it.first} (${it.second})" }
            
            // Calculate stock value
            val stockValue = stockItems.sumOf { (_, quantity, price) ->
                quantity * price
            }
            
            // Calculate stock turnover rate (simplified)
            val stockTurnoverRate = 0.75 // Default value, can be enhanced with actual calculations
            
            // Calculate stock movement (simplified - based on recent orders)
            val recentOrdersQuery = firestore.collection("users")
                .document(userId)
                .collection("orders")
                .whereGreaterThanOrEqualTo("orderDate", period.startDate)
                .whereLessThanOrEqualTo("orderDate", period.endDate)
            
            val recentOrdersSnapshot = recentOrdersQuery.get().await()
            val totalOrderedCylinders = recentOrdersSnapshot.documents.sumOf { doc ->
                try {
                    val cylinders = doc.get("cylinders") as? List<*> ?: emptyList<Any>()
                    cylinders.size
                } catch (e: Exception) {
                    0
                }
            }
            
            val stockMovement = mapOf(
                "ইন" to 0, // Simplified - would need stock in tracking
                "আউট" to totalOrderedCylinders
            )
            
            InventorySummary(
                totalStock = totalStock,
                stockByType = stockByType,
                lowStockItems = lowStockItems,
                stockTurnoverRate = stockTurnoverRate,
                stockValue = stockValue,
                stockMovement = stockMovement
            )
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error generating inventory summary", e)
            InventorySummary()
        }
    }
    
    /**
     * Generate expense summary for the report period
     */
    private suspend fun generateExpenseSummary(period: ReportPeriod): ExpenseSummary {
        return try {
            val userId = getCurrentUserId() ?: return ExpenseSummary()
            
            // Fetch expenses from user-specific Firebase collection for the specified period
            val expensesQuery = firestore.collection("users")
                .document(userId)
                .collection("expenses")
                .whereGreaterThanOrEqualTo("date", period.startDate)
                .whereLessThanOrEqualTo("date", period.endDate)
            
            val expensesSnapshot = expensesQuery.get().await()
            val expenses = expensesSnapshot.documents.mapNotNull { doc ->
                try {
                    Triple(
                        doc.getDouble("amount") ?: 0.0,
                        doc.getString("category") ?: "অন্যান্য",
                        doc.getString("date") ?: ""
                    )
                } catch (e: Exception) {
                    Log.e("AutomatedReportRepository", "Error parsing expense", e)
                    null
                }
            }
            
            val totalExpenses = expenses.sumOf { it.first }
            val expenseCount = expenses.size
            
            // Group expenses by category
            val expensesByCategory = expenses.groupBy { it.second }
                .mapValues { (_, expenseList) -> expenseList.sumOf { it.first } }
            
            // Group expenses by date
            val expensesByDate = expenses.groupBy { it.third }
                .mapValues { (_, expenseList) -> expenseList.sumOf { it.first } }
            
            // Calculate average daily expense
            val averageDailyExpense = if (expensesByDate.isNotEmpty()) {
                totalExpenses / expensesByDate.size
            } else 0.0
            
            // Find largest expense
            val largestExpense = expenses.maxOfOrNull { it.first } ?: 0.0
            
            // Calculate expense growth (simplified - would need previous period data)
            val expenseGrowth = 0.0 // TODO: Implement proper growth calculation
            
            ExpenseSummary(
                totalExpenses = totalExpenses,
                expensesByCategory = expensesByCategory,
                expensesByDate = expensesByDate,
                averageDailyExpense = averageDailyExpense,
                expenseGrowth = expenseGrowth,
                largestExpense = largestExpense,
                expenseCount = expenseCount
            )
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error generating expense summary", e)
            ExpenseSummary()
        }
    }
    
    /**
     * Generate financial summary for the report period
     */
    private suspend fun generateFinancialSummary(
        period: ReportPeriod,
        salesSummary: SalesSummary,
        expenseSummary: ExpenseSummary
    ): FinancialSummary {
        val totalRevenue = salesSummary.totalSales
        val totalExpenses = expenseSummary.totalExpenses
        val netProfit = totalRevenue - totalExpenses
        val profitMargin = if (totalRevenue > 0) (netProfit / totalRevenue) * 100 else 0.0
        
        // Mock cash flow and outstanding payments
        val cashFlow = 15000.0
        val outstandingPayments = 8000.0
        
        val revenueGrowth = salesSummary.salesGrowth
        
        return FinancialSummary(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMargin = profitMargin,
            cashFlow = cashFlow,
            outstandingPayments = outstandingPayments,
            expensesByCategory = expenseSummary.expensesByCategory,
            revenueGrowth = revenueGrowth
        )
    }
    
    /**
     * Generate top performers for the report period
     */
    private suspend fun generateTopPerformers(period: ReportPeriod): TopPerformers {
        // Mock data for top performers
        val topCustomers = listOf(
            CustomerRevenue(
                customerId = "1",
                customerName = "রহিম উদ্দিন",
                totalRevenue = 15000.0,
                orderCount = 8,
                averageOrderValue = 1875.0
            ),
            CustomerRevenue(
                customerId = "2",
                customerName = "করিম আহমদ",
                totalRevenue = 12000.0,
                orderCount = 6,
                averageOrderValue = 2000.0
            )
        )
        
        val topProducts = listOf(
            ProductSales(
                productType = "১২ কেজি",
                quantitySold = 50,
                revenue = 25000.0,
                profitMargin = 25.0
            ),
            ProductSales(
                productType = "৫ কেজি",
                quantitySold = 30,
                revenue = 15000.0,
                profitMargin = 25.0
            )
        )
        
        val topAreas = listOf(
            AreaSales(
                area = "ঢাকা",
                totalSales = 30000.0,
                orderCount = 15
            ),
            AreaSales(
                area = "চট্টগ্রাম",
                totalSales = 20000.0,
                orderCount = 10
            )
        )
        
        val topDays = listOf(
            DayPerformance(
                date = "2024-01-15",
                sales = 8000.0,
                orders = 5,
                profit = 2000.0
            ),
            DayPerformance(
                date = "2024-01-20",
                sales = 7500.0,
                orders = 4,
                profit = 1875.0
            )
        )
        
        return TopPerformers(
            topCustomers = topCustomers,
            topProducts = topProducts,
            topSalesAreas = topAreas,
            bestPerformingDays = topDays
        )
    }
    
    /**
     * Generate business insights based on data analysis
     */
    private fun generateBusinessInsights(
        salesSummary: SalesSummary,
        orderSummary: OrderSummary,
        customerSummary: CustomerSummary,
        inventorySummary: InventorySummary,
        financialSummary: FinancialSummary
    ): List<BusinessInsight> {
        val insights = mutableListOf<BusinessInsight>()
        
        // Sales insights
        if (salesSummary.salesGrowth > 20) {
            insights.add(
                BusinessInsight(
                    type = InsightType.SALES,
                    title = "উচ্চ বিক্রয় বৃদ্ধি",
                    description = "বিক্রয় ${salesSummary.salesGrowth.roundToInt()}% বৃদ্ধি পেয়েছে",
                    impact = InsightImpact.HIGH,
                    recommendation = "এই ট্রেন্ড বজায় রাখতে মার্কেটিং বাড়ান"
                )
            )
        } else if (salesSummary.salesGrowth < -10) {
            insights.add(
                BusinessInsight(
                    type = InsightType.SALES,
                    title = "বিক্রয় হ্রাস",
                    description = "বিক্রয় ${salesSummary.salesGrowth.roundToInt()}% কমেছে",
                    impact = InsightImpact.HIGH,
                    actionRequired = true,
                    recommendation = "বিক্রয় কৌশল পুনর্বিবেচনা করুন"
                )
            )
        }
        
        // Inventory insights
        if (inventorySummary.lowStockItems.isNotEmpty()) {
            insights.add(
                BusinessInsight(
                    type = InsightType.INVENTORY,
                    title = "কম স্টক সতর্কতা",
                    description = "${inventorySummary.lowStockItems.size}টি পণ্যের স্টক কম",
                    impact = InsightImpact.MEDIUM,
                    actionRequired = true,
                    recommendation = "দ্রুত স্টক পূরণ করুন"
                )
            )
        }
        
        // Customer insights
        if (customerSummary.customerRetentionRate < 60) {
            insights.add(
                BusinessInsight(
                    type = InsightType.CUSTOMER,
                    title = "গ্রাহক ধরে রাখার হার কম",
                    description = "গ্রাহক ধরে রাখার হার ${customerSummary.customerRetentionRate.roundToInt()}%",
                    impact = InsightImpact.HIGH,
                    actionRequired = true,
                    recommendation = "গ্রাহক সেবা উন্নত করুন"
                )
            )
        }
        
        // Financial insights
        if (financialSummary.profitMargin < 15) {
            insights.add(
                BusinessInsight(
                    type = InsightType.FINANCIAL,
                    title = "কম লাভের মার্জিন",
                    description = "লাভের মার্জিন ${financialSummary.profitMargin.roundToInt()}%",
                    impact = InsightImpact.MEDIUM,
                    actionRequired = true,
                    recommendation = "খরচ কমান বা দাম বাড়ান"
                )
            )
        }
        
        return insights
    }
    
    /**
     * Generate recommendations based on insights
     */
    private fun generateRecommendations(insights: List<BusinessInsight>): List<String> {
        val recommendations = mutableListOf<String>()
        
        insights.forEach { insight ->
            if (insight.actionRequired && insight.recommendation.isNotEmpty()) {
                recommendations.add(insight.recommendation)
            }
        }
        
        // Add general recommendations
        recommendations.addAll(
            listOf(
                "নিয়মিত স্টক পর্যবেক্ষণ করুন",
                "গ্রাহক ফিডব্যাক সংগ্রহ করুন",
                "বিক্রয় ট্রেন্ড বিশ্লেষণ করুন",
                "খরচ নিয়ন্ত্রণে রাখুন",
                "নতুন গ্রাহক অর্জনে মনোযোগ দিন"
            )
        )
        
        return recommendations.distinct()
    }
    
    /**
     * Save report to Firestore
     */
    private suspend fun saveReport(report: AutomatedReport) {
        val reportsCollection = getReportsCollection()
        reportsCollection?.document(report.id)?.set(report)?.await()
    }
    
    /**
     * Get all reports
     */
    suspend fun getAllReports(): List<AutomatedReport> {
        return try {
            val reportsCollection = getReportsCollection() ?: return emptyList()
            val snapshot = reportsCollection
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AutomatedReport::class.java)
            }
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error fetching reports", e)
            emptyList()
        }
    }
    
    /**
     * Get reports by type
     */
    suspend fun getReportsByType(reportType: ReportType): List<AutomatedReport> {
        return try {
            val reportsCollection = getReportsCollection() ?: return emptyList()
            val snapshot = reportsCollection
                .whereEqualTo("reportType", reportType.name)
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(AutomatedReport::class.java)
            }
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error fetching reports by type", e)
            emptyList()
        }
    }
    
    /**
     * Delete report
     */
    suspend fun deleteReport(reportId: String): Boolean {
        return try {
            val reportsCollection = getReportsCollection() ?: return false
            reportsCollection.document(reportId).delete().await()
            true
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error deleting report", e)
            false
        }
    }
    
    /**
     * Save report configuration
     */
    suspend fun saveReportConfig(config: ReportConfig) {
        val configCollection = getConfigCollection()
        configCollection?.document(config.id)?.set(config)?.await()
    }
    
    /**
     * Get report configurations
     */
    suspend fun getReportConfigs(): List<ReportConfig> {
        return try {
            val configCollection = getConfigCollection() ?: return emptyList()
            val snapshot = configCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(ReportConfig::class.java)
            }
        } catch (e: Exception) {
            Log.e("AutomatedReportRepository", "Error fetching report configs", e)
            emptyList()
        }
    }
    
    /**
     * Get previous period for comparison
     */
    private fun getPreviousPeriod(currentPeriod: ReportPeriod): ReportPeriod {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startDate = sdf.parse(currentPeriod.startDate)
        val endDate = sdf.parse(currentPeriod.endDate)
        
        val calendar = Calendar.getInstance()
        calendar.time = startDate
        val daysDiff = ((endDate.time - startDate.time) / (1000 * 60 * 60 * 24)).toInt()
        
        calendar.add(Calendar.DAY_OF_MONTH, -daysDiff)
        val prevStartDate = calendar.time
        
        calendar.time = endDate
        calendar.add(Calendar.DAY_OF_MONTH, -daysDiff)
        val prevEndDate = calendar.time
        
        return ReportPeriod(
            startDate = sdf.format(prevStartDate),
            endDate = sdf.format(prevEndDate),
            periodDescription = "পূর্ববর্তী পিরিয়ড"
        )
    }
    
    /**
     * Get date range for report type
     */
    private fun getDateRangeForReportType(reportType: ReportType): ReportPeriod {
        val calendar = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = sdf.format(calendar.time)
        
        when (reportType) {
            ReportType.DAILY -> {
                // Today
                val startDate = endDate
                return ReportPeriod(startDate, endDate, "আজকের রিপোর্ট")
            }
            ReportType.WEEKLY -> {
                // Last 7 days
                calendar.add(Calendar.DAY_OF_MONTH, -6)
                val startDate = sdf.format(calendar.time)
                return ReportPeriod(startDate, endDate, "সাপ্তাহিক রিপোর্ট")
            }
            ReportType.MONTHLY -> {
                // This month
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = sdf.format(calendar.time)
                return ReportPeriod(startDate, endDate, "মাসিক রিপোর্ট")
            }
            ReportType.QUARTERLY -> {
                // Last 3 months
                calendar.add(Calendar.MONTH, -2)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val startDate = sdf.format(calendar.time)
                return ReportPeriod(startDate, endDate, "ত্রৈমাসিক রিপোর্ট")
            }
            ReportType.YEARLY -> {
                // This year
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                val startDate = sdf.format(calendar.time)
                return ReportPeriod(startDate, endDate, "বার্ষিক রিপোর্ট")
            }
            ReportType.CUSTOM -> {
                // Default to last 30 days
                calendar.add(Calendar.DAY_OF_MONTH, -29)
                val startDate = sdf.format(calendar.time)
                return ReportPeriod(startDate, endDate, "কাস্টম রিপোর্ট")
            }
        }
    }
    
    /**
     * Get current date in Bengali format
     */
    private fun getCurrentDateBengali(): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
        return sdf.format(Date())
    }
}
