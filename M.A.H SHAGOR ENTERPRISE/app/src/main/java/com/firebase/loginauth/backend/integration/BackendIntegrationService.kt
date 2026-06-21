package com.firebase.loginauth.backend.integration

import com.firebase.loginauth.backend.auth.AuthManager
import com.firebase.loginauth.backend.repositories.*
import com.firebase.loginauth.backend.sync.BackgroundSyncManager
import com.firebase.loginauth.database.CylinderItem
import com.firebase.loginauth.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackendIntegrationService @Inject constructor(
    private val authManager: AuthManager,
    private val stockRepository: StockBackendRepository,
    private val orderRepository: OrderBackendRepository,
    private val deliveryRepository: DeliveryBackendRepository,
    private val sellsRepository: SellsBackendRepository,
    private val dueAccountRepository: DueAccountBackendRepository,
    private val syncManager: BackgroundSyncManager
) {
    
    // **AUTHENTICATION OPERATIONS**
    
    suspend fun signIn(email: String, password: String) = authManager.signInWithEmailAndPassword(email, password)
    suspend fun signUp(email: String, password: String) = authManager.createUserWithEmailAndPassword(email, password)
    suspend fun signOut() = authManager.signOut()
    suspend fun resetPassword(email: String) = authManager.sendPasswordResetEmail(email)
    fun getCurrentUserId() = authManager.getCurrentUserId()
    fun isAuthenticated() = authManager.isUserAuthenticated()
    
    // **STOCK MANAGEMENT OPERATIONS**
    
    suspend fun addStock(brand: String, size: String, quantity: Int, pricePerUnit: Double) = 
        stockRepository.addStock(brand, size, quantity, pricePerUnit)
    
    suspend fun updateStockQuantity(stockId: String, newQuantity: Int) = 
        stockRepository.updateStockQuantity(stockId, newQuantity)
    
    suspend fun deleteStock(stockId: String) = stockRepository.deleteStock(stockId)
    
    fun getAllStocks(): Flow<List<StockEntity>> = stockRepository.getAllStocks()
    fun getLowStocks(): Flow<List<StockEntity>> = stockRepository.getLowStocks()
    
    suspend fun getStockById(stockId: String) = stockRepository.getStockById(stockId)
    suspend fun getTotalStockValue() = stockRepository.getTotalStockValue()
    suspend fun getStockCount() = stockRepository.getStockCount()
    
    // **ORDER MANAGEMENT OPERATIONS**
    
    suspend fun createOrder(
        customerName: String,
        customerPhone: String,
        customerAddress: String,
        cylinderItems: List<CylinderItem>,
        notes: String = "",
        priority: String = "Medium"
    ) = orderRepository.createOrder(customerName, customerPhone, customerAddress, cylinderItems, notes, priority)
    
    suspend fun updateOrderStatus(orderId: String, newStatus: String) = 
        orderRepository.updateOrderStatus(orderId, newStatus)
    
    suspend fun updatePaymentStatus(orderId: String, paymentStatus: String) = 
        orderRepository.updatePaymentStatus(orderId, paymentStatus)
    
    suspend fun markOrderAsDelivered(orderId: String) = orderRepository.markOrderAsDelivered(orderId)
    suspend fun deleteOrder(orderId: String) = orderRepository.deleteOrder(orderId)
    
    fun getAllOrders(): Flow<List<OrderEntity>> = orderRepository.getAllOrders()
    fun getPendingOrders(): Flow<List<OrderEntity>> = orderRepository.getPendingOrders()
    fun getDeliveredOrders(): Flow<List<OrderEntity>> = orderRepository.getDeliveredOrders()
    fun getOrdersByStatus(status: String): Flow<List<OrderEntity>> = orderRepository.getOrdersByStatus(status)
    fun getOrdersByCustomer(customerPhone: String): Flow<List<OrderEntity>> = orderRepository.getOrdersByCustomer(customerPhone)
    fun getOrdersByDateRange(startDate: Date, endDate: Date): Flow<List<OrderEntity>> = orderRepository.getOrdersByDateRange(startDate, endDate)
    
    suspend fun getOrderById(orderId: String) = orderRepository.getOrderById(orderId)
    suspend fun getTotalSalesAmount() = orderRepository.getTotalSalesAmount()
    suspend fun getOrderCountByStatus(status: String) = orderRepository.getOrderCountByStatus(status)
    
    // **DELIVERY MANAGEMENT OPERATIONS**
    
    suspend fun createDeliveryFromOrder(orderId: String, deliveryPersonName: String = "") = 
        deliveryRepository.createDeliveryFromOrder(orderId, deliveryPersonName)
    
    suspend fun confirmDelivery(deliveryId: String, deliveryNotes: String = "") = 
        deliveryRepository.confirmDelivery(deliveryId, deliveryNotes)
    
    suspend fun updateDeliveryStatus(deliveryId: String, newStatus: String) = 
        deliveryRepository.updateDeliveryStatus(deliveryId, newStatus)
    
    suspend fun deleteDelivery(deliveryId: String) = deliveryRepository.deleteDelivery(deliveryId)
    
    fun getAllDeliveries(): Flow<List<DeliveryEntity>> = deliveryRepository.getAllDeliveries()
    fun getPendingDeliveries(): Flow<List<DeliveryEntity>> = deliveryRepository.getPendingDeliveries()
    fun getDeliveriesByStatus(status: String): Flow<List<DeliveryEntity>> = deliveryRepository.getDeliveriesByStatus(status)
    
    suspend fun getDeliveryById(deliveryId: String) = deliveryRepository.getDeliveryById(deliveryId)
    suspend fun getDeliveryByOrderId(orderId: String) = deliveryRepository.getDeliveryByOrderId(orderId)
    suspend fun getCompletedDeliveryCount() = deliveryRepository.getCompletedDeliveryCount()
    
    // **SELLS MANAGEMENT OPERATIONS**
    
    suspend fun createDirectSell(
        customerName: String,
        customerPhone: String,
        cylinderItems: List<CylinderItem>,
        paymentMethod: String = "Cash",
        paidAmount: Double,
        discount: Double = 0.0,
        tax: Double = 0.0,
        salesPersonName: String = "",
        notes: String = ""
    ) = sellsRepository.createDirectSell(customerName, customerPhone, cylinderItems, paymentMethod, paidAmount, discount, tax, salesPersonName, notes)
    
    suspend fun updatePayment(sellId: String, additionalPayment: Double, paymentMethod: String = "Cash") = 
        sellsRepository.updatePayment(sellId, additionalPayment, paymentMethod)
    
    suspend fun deleteSell(sellId: String) = sellsRepository.deleteSell(sellId)
    
    fun getAllSells(): Flow<List<SellEntity>> = sellsRepository.getAllSells()
    fun getDueSells(): Flow<List<SellEntity>> = sellsRepository.getDueSells()
    fun getSellsByPaymentStatus(status: String): Flow<List<SellEntity>> = sellsRepository.getSellsByPaymentStatus(status)
    fun getSellsByDateRange(startDate: Date, endDate: Date): Flow<List<SellEntity>> = sellsRepository.getSellsByDateRange(startDate, endDate)
    
    suspend fun getSellById(sellId: String) = sellsRepository.getSellById(sellId)
    suspend fun getSellByOrderId(orderId: String) = sellsRepository.getSellByOrderId(orderId)
    
    // **SALES ANALYTICS**
    suspend fun getDailySales(date: Date) = sellsRepository.getDailySales(date)
    suspend fun getMonthlySales(year: Int, month: Int) = sellsRepository.getMonthlySales(year, month)
    suspend fun getCustomSales(startDate: Date, endDate: Date) = sellsRepository.getCustomSales(startDate, endDate)
    suspend fun getSalesAnalytics() = sellsRepository.getSalesAnalytics()
    
    // **DUE ACCOUNT MANAGEMENT OPERATIONS**
    
    suspend fun createDueAccount(
        customerName: String,
        customerPhone: String,
        customerAddress: String = "",
        dueAmount: Double,
        dueDate: Date,
        notes: String = "",
        priority: String = "Medium"
    ) = dueAccountRepository.createDueAccount(customerName, customerPhone, customerAddress, dueAmount, dueDate, notes, priority)
    
    suspend fun makePayment(
        dueAccountId: String,
        paymentAmount: Double,
        paymentMethod: String = "Cash",
        notes: String = ""
    ) = dueAccountRepository.makePayment(dueAccountId, paymentAmount, paymentMethod, notes)
    
    suspend fun clearPartialDue(dueAccountId: String) = dueAccountRepository.clearPartialDue(dueAccountId)
    suspend fun updateDueAccountStatus(dueAccountId: String, newStatus: String) = dueAccountRepository.updateDueAccountStatus(dueAccountId, newStatus)
    suspend fun markOverdueDueAccounts() = dueAccountRepository.markOverdueDueAccounts()
    suspend fun deleteDueAccount(dueAccountId: String) = dueAccountRepository.deleteDueAccount(dueAccountId)
    
    fun getAllDueAccounts(): Flow<List<DueAccountEntity>> = dueAccountRepository.getAllDueAccounts()
    fun getActiveDueAccounts(): Flow<List<DueAccountEntity>> = dueAccountRepository.getActiveDueAccounts()
    fun getDueAccountsByStatus(status: String): Flow<List<DueAccountEntity>> = dueAccountRepository.getDueAccountsByStatus(status)
    fun getDueAccountsByCustomer(customerPhone: String): Flow<List<DueAccountEntity>> = dueAccountRepository.getDueAccountsByCustomer(customerPhone)
    fun getOverdueDueAccounts(): Flow<List<DueAccountEntity>> = dueAccountRepository.getOverdueDueAccounts()
    
    suspend fun getDueAccountById(dueAccountId: String) = dueAccountRepository.getDueAccountById(dueAccountId)
    suspend fun getTotalDueAmount() = dueAccountRepository.getTotalDueAmount()
    suspend fun getActiveDueCount() = dueAccountRepository.getActiveDueCount()
    suspend fun getOverdueCount() = dueAccountRepository.getOverdueCount()
    
    // **SYNC MANAGEMENT OPERATIONS**
    
    suspend fun performFullSync() = syncManager.performFullSync()
    suspend fun syncStocksOnly() = syncManager.syncStocksOnly()
    suspend fun syncOrdersOnly() = syncManager.syncOrdersOnly()
    suspend fun syncDeliveriesOnly() = syncManager.syncDeliveriesOnly()
    suspend fun syncSellsOnly() = syncManager.syncSellsOnly()
    suspend fun syncDueAccountsOnly() = syncManager.syncDueAccountsOnly()
    suspend fun resolveConflicts() = syncManager.resolveConflicts()
    
    fun getSyncStatus() = syncManager.syncStatus
    fun getLastSyncTime() = syncManager.lastSyncTime
    fun getPendingSyncCount() = syncManager.pendingSyncCount
    fun getSyncStatusInfo() = syncManager.getSyncStatusInfo()
    
    fun startPeriodicSync() = syncManager.startPeriodicSync()
    fun stopPeriodicSync() = syncManager.stopPeriodicSync()
    
    // **BUSINESS FLOW OPERATIONS**
    
    /**
     * Complete business flow: Order -> Delivery -> Stock Update -> Sell Record -> Due Account (if needed)
     */
    suspend fun processCompleteDelivery(deliveryId: String, deliveryNotes: String = ""): Result<Boolean> {
        return try {
            // 1. Confirm delivery (this automatically updates stock and creates sell record)
            val deliveryResult = confirmDelivery(deliveryId, deliveryNotes)
            if (deliveryResult.isFailure) {
                return deliveryResult
            }
            
            // 2. Get the delivery and associated order
            val delivery = getDeliveryById(deliveryId)
            val order = delivery?.let { getOrderById(it.orderId) }
            
            if (order != null) {
                // 3. Create due account if payment is not complete
                if (order.paymentStatus == "Due" || order.paymentStatus == "Partial") {
                    val sell = getSellByOrderId(order.id)
                    if (sell != null && sell.dueAmount > 0) {
                        dueAccountRepository.createDueAccountFromSell(sell)
                    }
                }
            }
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get comprehensive business dashboard data
     */
    suspend fun getDashboardData(): BusinessDashboardData {
        return BusinessDashboardData(
            totalStockValue = getTotalStockValue(),
            stockCount = getStockCount(),
            pendingOrdersCount = getOrderCountByStatus("Pending"),
            deliveredOrdersCount = getOrderCountByStatus("Delivered"),
            totalSalesAmount = getTotalSalesAmount(),
            totalDueAmount = getTotalDueAmount(),
            activeDueCount = getActiveDueCount(),
            overdueCount = getOverdueCount(),
            completedDeliveryCount = getCompletedDeliveryCount(),
            syncStatusInfo = getSyncStatusInfo()
        )
    }
}

// **DATA CLASSES**

data class BusinessDashboardData(
    val totalStockValue: Double,
    val stockCount: Int,
    val pendingOrdersCount: Int,
    val deliveredOrdersCount: Int,
    val totalSalesAmount: Double,
    val totalDueAmount: Double,
    val activeDueCount: Int,
    val overdueCount: Int,
    val completedDeliveryCount: Int,
    val syncStatusInfo: com.firebase.loginauth.backend.sync.SyncStatusInfo
)
