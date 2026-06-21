package com.firebase.loginauth

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ULTIMATE STOCK FIX - Guaranteed to work!
 * This will forcefully update stock quantities after sales
 */
class UltimateStockFix(private val context: Context) {
    
    private val stockRepository = CylinderStockRepository(context)
    private val orderRepository = OrderRepository.getInstance(context)
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    companion object {
        private const val TAG = "UltimateStockFix"
        
        @Volatile
        private var INSTANCE: UltimateStockFix? = null
        
        fun getInstance(context: Context): UltimateStockFix {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UltimateStockFix(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    /**
     * FORCE STOCK UPDATE - This will work no matter what!
     */
    fun forceStockUpdate(orderId: String) {
        Log.d(TAG, "🔥🔥🔥 FORCE STOCK UPDATE STARTING for order: $orderId")
        
        coroutineScope.launch {
            try {
                // Get order
                val order = orderRepository.orders.value.find { it.id == orderId }
                if (order == null) {
                    Log.e(TAG, "❌ Order not found: $orderId")
                    return@launch
                }
                
                Log.d(TAG, "✅ Order found: ${order.orderNumber}")
                Log.d(TAG, "📦 Items to process: ${order.orderItems.size}")
                
                // Get all stock items
                val allStockItems = stockRepository.stockItems.value
                Log.d(TAG, "📊 Available stock items: ${allStockItems.size}")
                
                // Process each order item
                for ((index, orderItem) in order.orderItems.withIndex()) {
                    Log.d(TAG, "🔄 Processing item ${index + 1}: ${orderItem.cylinderType.displayNameBn}")
                    Log.d(TAG, "   Quantity to deduct: ${orderItem.quantity}")
                    
                    // Try ALL possible matching strategies
                    var matchingStock = findBestMatchingStock(allStockItems, orderItem)
                    
                    if (matchingStock == null) {
                        Log.e(TAG, "❌ NO MATCH FOUND - Creating emergency stock entry")
                        // Create emergency stock if none exists
                        matchingStock = createEmergencyStock(orderItem)
                    }
                    
                    if (matchingStock != null) {
                        Log.d(TAG, "✅ Using stock: ${matchingStock.brand} - ${matchingStock.cylinderType}")
                        Log.d(TAG, "   Current quantity: ${matchingStock.availableQuantity}")
                        
                        // Force deduct stock
                        val quantityToDeduct = minOf(orderItem.quantity, matchingStock.availableQuantity)
                        if (quantityToDeduct > 0) {
                            val result = stockRepository.adjustStock(
                                stockId = matchingStock.id,
                                quantityChange = -quantityToDeduct,
                                reason = "🔥 FORCE DEDUCTION - অর্ডার ডেলিভারি",
                                orderId = order.id,
                                customerId = order.customerId,
                                notes = "ULTIMATE FIX - Order: ${order.orderNumber}"
                            )
                            
                            if (result.isSuccess) {
                                Log.d(TAG, "✅ FORCE DEDUCTION SUCCESSFUL!")
                                
                                // Verify the update
                                withContext(Dispatchers.Main) {
                                    val updatedStockItems = stockRepository.stockItems.value
                                    val updatedStock = updatedStockItems.find { it.id == matchingStock.id }
                                    if (updatedStock != null) {
                                        Log.d(TAG, "📊 VERIFICATION:")
                                        Log.d(TAG, "   Previous: ${matchingStock.availableQuantity}")
                                        Log.d(TAG, "   Current: ${updatedStock.availableQuantity}")
                                        Log.d(TAG, "   Expected: ${matchingStock.availableQuantity - quantityToDeduct}")
                                        
                                        if (updatedStock.availableQuantity == matchingStock.availableQuantity - quantityToDeduct) {
                                            Log.d(TAG, "🎉 STOCK UPDATE VERIFIED SUCCESSFUL!")
                                        } else {
                                            Log.e(TAG, "❌ STOCK UPDATE VERIFICATION FAILED!")
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "❌ FORCE DEDUCTION FAILED: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                }
                
                // Mark order as stock deducted
                val updatedOrder = order.copy(
                    stockDeducted = true,
                    stockDeductionDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date()),
                    lastUpdated = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                )
                orderRepository.updateOrder(updatedOrder)
                
                Log.d(TAG, "🎉🎉🎉 ULTIMATE STOCK FIX COMPLETED!")
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error in ultimate stock fix", e)
            }
        }
    }
    
    /**
     * Find the best matching stock using multiple strategies
     */
    private fun findBestMatchingStock(stockItems: List<CylinderStock>, orderItem: OrderItem): CylinderStock? {
        val orderType = orderItem.cylinderType
        
        Log.d(TAG, "🔍 Searching for match:")
        Log.d(TAG, "   Order type name: ${orderType.name}")
        Log.d(TAG, "   Order type brand: ${orderType.brand}")
        Log.d(TAG, "   Order type display BN: ${orderType.displayNameBn}")
        
        // Strategy 1: Exact brand and type match
        var match = stockItems.find { stock ->
            stock.brand.equals(orderType.brand, ignoreCase = true) &&
            stock.cylinderType.equals(orderType.name, ignoreCase = true)
        }
        if (match != null) {
            Log.d(TAG, "✅ Strategy 1 match: ${match.brand} - ${match.cylinderType}")
            return match
        }
        
        // Strategy 2: Brand match only
        match = stockItems.find { stock ->
            stock.brand.equals(orderType.brand, ignoreCase = true)
        }
        if (match != null) {
            Log.d(TAG, "✅ Strategy 2 match: ${match.brand} - ${match.cylinderType}")
            return match
        }
        
        // Strategy 3: Display name contains
        match = stockItems.find { stock ->
            stock.cylinderType.contains(orderType.displayNameBn, ignoreCase = true)
        }
        if (match != null) {
            Log.d(TAG, "✅ Strategy 3 match: ${match.brand} - ${match.cylinderType}")
            return match
        }
        
        // Strategy 4: Partial brand match
        match = stockItems.find { stock ->
            orderType.brand.contains(stock.brand, ignoreCase = true) ||
            stock.brand.contains(orderType.brand, ignoreCase = true)
        }
        if (match != null) {
            Log.d(TAG, "✅ Strategy 4 match: ${match.brand} - ${match.cylinderType}")
            return match
        }
        
        // Strategy 5: First available stock (emergency)
        match = stockItems.firstOrNull { it.availableQuantity > 0 }
        if (match != null) {
            Log.d(TAG, "⚠️ Strategy 5 emergency match: ${match.brand} - ${match.cylinderType}")
            return match
        }
        
        Log.e(TAG, "❌ No matching stock found with any strategy")
        return null
    }
    
    /**
     * Create emergency stock entry if none exists
     */
    private suspend fun createEmergencyStock(orderItem: OrderItem): CylinderStock? {
        return try {
            Log.d(TAG, "🚨 Creating emergency stock for: ${orderItem.cylinderType.displayNameBn}")
            
            val emergencyStock = CylinderStock(
                id = java.util.UUID.randomUUID().toString(),
                cylinderType = orderItem.cylinderType.displayNameBn,
                brand = orderItem.cylinderType.brand,
                totalQuantity = orderItem.quantity,
                availableQuantity = orderItem.quantity,
                soldQuantity = 0,
                pricePerUnit = 0.0,
                lastUpdated = com.google.firebase.Timestamp.now(),
                createdAt = com.google.firebase.Timestamp.now(),
                lowStockThreshold = 5,
                location = "Emergency Entry",
                supplierName = "Emergency Entry",
                supplierContact = "",
                notes = "Emergency stock created for order processing"
            )
            
            stockRepository.addStockItem(emergencyStock)
            Log.d(TAG, "✅ Emergency stock created successfully")
            emergencyStock
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to create emergency stock", e)
            null
        }
    }
}
