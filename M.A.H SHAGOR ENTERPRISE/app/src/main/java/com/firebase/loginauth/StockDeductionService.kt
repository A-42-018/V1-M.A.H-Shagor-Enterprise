package com.firebase.loginauth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Service responsible for automatically deducting stock quantities when orders are delivered
 */
class StockDeductionService(private val context: Context) {
    
    private val stockRepository = CylinderStockRepository(context)
    private val orderRepository = OrderRepository.getInstance(context)
    private val activityLogger = RecentActivityRepository.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "StockDeductionService"
        
        @Volatile
        private var INSTANCE: StockDeductionService? = null
        
        fun getInstance(context: Context): StockDeductionService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StockDeductionService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Process stock deduction when an order is marked as delivered
     * This should be called from DeliveryRepository when delivery status is updated to DELIVERED
     */
    fun processDeliveryStockDeduction(orderId: String) {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Processing stock deduction for order: $orderId")
                
                // Get the order details
                val order = orderRepository.getOrderById(orderId)
                if (order == null) {
                    Log.e(TAG, "Order not found: $orderId")
                    return@launch
                }
                
                // Check if stock has already been deducted for this order
                if (order.stockDeducted) {
                    Log.d(TAG, "Stock already deducted for order: $orderId")
                    return@launch
                }
                
                Log.d(TAG, "Deducting stock for order: ${order.orderNumber} with ${order.orderItems.size} items")
                
                // Process each order item
                var allDeductionsSuccessful = true
                val deductionResults = mutableListOf<StockDeductionResult>()
                
                for (orderItem in order.orderItems) {
                    val result = deductStockForOrderItem(order, orderItem)
                    deductionResults.add(result)
                    
                    if (!result.success) {
                        allDeductionsSuccessful = false
                        Log.e(TAG, "Failed to deduct stock for item: ${orderItem.cylinderType.displayNameBn} - ${result.errorMessage}")
                    }
                }
                
                // Update order to mark stock as deducted (even if some deductions failed)
                // This prevents duplicate deduction attempts
                val updatedOrder = order.copy(
                    stockDeducted = true,
                    stockDeductionDate = getCurrentDateTime(),
                    lastUpdated = getCurrentDateTime()
                )
                orderRepository.updateOrder(updatedOrder)
                
                // Log the activity
                val successfulDeductions = deductionResults.count { it.success }
                val totalItems = deductionResults.size
                
                if (allDeductionsSuccessful) {
                    activityLogger.addActivity(
                        RecentActivityEntry(
                            type = ActivityType.STOCK_UPDATE,
                            title = "স্টক আপডেট",
                            description = "অর্ডার ডেলিভারির জন্য স্বয়ংক্রিয় স্টক কাটা হয়েছে",
                            customerName = order.customerName
                        )
                    )
                    Log.d(TAG, "Successfully deducted stock for all items in order: $orderId")
                } else {
                    activityLogger.addActivity(
                        RecentActivityEntry(
                            type = ActivityType.STOCK_UPDATE,
                            title = "স্টক আপডেট",
                            description = "অর্ডার ডেলিভারির জন্য আংশিক স্টক কাটা হয়েছে",
                            customerName = order.customerName
                        )
                    )
                    Log.w(TAG, "Partial stock deduction for order: $orderId ($successfulDeductions/$totalItems successful)")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error processing stock deduction for order: $orderId", e)
                activityLogger.addActivity(
                    RecentActivityEntry(
                        type = ActivityType.STOCK_UPDATE,
                        title = "ত্রুটি",
                        description = "স্টক কাটার সময় ত্রুটি: ${e.message}"
                    )
                )
            }
        }
    }
    
    /**
     * Deduct stock for a specific order item
     */
    private suspend fun deductStockForOrderItem(order: Order, orderItem: OrderItem): StockDeductionResult {
        return try {
            val cylinderType = orderItem.cylinderType
            val quantityToDeduct = orderItem.quantity
            
            Log.d(TAG, "Deducting ${quantityToDeduct} units of ${cylinderType.displayNameBn}")
            
            // Find matching stock item
            val stockItems = stockRepository.stockItems.value
            val matchingStock = findMatchingStockItem(stockItems, cylinderType)
            
            if (matchingStock == null) {
                return StockDeductionResult(
                    success = false,
                    cylinderType = cylinderType,
                    requestedQuantity = quantityToDeduct,
                    deductedQuantity = 0,
                    errorMessage = "স্টক আইটেম পাওয়া যায়নি: ${cylinderType.displayNameBn}"
                )
            }
            
            // Check if sufficient stock is available
            if (matchingStock.availableQuantity < quantityToDeduct) {
                Log.w(TAG, "Insufficient stock for ${cylinderType.displayNameBn}. Available: ${matchingStock.availableQuantity}, Required: $quantityToDeduct")
                
                // Deduct whatever is available
                val actualDeductedQuantity = matchingStock.availableQuantity
                if (actualDeductedQuantity > 0) {
                    stockRepository.adjustStock(
                        stockId = matchingStock.id,
                        quantityChange = -actualDeductedQuantity,
                        reason = "অর্ডার ডেলিভারি (আংশিক)",
                        orderId = order.id,
                        customerId = order.customerId,
                        notes = "অর্ডার: ${order.orderNumber}, গ্রাহক: ${order.customerName}"
                    )
                }
                
                return StockDeductionResult(
                    success = false,
                    cylinderType = cylinderType,
                    requestedQuantity = quantityToDeduct,
                    deductedQuantity = actualDeductedQuantity,
                    errorMessage = "অপর্যাপ্ত স্টক। প্রয়োজন: $quantityToDeduct, পাওয়া গেছে: ${matchingStock.availableQuantity}"
                )
            }
            
            // Deduct the stock
            stockRepository.adjustStock(
                stockId = matchingStock.id,
                quantityChange = -quantityToDeduct,
                reason = "অর্ডার ডেলিভারি",
                orderId = order.id,
                customerId = order.customerId,
                notes = "অর্ডার: ${order.orderNumber}, গ্রাহক: ${order.customerName}"
            )
            
            Log.d(TAG, "Successfully deducted $quantityToDeduct units of ${cylinderType.displayNameBn}")
            
            StockDeductionResult(
                success = true,
                cylinderType = cylinderType,
                requestedQuantity = quantityToDeduct,
                deductedQuantity = quantityToDeduct,
                errorMessage = null
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error deducting stock for ${orderItem.cylinderType.displayNameBn}", e)
            StockDeductionResult(
                success = false,
                cylinderType = orderItem.cylinderType,
                requestedQuantity = orderItem.quantity,
                deductedQuantity = 0,
                errorMessage = "স্টক কাটার সময় ত্রুটি: ${e.message}"
            )
        }
    }
    
    /**
     * Find matching stock item for a cylinder type
     * Matches by cylinder type and brand
     */
    private fun findMatchingStockItem(stockItems: List<CylinderStock>, cylinderType: CylinderType): CylinderStock? {
        // First try exact match by type and brand
        val exactMatch = stockItems.find { stock ->
            stock.cylinderType.equals(cylinderType.name, ignoreCase = true) &&
            stock.brand.equals(cylinderType.brand, ignoreCase = true)
        }
        
        if (exactMatch != null) {
            return exactMatch
        }
        
        // If no exact match, try to find by type only (for backward compatibility)
        val typeMatch = stockItems.find { stock ->
            stock.cylinderType.equals(cylinderType.name, ignoreCase = true)
        }
        
        if (typeMatch != null) {
            Log.d(TAG, "Using type-only match for ${cylinderType.displayNameBn}")
            return typeMatch
        }
        
        // Try to find by display name
        val displayNameMatch = stockItems.find { stock ->
            stock.cylinderType.contains(cylinderType.displayNameBn, ignoreCase = true) ||
            cylinderType.displayNameBn.contains(stock.cylinderType, ignoreCase = true)
        }
        
        if (displayNameMatch != null) {
            Log.d(TAG, "Using display name match for ${cylinderType.displayNameBn}")
            return displayNameMatch
        }
        
        Log.w(TAG, "No matching stock item found for ${cylinderType.displayNameBn}")
        return null
    }
    
    /**
     * Manually trigger stock deduction for an order (for testing or correction purposes)
     */
    fun manualStockDeduction(orderId: String, force: Boolean = false) {
        coroutineScope.launch {
            try {
                val order = orderRepository.getOrderById(orderId)
                if (order == null) {
                    Log.e(TAG, "Order not found for manual deduction: $orderId")
                    return@launch
                }
                
                if (order.stockDeducted && !force) {
                    Log.d(TAG, "Stock already deducted for order: $orderId. Use force=true to override.")
                    return@launch
                }
                
                Log.d(TAG, "Manual stock deduction for order: $orderId")
                processDeliveryStockDeduction(orderId)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in manual stock deduction for order: $orderId", e)
            }
        }
    }
    
    /**
     * Reverse stock deduction (add stock back) - for order cancellations or corrections
     */
    fun reverseStockDeduction(orderId: String, reason: String = "অর্ডার বাতিল") {
        coroutineScope.launch {
            try {
                Log.d(TAG, "Reversing stock deduction for order: $orderId")
                
                val order = orderRepository.getOrderById(orderId)
                if (order == null) {
                    Log.e(TAG, "Order not found for stock reversal: $orderId")
                    return@launch
                }
                
                if (!order.stockDeducted) {
                    Log.d(TAG, "No stock deduction to reverse for order: $orderId")
                    return@launch
                }
                
                // Add stock back for each order item
                for (orderItem in order.orderItems) {
                    val stockItems = stockRepository.stockItems.value
                    val matchingStock = findMatchingStockItem(stockItems, orderItem.cylinderType)
                    
                    if (matchingStock != null) {
                        stockRepository.adjustStock(
                            stockId = matchingStock.id,
                            quantityChange = orderItem.quantity, // Positive to add back
                            reason = reason,
                            orderId = order.id,
                            customerId = order.customerId,
                            notes = "স্টক ফেরত: অর্ডার ${order.orderNumber}"
                        )
                        
                        Log.d(TAG, "Reversed ${orderItem.quantity} units of ${orderItem.cylinderType.displayNameBn}")
                    }
                }
                
                // Update order to mark stock as not deducted
                val updatedOrder = order.copy(
                    stockDeducted = false,
                    stockDeductionDate = null,
                    lastUpdated = getCurrentDateTime()
                )
                orderRepository.updateOrder(updatedOrder)
                
                activityLogger.addActivity(
                    RecentActivityEntry(
                        type = ActivityType.STOCK_UPDATE,
                        title = "স্টক ফেরত",
                        description = "স্টক ফেরত দেওয়া হয়েছে: $reason",
                        customerName = order.customerName
                    )
                )
                
                Log.d(TAG, "Successfully reversed stock deduction for order: $orderId")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error reversing stock deduction for order: $orderId", e)
            }
        }
    }
}

/**
 * Result of stock deduction operation
 */
data class StockDeductionResult(
    val success: Boolean,
    val cylinderType: CylinderType,
    val requestedQuantity: Int,
    val deductedQuantity: Int,
    val errorMessage: String?
)

// Utility functions
private fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD"))
    return sdf.format(Date())
}
