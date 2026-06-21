package com.firebase.loginauth

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing an order in the gas cylinder business
 */
data class Order(
    val id: String = UUID.randomUUID().toString(),
    val orderNumber: String = generateOrderNumber(),
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val orderItems: List<OrderItem> = emptyList(),
    val totalAmount: Double = 0.0,
    val paidAmount: Double = 0.0,
    val remainingAmount: Double = totalAmount - paidAmount,
    val orderStatus: OrderStatus = OrderStatus.PENDING,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING,
    val orderDate: String = getCurrentDateTime(),
    val deliveryDate: String = "",
    val completedDate: String = "",
    val notes: String = "",
    val priority: OrderPriority = OrderPriority.NORMAL,
    val deliveryArea: String = "",
    val estimatedDeliveryTime: String = "",
    val createdBy: String = "System",
    val lastUpdated: String = getCurrentDateTime(),
    // Stock deduction tracking fields
    val stockDeducted: Boolean = false,
    val stockDeductionDate: String? = null
)

/**
 * Data class for individual order items
 */
data class OrderItem(
    val id: String = UUID.randomUUID().toString(),
    val cylinderType: CylinderType = CylinderType.KG_12,
    val quantity: Int = 1,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = quantity * unitPrice,
    val notes: String = ""
)

// CylinderType enum is defined in CylinderStock.kt - using that instead

/**
 * Enum for order status
 */
enum class OrderStatus(val displayNameBn: String, val displayNameEn: String) {
    PENDING("অপেক্ষমান", "Pending"),
    CONFIRMED("নিশ্চিত", "Confirmed"),
    PROCESSING("প্রক্রিয়াধীন", "Processing"),
    READY_FOR_DELIVERY("ডেলিভারির জন্য প্রস্তুত", "Ready for Delivery"),
    OUT_FOR_DELIVERY("ডেলিভারিতে", "Out for Delivery"),
    DELIVERED("সরবরাহ সম্পন্ন", "Delivered"),
    CANCELLED("বাতিল", "Cancelled"),
    RETURNED("ফেরত", "Returned")
}

/**
 * Enum for order priority
 */
enum class OrderPriority(val displayNameBn: String, val displayNameEn: String) {
    LOW("কম", "Low"),
    NORMAL("সাধারণ", "Normal"),
    HIGH("উচ্চ", "High"),
    URGENT("জরুরি", "Urgent"),
    IRREGULAR("অনিয়মিত", "Irregular")
}

/**
 * Data class for order statistics
 */
data class OrderStats(
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val confirmedOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val totalRevenue: Double = 0.0,
    val pendingPayments: Double = 0.0,
    val todaysOrders: Int = 0,
    val thisWeekOrders: Int = 0,
    val thisMonthOrders: Int = 0
)

/**
 * Data class for order search and filter
 */
data class OrderFilter(
    val searchQuery: String = "",
    val orderStatus: OrderStatus? = null,
    val paymentStatus: PaymentStatus? = null,
    val deliveryStatus: DeliveryStatus? = null,
    val priority: OrderPriority? = null,
    val dateFrom: String = "",
    val dateTo: String = "",
    val customerId: String = "",
    val deliveryArea: String = ""
)

/**
 * Utility function to generate order number
 */
private fun generateOrderNumber(): String {
    val timestamp = System.currentTimeMillis()
    val random = (1000..9999).random()
    return "ORD-${timestamp.toString().takeLast(6)}-$random"
}

/**
 * Utility function to get current date and time
 */
private fun getCurrentDateTime(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy, hh:mm a", Locale("bn", "BD"))
    return sdf.format(Date())
}

/**
 * Utility function to get current date only
 */
fun getCurrentDate(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
    return sdf.format(Date())
}

/**
 * Utility function to calculate order total
 */
fun calculateOrderTotal(items: List<OrderItem>): Double {
    return items.sumOf { it.totalPrice }
}

/**
 * Utility function to get order status color
 */
fun getOrderStatusColor(status: OrderStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        OrderStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        OrderStatus.CONFIRMED -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        OrderStatus.PROCESSING -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
        OrderStatus.READY_FOR_DELIVERY -> androidx.compose.ui.graphics.Color(0xFF00BCD4) // Cyan
        OrderStatus.OUT_FOR_DELIVERY -> androidx.compose.ui.graphics.Color(0xFF3F51B5) // Indigo
        OrderStatus.DELIVERED -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        OrderStatus.CANCELLED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        OrderStatus.RETURNED -> androidx.compose.ui.graphics.Color(0xFF795548) // Brown
    }
}

/**
 * Utility function to get payment status color
 */
fun getPaymentStatusColor(status: PaymentStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        PaymentStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        PaymentStatus.PARTIAL -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        PaymentStatus.PAID -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        PaymentStatus.OVERDUE -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
    }
}

/**
 * Utility function to get delivery status color
 */
fun getDeliveryStatusColor(status: DeliveryStatus): androidx.compose.ui.graphics.Color {
    return when (status) {
        DeliveryStatus.PENDING -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        DeliveryStatus.IN_TRANSIT -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
        DeliveryStatus.DELIVERED -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        DeliveryStatus.CANCELLED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
    }
}

/**
 * Utility function to get priority color
 */
fun getPriorityColor(priority: OrderPriority): androidx.compose.ui.graphics.Color {
    return when (priority) {
        OrderPriority.LOW -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        OrderPriority.NORMAL -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        OrderPriority.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        OrderPriority.URGENT -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        OrderPriority.IRREGULAR -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
    }
}
