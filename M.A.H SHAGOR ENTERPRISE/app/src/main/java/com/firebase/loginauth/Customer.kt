package com.firebase.loginauth

import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a customer in the gas cylinder business
 */
data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val phone: String = "",
    val address: String = "",
    val area: String = "",
    val customerType: CustomerType = CustomerType.REGULAR,
    val creditLimit: Double = 0.0,
    val currentCredit: Double = 0.0,
    val totalOrders: Int = 0,
    val lastOrderDate: String = "",
    val notes: String = "",
    val isActive: Boolean = true,
    val createdDate: String = getCurrentDateBengali(),
    val email: String = ""
)

/**
 * Enum for customer types
 */
enum class CustomerType(val displayNameBn: String, val displayNameEn: String) {
    REGULAR("নিয়মিত", "Regular"),
    VIP("ভিআইপি", "VIP"),
    COMMERCIAL("ব্যবসায়িক", "Commercial"),
    IRREGULAR("অনিয়মিত", "Irregular")
}

/**
 * Data class for customer order history
 */
data class CustomerOrder(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String = "",
    val orderDate: String = getCurrentDateBengali(),
    val cylinderCount: Int = 0,
    val totalAmount: Double = 0.0,
    val paymentStatus: PaymentStatus = PaymentStatus.PENDING,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.PENDING,
    val notes: String = ""
)

/**
 * Enum for payment status
 */
enum class PaymentStatus(val displayNameBn: String, val displayNameEn: String) {
    PENDING("বকেয়া", "Pending"),
    PAID("পরিশোধিত", "Paid"),
    PARTIAL("আংশিক", "Partial"),
    OVERDUE("অতিরিক্ত বকেয়া", "Overdue")
}

/**
 * Enum for delivery status
 */
enum class DeliveryStatus(val displayNameBn: String, val displayNameEn: String) {
    PENDING("অপেক্ষমান", "Pending"),
    IN_TRANSIT("পথে", "In Transit"),
    DELIVERED("সরবরাহ করা হয়েছে", "Delivered"),
    CANCELLED("বাতিল", "Cancelled")
}

/**
 * Utility function to get current date in Bengali format
 */
private fun getCurrentDateBengali(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
    return sdf.format(Date())
}

/**
 * Data class for customer statistics
 */
data class CustomerStats(
    val totalCustomers: Int = 0,
    val activeCustomers: Int = 0,
    val vipCustomers: Int = 0,
    val commercialCustomers: Int = 0,
    val totalCredit: Double = 0.0,
    val totalOrders: Int = 0
)
