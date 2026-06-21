package com.firebase.loginauth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Automated Report Dashboard Backend
 * Provides real-time business intelligence and analytics
 */
class AutomatedReportDashboard(private val context: Context) {
    
    private val repository = AutomatedReportRepository()
    private val orderRepository = OrderRepository.getInstance(context)
    private val customerRepository = CustomerRepository(context)
    private val stockRepository = CylinderStockRepository(context)
    private val expenseRepository = ExpenseRepository(context)
    private val cashDepositRepository = CashDepositRepository(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Dashboard State
    private val _dashboardState = MutableStateFlow(DashboardState())
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
    
    // Real-time Analytics
    private val _realtimeAnalytics = MutableStateFlow(RealtimeAnalytics())
    val realtimeAnalytics: StateFlow<RealtimeAnalytics> = _realtimeAnalytics.asStateFlow()
    
    // Report Generation Status
    private val _reportGenerationStatus = MutableStateFlow<ReportGenerationStatus?>(null)
    val reportGenerationStatus: StateFlow<ReportGenerationStatus?> = _reportGenerationStatus.asStateFlow()
    
    companion object {
        private const val TAG = "AutomatedReportDashboard"
        
        @Volatile
        private var INSTANCE: AutomatedReportDashboard? = null
        
        fun getInstance(context: Context): AutomatedReportDashboard {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AutomatedReportDashboard(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    init {
        initializeDashboard()
    }
    
    /**
     * Initialize dashboard with real-time data monitoring
     */
    private fun initializeDashboard() {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Initializing automated report dashboard...")
                
                // Load initial dashboard data
                loadDashboardData()
                
                // Start real-time analytics monitoring
                startRealtimeMonitoring()
                
                Log.d(TAG, "Dashboard initialized successfully")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing dashboard", e)
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    error = "ড্যাশবোর্ড লোড করতে ব্যর্থ: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Load comprehensive dashboard data
     */
    private suspend fun loadDashboardData() {
        _dashboardState.value = _dashboardState.value.copy(isLoading = true)
        
        try {
            // Get current period data
            val today = getCurrentDate()
            val thisWeek = getWeekRange()
            val thisMonth = getMonthRange()
            
            // Load business metrics
            val todayMetrics = calculateDailyMetrics(today)
            val weeklyMetrics = calculateWeeklyMetrics(thisWeek)
            val monthlyMetrics = calculateMonthlyMetrics(thisMonth)
            
            // Load recent reports
            val recentReports = repository.getAllReports().take(5)
            
            // Load top performers
            val topCustomers = getTopCustomers(thisMonth)
            val topProducts = getTopProducts(thisMonth)
            
            // Load financial overview
            val financialOverview = getFinancialOverview(thisMonth)
            
            // Load alerts and notifications
            val businessAlerts = generateBusinessAlerts()
            
            _dashboardState.value = DashboardState(
                isLoading = false,
                todayMetrics = todayMetrics,
                weeklyMetrics = weeklyMetrics,
                monthlyMetrics = monthlyMetrics,
                recentReports = recentReports,
                topCustomers = topCustomers,
                topProducts = topProducts,
                financialOverview = financialOverview,
                businessAlerts = businessAlerts,
                lastUpdated = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Dashboard data loaded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error loading dashboard data", e)
            _dashboardState.value = _dashboardState.value.copy(
                isLoading = false,
                error = "ডেটা লোড করতে ব্যর্থ: ${e.message}"
            )
        }
    }
    
    /**
     * Start real-time analytics monitoring
     */
    private fun startRealtimeMonitoring() {
        coroutineScope.launch {
            try {
                // Monitor orders in real-time
                orderRepository.orders.collect { orders ->
                    updateRealtimeAnalytics(orders)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in real-time monitoring", e)
            }
        }
    }
    
    /**
     * Update real-time analytics based on current data
     */
    private suspend fun updateRealtimeAnalytics(orders: List<Order>) {
        try {
            val today = getCurrentDate()
            val todayOrders = orders.filter { it.orderDate.startsWith(today) }
            
            val totalSalesToday = todayOrders.sumOf { it.totalAmount }
            val ordersCountToday = todayOrders.size
            val pendingOrders = orders.count { it.orderStatus == OrderStatus.PENDING }
            val deliveredToday = todayOrders.count { it.orderStatus == OrderStatus.DELIVERED }
            
            // Calculate trends
            val salesTrend = calculateSalesTrend(orders)
            val orderTrend = calculateOrderTrend(orders)
            
            _realtimeAnalytics.value = RealtimeAnalytics(
                totalSalesToday = totalSalesToday,
                ordersCountToday = ordersCountToday,
                pendingOrders = pendingOrders,
                deliveredToday = deliveredToday,
                salesTrend = salesTrend,
                orderTrend = orderTrend,
                lastUpdated = System.currentTimeMillis()
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating real-time analytics", e)
        }
    }
    
    /**
     * Generate automated report with progress tracking
     */
    suspend fun generateAutomatedReport(
        reportType: ReportType,
        customStartDate: String? = null,
        customEndDate: String? = null
    ): Result<AutomatedReport> {
        return try {
            Log.d(TAG, "Starting automated report generation: $reportType")
            
            _reportGenerationStatus.value = ReportGenerationStatus(
                isGenerating = true,
                progress = 0,
                currentStep = "প্রস্তুতি চলছে...",
                startTime = System.currentTimeMillis()
            )
            
            // Step 1: Data Collection (0-30%)
            updateGenerationProgress(10, "অর্ডার ডেটা সংগ্রহ করা হচ্ছে...")
            val orders = orderRepository.orders.value
            
            updateGenerationProgress(15, "গ্রাহক ডেটা সংগ্রহ করা হচ্ছে...")
            val customers = customerRepository.customers.value
            
            updateGenerationProgress(20, "স্টক ডেটা সংগ্রহ করা হচ্ছে...")
            val stockItems = stockRepository.stockItems.value
            
            updateGenerationProgress(25, "খরচ ডেটা সংগ্রহ করা হচ্ছে...")
            val expenses = expenseRepository.expenses.value
            
            updateGenerationProgress(30, "ক্যাশ ডিপোজিট ডেটা সংগ্রহ করা হচ্ছে...")
            val cashDeposits = cashDepositRepository.cashDeposits.value
            
            // Step 2: Data Processing (30-70%)
            updateGenerationProgress(40, "ডেটা বিশ্লেষণ করা হচ্ছে...")
            val result = repository.generateAutomatedReport(reportType, customStartDate, customEndDate)
            
            updateGenerationProgress(60, "রিপোর্ট তৈরি করা হচ্ছে...")
            
            // Step 3: Finalization (70-100%)
            updateGenerationProgress(80, "চূড়ান্ত করা হচ্ছে...")
            
            if (result.isSuccess) {
                updateGenerationProgress(100, "সম্পূর্ণ!")
                
                // Refresh dashboard data
                loadDashboardData()
                
                _reportGenerationStatus.value = ReportGenerationStatus(
                    isGenerating = false,
                    progress = 100,
                    currentStep = "সফলভাবে সম্পূর্ণ হয়েছে",
                    startTime = _reportGenerationStatus.value?.startTime ?: System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    isSuccess = true
                )
                
                Log.d(TAG, "Report generated successfully: ${result.getOrNull()?.id}")
                result
            } else {
                _reportGenerationStatus.value = ReportGenerationStatus(
                    isGenerating = false,
                    progress = 0,
                    currentStep = "ব্যর্থ: ${result.exceptionOrNull()?.message}",
                    startTime = _reportGenerationStatus.value?.startTime ?: System.currentTimeMillis(),
                    endTime = System.currentTimeMillis(),
                    isSuccess = false,
                    error = result.exceptionOrNull()?.message
                )
                result
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating automated report", e)
            _reportGenerationStatus.value = ReportGenerationStatus(
                isGenerating = false,
                progress = 0,
                currentStep = "ত্রুটি: ${e.message}",
                startTime = _reportGenerationStatus.value?.startTime ?: System.currentTimeMillis(),
                endTime = System.currentTimeMillis(),
                isSuccess = false,
                error = e.message
            )
            Result.failure(e)
        }
    }
    
    /**
     * Update report generation progress
     */
    private fun updateGenerationProgress(progress: Int, step: String) {
        _reportGenerationStatus.value = _reportGenerationStatus.value?.copy(
            progress = progress,
            currentStep = step
        )
    }
    
    /**
     * Calculate daily business metrics
     */
    private suspend fun calculateDailyMetrics(date: String): DailyMetrics {
        val orders = orderRepository.orders.value.filter { it.orderDate.startsWith(date) }
        val totalSales = orders.sumOf { it.totalAmount }
        val orderCount = orders.size
        val avgOrderValue = if (orderCount > 0) totalSales / orderCount else 0.0
        val deliveredOrders = orders.count { it.orderStatus == OrderStatus.DELIVERED }
        
        return DailyMetrics(
            date = date,
            totalSales = totalSales,
            orderCount = orderCount,
            avgOrderValue = avgOrderValue,
            deliveredOrders = deliveredOrders,
            deliveryRate = if (orderCount > 0) (deliveredOrders.toDouble() / orderCount) * 100 else 0.0
        )
    }
    
    /**
     * Calculate weekly business metrics
     */
    private suspend fun calculateWeeklyMetrics(weekRange: Pair<String, String>): WeeklyMetrics {
        val orders = orderRepository.orders.value.filter { order ->
            order.orderDate >= weekRange.first && order.orderDate <= weekRange.second
        }
        
        val totalSales = orders.sumOf { it.totalAmount }
        val orderCount = orders.size
        val newCustomers = customerRepository.customers.value.count { customer ->
            customer.createdDate >= weekRange.first && customer.createdDate <= weekRange.second
        }
        
        return WeeklyMetrics(
            weekStart = weekRange.first,
            weekEnd = weekRange.second,
            totalSales = totalSales,
            orderCount = orderCount,
            newCustomers = newCustomers,
            avgDailySales = totalSales / 7
        )
    }
    
    /**
     * Calculate monthly business metrics
     */
    private suspend fun calculateMonthlyMetrics(monthRange: Pair<String, String>): MonthlyMetrics {
        val orders = orderRepository.orders.value.filter { order ->
            order.orderDate >= monthRange.first && order.orderDate <= monthRange.second
        }
        
        val expenses = expenseRepository.expenses.value.filter { expense ->
            expense.date >= monthRange.first && expense.date <= monthRange.second
        }
        
        val totalSales = orders.sumOf { it.totalAmount }
        val totalExpenses = expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val netProfit = totalSales - totalExpenses
        val orderCount = orders.size
        
        return MonthlyMetrics(
            monthStart = monthRange.first,
            monthEnd = monthRange.second,
            totalSales = totalSales,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            orderCount = orderCount,
            profitMargin = if (totalSales > 0.0) (netProfit / totalSales) * 100.0 else 0.0
        )
    }
    
    /**
     * Get top customers for the period
     */
    private suspend fun getTopCustomers(period: Pair<String, String>): List<TopCustomer> {
        val orders = orderRepository.orders.value.filter { order ->
            order.orderDate >= period.first && order.orderDate <= period.second
        }
        
        return orders.groupBy { it.customerName }
            .map { (customerName, customerOrders) ->
                TopCustomer(
                    name = customerName,
                    totalOrders = customerOrders.size,
                    totalAmount = customerOrders.sumOf { it.totalAmount },
                    lastOrderDate = customerOrders.maxOfOrNull { it.orderDate } ?: ""
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(5)
    }
    
    /**
     * Get top products for the period
     */
    private suspend fun getTopProducts(period: Pair<String, String>): List<TopProduct> {
        val orders = orderRepository.orders.value.filter { order ->
            order.orderDate >= period.first && order.orderDate <= period.second
        }
        
        val productSales = mutableMapOf<String, Pair<Int, Double>>()
        
        orders.forEach { order ->
            order.orderItems.forEach { item ->
                val productName = item.cylinderType.displayNameBn
                val current = productSales[productName] ?: Pair(0, 0.0)
                productSales[productName] = Pair(
                    current.first + item.quantity,
                    current.second + item.totalPrice
                )
            }
        }
        
        return productSales.map { (productName, data) ->
            TopProduct(
                name = productName,
                totalQuantity = data.first,
                totalRevenue = data.second
            )
        }.sortedByDescending { it.totalRevenue }.take(5)
    }
    
    /**
     * Get financial overview
     */
    private suspend fun getFinancialOverview(period: Pair<String, String>): FinancialOverview {
        val orders = orderRepository.orders.value.filter { order ->
            order.orderDate >= period.first && order.orderDate <= period.second
        }
        
        val expenses = expenseRepository.expenses.value.filter { expense ->
            expense.date >= period.first && expense.date <= period.second
        }
        
        val cashDeposits = cashDepositRepository.cashDeposits.value.filter { deposit ->
            deposit.date >= period.first && deposit.date <= period.second
        }
        
        val totalRevenue = orders.sumOf { it.totalAmount }
        val totalExpenses = expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val totalCashDeposits = cashDeposits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val netProfit = totalRevenue - totalExpenses
        val outstandingDue = orders.sumOf { it.remainingAmount }
        
        return FinancialOverview(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            totalCashDeposits = totalCashDeposits,
            outstandingDue = outstandingDue,
            profitMargin = if (totalRevenue > 0.0) (netProfit / totalRevenue) * 100.0 else 0.0
        )
    }
    
    /**
     * Generate business alerts
     */
    private suspend fun generateBusinessAlerts(): List<BusinessAlert> {
        val alerts = mutableListOf<BusinessAlert>()
        
        try {
            // Low stock alerts
            val lowStockItems = stockRepository.stockItems.value.filter { 
                it.availableQuantity <= it.lowStockThreshold 
            }
            if (lowStockItems.isNotEmpty()) {
                alerts.add(
                    BusinessAlert(
                        type = AlertType.LOW_STOCK,
                        title = "কম স্টক সতর্কতা",
                        message = "${lowStockItems.size}টি পণ্যের স্টক কম",
                        severity = AlertSeverity.HIGH,
                        actionRequired = true
                    )
                )
            }
            
            // Overdue payments
            val overdueOrders = orderRepository.orders.value.filter { 
                it.paymentStatus == PaymentStatus.OVERDUE 
            }
            if (overdueOrders.isNotEmpty()) {
                alerts.add(
                    BusinessAlert(
                        type = AlertType.OVERDUE_PAYMENT,
                        title = "বকেয়া পেমেন্ট",
                        message = "${overdueOrders.size}টি অর্ডারের পেমেন্ট বকেয়া",
                        severity = AlertSeverity.MEDIUM,
                        actionRequired = true
                    )
                )
            }
            
            // Pending deliveries
            val pendingDeliveries = orderRepository.orders.value.filter { 
                it.orderStatus == OrderStatus.PENDING || it.orderStatus == OrderStatus.CONFIRMED 
            }
            if (pendingDeliveries.isNotEmpty()) {
                alerts.add(
                    BusinessAlert(
                        type = AlertType.PENDING_DELIVERY,
                        title = "পেন্ডিং ডেলিভারি",
                        message = "${pendingDeliveries.size}টি অর্ডার ডেলিভারির জন্য অপেক্ষমাণ",
                        severity = AlertSeverity.LOW,
                        actionRequired = false
                    )
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating business alerts", e)
        }
        
        return alerts
    }
    
    /**
     * Calculate sales trend
     */
    private fun calculateSalesTrend(orders: List<Order>): TrendIndicator {
        // Simple trend calculation based on last 7 days vs previous 7 days
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val twoWeeksAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        val thisWeekSales = orders.filter { it.orderDate >= weekAgo && it.orderDate <= today }
            .sumOf { it.totalAmount }
        
        val lastWeekSales = orders.filter { it.orderDate >= twoWeeksAgo && it.orderDate < weekAgo }
            .sumOf { it.totalAmount }
        
        val changePercent = if (lastWeekSales > 0) {
            ((thisWeekSales - lastWeekSales) / lastWeekSales) * 100
        } else 0.0
        
        return when {
            changePercent > 5 -> TrendIndicator.UP
            changePercent < -5 -> TrendIndicator.DOWN
            else -> TrendIndicator.STABLE
        }
    }
    
    /**
     * Calculate order trend
     */
    private fun calculateOrderTrend(orders: List<Order>): TrendIndicator {
        // Similar to sales trend but for order count
        val calendar = Calendar.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val weekAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, -7)
        val twoWeeksAgo = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        val thisWeekOrders = orders.filter { it.orderDate >= weekAgo && it.orderDate <= today }.size
        val lastWeekOrders = orders.filter { it.orderDate >= twoWeeksAgo && it.orderDate < weekAgo }.size
        
        val changePercent = if (lastWeekOrders > 0) {
            ((thisWeekOrders - lastWeekOrders).toDouble() / lastWeekOrders) * 100
        } else 0.0
        
        return when {
            changePercent > 10 -> TrendIndicator.UP
            changePercent < -10 -> TrendIndicator.DOWN
            else -> TrendIndicator.STABLE
        }
    }
    
    /**
     * Refresh dashboard data
     */
    fun refreshDashboard() {
        coroutineScope.launch {
            loadDashboardData()
        }
    }
    
    /**
     * Clear report generation status
     */
    fun clearReportGenerationStatus() {
        _reportGenerationStatus.value = null
    }
    
    // Utility functions
    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
    
    private fun getWeekRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val weekStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val weekEnd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        return Pair(weekStart, weekEnd)
    }
    
    private fun getMonthRange(): Pair<String, String> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        
        return Pair(monthStart, monthEnd)
    }
}

// Data classes for dashboard state
data class DashboardState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val todayMetrics: DailyMetrics? = null,
    val weeklyMetrics: WeeklyMetrics? = null,
    val monthlyMetrics: MonthlyMetrics? = null,
    val recentReports: List<AutomatedReport> = emptyList(),
    val topCustomers: List<TopCustomer> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val financialOverview: FinancialOverview? = null,
    val businessAlerts: List<BusinessAlert> = emptyList(),
    val lastUpdated: Long = 0L
)

data class RealtimeAnalytics(
    val totalSalesToday: Double = 0.0,
    val ordersCountToday: Int = 0,
    val pendingOrders: Int = 0,
    val deliveredToday: Int = 0,
    val salesTrend: TrendIndicator = TrendIndicator.STABLE,
    val orderTrend: TrendIndicator = TrendIndicator.STABLE,
    val lastUpdated: Long = 0L
)

data class ReportGenerationStatus(
    val isGenerating: Boolean = false,
    val progress: Int = 0,
    val currentStep: String = "",
    val startTime: Long = 0L,
    val endTime: Long? = null,
    val isSuccess: Boolean = false,
    val error: String? = null
)

data class DailyMetrics(
    val date: String,
    val totalSales: Double,
    val orderCount: Int,
    val avgOrderValue: Double,
    val deliveredOrders: Int,
    val deliveryRate: Double
)

data class WeeklyMetrics(
    val weekStart: String,
    val weekEnd: String,
    val totalSales: Double,
    val orderCount: Int,
    val newCustomers: Int,
    val avgDailySales: Double
)

data class MonthlyMetrics(
    val monthStart: String,
    val monthEnd: String,
    val totalSales: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val orderCount: Int,
    val profitMargin: Double
)

data class TopCustomer(
    val name: String,
    val totalOrders: Int,
    val totalAmount: Double,
    val lastOrderDate: String
)

data class TopProduct(
    val name: String,
    val totalQuantity: Int,
    val totalRevenue: Double
)

data class FinancialOverview(
    val totalRevenue: Double,
    val totalExpenses: Double,
    val netProfit: Double,
    val totalCashDeposits: Double,
    val outstandingDue: Double,
    val profitMargin: Double
)

data class BusinessAlert(
    val type: AlertType,
    val title: String,
    val message: String,
    val severity: AlertSeverity,
    val actionRequired: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

enum class AlertType {
    LOW_STOCK,
    OVERDUE_PAYMENT,
    PENDING_DELIVERY,
    HIGH_EXPENSES,
    PROFIT_DECLINE
}

enum class AlertSeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class TrendIndicator {
    UP,
    DOWN,
    STABLE
}
