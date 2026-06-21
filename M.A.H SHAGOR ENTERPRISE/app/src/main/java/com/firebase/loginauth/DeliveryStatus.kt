package com.firebase.loginauth

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a delivery entry in the gas cylinder business
 */
data class DeliveryEntry(
    val id: String = UUID.randomUUID().toString(),
    val orderId: String = "",
    val orderNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val deliveryAddress: String = "",
    val cylinderDetails: String = "",
    val totalAmount: Double = 0.0,
    val deliveryStatus: DeliveryStatusType = DeliveryStatusType.PENDING,
    val deliveryPerson: String = "",
    val deliveryPersonPhone: String = "",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val actualDeliveryDate: String = "",
    val actualDeliveryTime: String = "",
    val notes: String = "",
    val priority: DeliveryPriority = DeliveryPriority.NORMAL,
    val deliveryArea: String = "",
    val estimatedDuration: String = "",
    val createdBy: String = "System",
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdated: String = getCurrentDateTimeForDelivery()
)

/**
 * Enum for delivery status types
 */
enum class DeliveryStatusType(val displayNameBn: String, val displayNameEn: String) {
    PENDING("অপেক্ষমান", "Pending"),
    ASSIGNED("নিযুক্ত", "Assigned"),
    IN_TRANSIT("পথে", "In Transit"),
    OUT_FOR_DELIVERY("ডেলিভারিতে", "Out for Delivery"),
    DELIVERED("সরবরাহ সম্পন্ন", "Delivered"),
    FAILED("ব্যর্থ", "Failed"),
    RETURNED("ফেরত", "Returned"),
    CANCELLED("বাতিল", "Cancelled")
}

/**
 * Enum for delivery priority
 */
enum class DeliveryPriority(val displayNameBn: String, val displayNameEn: String) {
    LOW("কম", "Low"),
    NORMAL("সাধারণ", "Normal"),
    HIGH("উচ্চ", "High"),
    URGENT("জরুরি", "Urgent")
}

/**
 * Data class for delivery person
 */
data class DeliveryPerson(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val vehicleType: String = "",
    val vehicleNumber: String = "",
    val isAvailable: Boolean = true,
    val currentLocation: String = "",
    val totalDeliveries: Int = 0,
    val rating: Double = 5.0
)

/**
 * Data class for delivery statistics
 */
data class DeliveryStats(
    val totalDeliveries: Int = 0,
    val pendingDeliveries: Int = 0,
    val inTransitDeliveries: Int = 0,
    val completedDeliveries: Int = 0,
    val failedDeliveries: Int = 0,
    val todayDeliveries: Int = 0,
    val averageDeliveryTime: String = "0 মিনিট"
)

/**
 * Data class for delivery filter
 */
data class DeliveryFilter(
    val status: DeliveryStatusType? = null,
    val deliveryPerson: String = "",
    val dateFrom: String = "",
    val dateTo: String = "",
    val area: String = "",
    val priority: DeliveryPriority? = null
)

// Utility function to generate delivery tracking number
fun generateDeliveryTrackingNumber(): String {
    val dateFormat = SimpleDateFormat("yyMMdd", Locale.getDefault())
    val currentDate = dateFormat.format(Date())
    val randomNumber = (1000..9999).random()
    return "DEL$currentDate$randomNumber"
}

// Utility function to get current date and time in Bengali format
fun getCurrentDateTimeForDelivery(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
    return dateFormat.format(Date())
}

// Utility function to get current date only
fun getCurrentDateForDelivery(): String {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return dateFormat.format(Date())
}

// Utility function to get delivery status color
fun getDeliveryStatusColor(status: DeliveryStatusType): androidx.compose.ui.graphics.Color {
    return when (status) {
        DeliveryStatusType.PENDING -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        DeliveryStatusType.ASSIGNED -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        DeliveryStatusType.IN_TRANSIT -> androidx.compose.ui.graphics.Color(0xFF9C27B0) // Purple
        DeliveryStatusType.OUT_FOR_DELIVERY -> androidx.compose.ui.graphics.Color(0xFF3F51B5) // Indigo
        DeliveryStatusType.DELIVERED -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        DeliveryStatusType.FAILED -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
        DeliveryStatusType.RETURNED -> androidx.compose.ui.graphics.Color(0xFF795548) // Brown
        DeliveryStatusType.CANCELLED -> androidx.compose.ui.graphics.Color(0xFF607D8B) // Blue Grey
    }
}

// Utility function to get delivery priority color
fun getDeliveryPriorityColor(priority: DeliveryPriority): androidx.compose.ui.graphics.Color {
    return when (priority) {
        DeliveryPriority.LOW -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        DeliveryPriority.NORMAL -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
        DeliveryPriority.HIGH -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        DeliveryPriority.URGENT -> androidx.compose.ui.graphics.Color(0xFFF44336) // Red
    }
}

// Utility function to calculate estimated delivery time
fun calculateEstimatedDeliveryTime(area: String): String {
    return when (area.lowercase()) {
        "ঢাকা", "dhaka" -> "৩০-৪৫ মিনিট"
        "চট্টগ্রাম", "chittagong" -> "৪৫-৬০ মিনিট"
        "সিলেট", "sylhet" -> "৬০-৯০ মিনিট"
        else -> "৩০-৬০ মিনিট"
    }
}
