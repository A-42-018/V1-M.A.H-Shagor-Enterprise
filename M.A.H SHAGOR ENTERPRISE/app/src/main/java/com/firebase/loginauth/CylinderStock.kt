package com.firebase.loginauth

import com.google.firebase.Timestamp
import java.util.*

data class CylinderStock(
    val id: String = "",
    val cylinderType: String = "", // e.g., "12kg", "5kg", "2.5kg"
    val brand: String = "", // e.g., "Bashundhara", "Jamuna", "Fresh"
    val totalQuantity: Int = 0,
    val availableQuantity: Int = 0,
    val soldQuantity: Int = 0,
    val pricePerUnit: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val lowStockThreshold: Int = 10, // Alert when stock goes below this
    val location: String = "", // Storage location
    val supplierName: String = "",
    val supplierContact: String = "",
    val notes: String = ""
)

data class StockTransaction(
    val id: String = "",
    val stockId: String = "",
    val transactionType: String = "", // "IN" (stock added), "OUT" (stock sold), "ADJUSTMENT"
    val quantity: Int = 0,
    val previousQuantity: Int = 0,
    val newQuantity: Int = 0,
    val reason: String = "", // "Purchase", "Sale", "Damage", "Return", etc.
    val orderId: String? = null, // Link to order if it's a sale
    val customerId: String? = null,
    val timestamp: Timestamp = Timestamp.now(),
    val performedBy: String = "", // User who performed the transaction
    val notes: String = ""
)

// Enum for cylinder types commonly used in Bangladesh
enum class CylinderType(val displayName: String, val displayNameBn: String, val brand: String = "") {
    OMERA_12KG("Omera 12KG", "ওমেরা ১২ কেজি", "Omera"),
    OMERA_25KG("Omera 25KG", "ওমেরা ২৫ কেজি", "Omera"),
    OMERA_35KG("Omera 35KG", "ওমেরা ৩৫ কেজি", "Omera"),
    LAUGFS_12KG("Laugfs 12KG", "লাউগফস ১২ কেজি", "Laugfs"),
    TOTAL_12KG("Total 12KG", "টোটাল ১২ কেজি", "Total"),
    TOTAL_15KG("Total 15KG", "টোটাল ১৫ কেজি", "Total"),
    
    // Legacy types for backward compatibility
    @Deprecated("Use specific brand types instead")
    KG_12("12 KG", "১২ কেজি"),
    @Deprecated("Use specific brand types instead")
    KG_25("25 KG", "২৫ কেজি"),
    @Deprecated("Use specific brand types instead")
    KG_35("35 KG", "৩৫ কেজি")
}

// Enum for popular gas brands in Bangladesh
enum class GasBrand(val displayName: String, val displayNameBn: String) {
    BASHUNDHARA("Bashundhara", "বসুন্ধরা"),
    JAMUNA("Jamuna", "যমুনা"),
    FRESH("Fresh", "ফ্রেশ"),
    OMERA("Omera", "ওমেরা"),
    TOTAL("Total", "টোটাল"),
    LAUGFS("Laugfs", "লাফ্স"),
    OTHER("Other", "অন্যান্য")
}

// Stock alert levels
enum class StockLevel {
    CRITICAL, // Below 5 units
    LOW,      // Below threshold but above 5
    NORMAL,   // Above threshold
    OVERSTOCKED // Way above normal levels
}

fun CylinderStock.getStockLevel(): StockLevel {
    return when {
        availableQuantity <= 5 -> StockLevel.CRITICAL
        availableQuantity <= lowStockThreshold -> StockLevel.LOW
        availableQuantity > (lowStockThreshold * 3) -> StockLevel.OVERSTOCKED
        else -> StockLevel.NORMAL
    }
}

fun CylinderStock.getStockLevelColor(): androidx.compose.ui.graphics.Color {
    return when (getStockLevel()) {
        StockLevel.CRITICAL -> androidx.compose.ui.graphics.Color.Red
        StockLevel.LOW -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        StockLevel.NORMAL -> androidx.compose.ui.graphics.Color(0xFF4CAF50) // Green
        StockLevel.OVERSTOCKED -> androidx.compose.ui.graphics.Color(0xFF2196F3) // Blue
    }
}
