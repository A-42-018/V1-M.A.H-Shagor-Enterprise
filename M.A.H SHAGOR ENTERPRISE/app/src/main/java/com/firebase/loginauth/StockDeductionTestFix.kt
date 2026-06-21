package com.firebase.loginauth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Enhanced Stock Deduction Service with immediate fixes and debugging
 */
class StockDeductionTestFix(private val context: Context) {
    
    private val stockRepository = CylinderStockRepository(context)
    private val orderRepository = OrderRepository.getInstance(context)
    private val deliveryRepository = DeliveryRepository(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "StockDeductionTestFix"
        
        @Volatile
        private var INSTANCE: StockDeductionTestFix? = null
        
        fun getInstance(context: Context): StockDeductionTestFix {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StockDeductionTestFix(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * Enhanced stock deduction with immediate execution and debugging
     */
    fun processImmediateStockDeduction(orderId: String) {
        Log.d(TAG, "🔥 IMMEDIATE STOCK DEDUCTION STARTED for order: $orderId")
        
        coroutineScope.launch {
            try {
                // Get the order details with detailed logging
                val allOrders = orderRepository.orders.value
                Log.d(TAG, "📋 Total orders in repository: ${allOrders.size}")
                
                val order = allOrders.find { it.id == orderId }
                if (order == null) {
                    Log.e(TAG, "❌ ORDER NOT FOUND: $orderId")
                    Log.d(TAG, "Available order IDs: ${allOrders.map { it.id }}")
                    return@launch
                }
                
                Log.d(TAG, "✅ Order found: ${order.orderNumber}")
                Log.d(TAG, "📦 Order items: ${order.orderItems.size}")
                Log.d(TAG, "📊 Order status: ${order.orderStatus}")
                Log.d(TAG, "🔄 Stock already deducted: ${order.stockDeducted}")
                
                // Check current stock levels
                val stockItems = stockRepository.stockItems.value
                Log.d(TAG, "📈 Current stock items: ${stockItems.size}")
                
                // Process each order item with detailed logging
                for ((index, orderItem) in order.orderItems.withIndex()) {
                    Log.d(TAG, "🔄 Processing item ${index + 1}/${order.orderItems.size}")
                    Log.d(TAG, "   Type: ${orderItem.cylinderType.displayNameBn}")
                    Log.d(TAG, "   Quantity to deduct: ${orderItem.quantity}")
                    
                    // Enhanced matching stock logic - try multiple approaches
                    val matchingStock = stockItems.find { stockItem ->
                        // Exact match on cylinder type name
                        stockItem.cylinderType.equals(orderItem.cylinderType.name, ignoreCase = true) ||
                        // Match on brand
                        stockItem.brand.equals(orderItem.cylinderType.brand, ignoreCase = true) ||
                        // Match on display name
                        stockItem.cylinderType.contains(orderItem.cylinderType.displayNameBn, ignoreCase = true) ||

                        // Partial brand match
                        orderItem.cylinderType.brand.contains(stockItem.brand, ignoreCase = true) ||
                        stockItem.brand.contains(orderItem.cylinderType.brand, ignoreCase = true) ||
                        // Match cylinder type enum name
                        stockItem.cylinderType.equals(orderItem.cylinderType.toString(), ignoreCase = true)
                    }
                    
                    if (matchingStock == null) {
                        Log.e(TAG, "❌ No matching stock found for: ${orderItem.cylinderType.displayNameBn}")
                        Log.d(TAG, "Available stock types: ${stockItems.map { "${it.cylinderType} (${it.brand})" }}")
                        continue
                    }
                    
                    Log.d(TAG, "✅ Matching stock found:")
                    Log.d(TAG, "   Stock ID: ${matchingStock.id}")
                    Log.d(TAG, "   Available quantity: ${matchingStock.availableQuantity}")
                    Log.d(TAG, "   Stock type: ${matchingStock.cylinderType}")
                    Log.d(TAG, "   Brand: ${matchingStock.brand}")
                    
                    // Check if sufficient stock
                    if (matchingStock.availableQuantity < orderItem.quantity) {
                        Log.w(TAG, "⚠️ Insufficient stock! Available: ${matchingStock.availableQuantity}, Required: ${orderItem.quantity}")
                    }
                    
                    // Perform the stock deduction
                    val quantityToDeduct = minOf(orderItem.quantity, matchingStock.availableQuantity)
                    if (quantityToDeduct > 0) {
                        Log.d(TAG, "🔄 Deducting $quantityToDeduct units...")
                        
                        val result = stockRepository.adjustStock(
                            stockId = matchingStock.id,
                            quantityChange = -quantityToDeduct,
                            reason = "অর্ডার ডেলিভারি - তাৎক্ষণিক",
                            orderId = order.id,
                            customerId = order.customerId,
                            notes = "অর্ডার: ${order.orderNumber}, গ্রাহক: ${order.customerName} - তাৎক্ষণিক স্টক কাটা"
                        )
                        
                        if (result.isSuccess) {
                            Log.d(TAG, "✅ Stock deduction successful for ${orderItem.cylinderType.displayNameBn}")
                            
                            // Verify the stock was actually updated
                            val updatedStockItems = stockRepository.stockItems.value
                            val updatedStock = updatedStockItems.find { it.id == matchingStock.id }
                            if (updatedStock != null) {
                                Log.d(TAG, "📊 Stock verification:")
                                Log.d(TAG, "   Previous quantity: ${matchingStock.availableQuantity}")
                                Log.d(TAG, "   Current quantity: ${updatedStock.availableQuantity}")
                                Log.d(TAG, "   Expected quantity: ${matchingStock.availableQuantity - quantityToDeduct}")
                                
                                if (updatedStock.availableQuantity == matchingStock.availableQuantity - quantityToDeduct) {
                                    Log.d(TAG, "✅ Stock update verified successfully!")
                                } else {
                                    Log.e(TAG, "❌ Stock update verification failed!")
                                }
                            }
                        } else {
                            Log.e(TAG, "❌ Stock deduction failed: ${result.exceptionOrNull()?.message}")
                        }
                    } else {
                        Log.w(TAG, "⚠️ No stock to deduct for ${orderItem.cylinderType.displayNameBn}")
                    }
                }
                
                // Update order to mark stock as deducted
                val updatedOrder = order.copy(
                    stockDeducted = true,
                    stockDeductionDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    lastUpdated = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                
                orderRepository.updateOrder(updatedOrder)
                Log.d(TAG, "✅ Order marked as stock deducted")
                
                Log.d(TAG, "🎉 IMMEDIATE STOCK DEDUCTION COMPLETED for order: $orderId")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in immediate stock deduction", e)
            }
        }
    }
    
    /**
     * Test and fix stock deduction for all delivered orders
     */
    fun testAndFixAllDeliveredOrders() {
        Log.d(TAG, "🔥 TESTING AND FIXING ALL DELIVERED ORDERS")
        
        coroutineScope.launch {
            try {
                val allOrders = orderRepository.orders.value
                val deliveredOrders = allOrders.filter { 
                    it.orderStatus == OrderStatus.DELIVERED && !it.stockDeducted 
                }
                
                Log.d(TAG, "📋 Found ${deliveredOrders.size} delivered orders without stock deduction")
                
                for (order in deliveredOrders) {
                    Log.d(TAG, "🔄 Processing delivered order: ${order.orderNumber}")
                    processImmediateStockDeduction(order.id)
                }
                
                Log.d(TAG, "🎉 COMPLETED TESTING AND FIXING ALL DELIVERED ORDERS")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in test and fix", e)
            }
        }
    }
    
    /**
     * Force stock deduction for a specific order regardless of status
     */
    fun forceStockDeduction(orderId: String) {
        Log.d(TAG, "🔥 FORCE STOCK DEDUCTION for order: $orderId")
        processImmediateStockDeduction(orderId)
    }
}
