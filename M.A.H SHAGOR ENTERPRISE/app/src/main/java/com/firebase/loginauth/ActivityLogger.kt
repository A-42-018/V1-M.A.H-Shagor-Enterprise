package com.firebase.loginauth

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.util.*

/**
 * Utility class to log activities from different modules
 * This provides a simple interface for all modules to log their activities
 */
object ActivityLogger {
    private var repository: RecentActivityRepository? = null
    private val loggerScope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO
    )

    fun initialize(context: Context) {
        repository = RecentActivityRepository.getInstance(context)
    }

    fun shutdown() {
        loggerScope.cancel()
    }
    
    // Cash Deposit Activities
    fun logCashDeposit(amount: Double, cylinderCount: Int, description: String = "ক্যাশ জমা") {
        val activity = RecentActivityEntry(
            id = "cash_${System.currentTimeMillis()}",
            type = ActivityType.CASH_DEPOSIT,
            title = "ক্যাশ জমা",
            description = "$description - $cylinderCount সিলিন্ডার",
            amount = amount,
            isCredit = true
        )
        logActivity(activity)
    }
    
    // Expense Activities
    fun logExpense(amount: Double, expenseType: String, description: String, vendor: String? = null) {
        val activity = RecentActivityEntry(
            id = "expense_${System.currentTimeMillis()}",
            type = ActivityType.EXPENSE,
            title = "খরচ - $expenseType",
            description = description,
            amount = amount,
            customerName = vendor,
            isCredit = false
        )
        logActivity(activity)
    }
    
    // Order Activities
    fun logOrderCreate(customerName: String, orderValue: Double, cylinderType: String) {
        val activity = RecentActivityEntry(
            id = "order_create_${System.currentTimeMillis()}",
            type = ActivityType.ORDER_CREATE,
            title = "নতুন অর্ডার",
            description = "$cylinderType অর্ডার তৈরি",
            amount = orderValue,
            customerName = customerName,
            isCredit = true
        )
        logActivity(activity)
    }
    
    fun logOrderUpdate(customerName: String, status: String, orderValue: Double? = null) {
        val activity = RecentActivityEntry(
            id = "order_update_${System.currentTimeMillis()}",
            type = ActivityType.ORDER_UPDATE,
            title = "অর্ডার আপডেট",
            description = "স্ট্যাটাস: $status",
            amount = orderValue,
            customerName = customerName,
            isCredit = false
        )
        logActivity(activity)
    }
    
    // Customer Activities
    fun logCustomerCreate(customerName: String, phone: String) {
        val activity = RecentActivityEntry(
            id = "customer_create_${System.currentTimeMillis()}",
            type = ActivityType.CUSTOMER_CREATE,
            title = "নতুন কাস্টমার",
            description = "কাস্টমার যোগ করা হয়েছে - $phone",
            customerName = customerName,
            isCredit = false
        )
        logActivity(activity)
    }
    
    fun logCustomerUpdate(customerName: String, updateType: String = "তথ্য আপডেট") {
        val activity = RecentActivityEntry(
            id = "customer_update_${System.currentTimeMillis()}",
            type = ActivityType.CUSTOMER_UPDATE,
            title = "কাস্টমার আপডেট",
            description = updateType,
            customerName = customerName,
            isCredit = false
        )
        logActivity(activity)
    }
    
    // Stock Activities
    fun logStockAdd(cylinderType: String, brand: String, quantity: Int, pricePerUnit: Double) {
        val activity = RecentActivityEntry(
            id = "stock_add_${System.currentTimeMillis()}",
            type = ActivityType.STOCK_ADD,
            title = "স্টক যোগ",
            description = "$brand $cylinderType - $quantity টি",
            amount = pricePerUnit * quantity,
            isCredit = false
        )
        logActivity(activity)
    }
    
    fun logStockUpdate(cylinderType: String, brand: String, newQuantity: Int) {
        val activity = RecentActivityEntry(
            id = "stock_update_${System.currentTimeMillis()}",
            type = ActivityType.STOCK_UPDATE,
            title = "স্টক আপডেট",
            description = "$brand $cylinderType - $newQuantity টি",
            isCredit = false
        )
        logActivity(activity)
    }
    
    // Due Account Activities
    fun logDueAccount(customerName: String, amount: Double, transactionType: String) {
        val activity = RecentActivityEntry(
            id = "due_${System.currentTimeMillis()}",
            type = ActivityType.DUE_ACCOUNT,
            title = "বাকীর হিসাব",
            description = transactionType,
            amount = amount,
            customerName = customerName,
            isCredit = transactionType.contains("পেমেন্ট") || transactionType.contains("Payment")
        )
        logActivity(activity)
    }
    
    // Note Activities
    fun logNoteCreate(noteTitle: String, noteType: String = "টেক্সট নোট") {
        val activity = RecentActivityEntry(
            id = "note_create_${System.currentTimeMillis()}",
            type = ActivityType.NOTE_CREATE,
            title = "নতুন নোট",
            description = "$noteType তৈরি - $noteTitle",
            isCredit = false
        )
        logActivity(activity)
    }
    
    fun logNoteUpdate(noteTitle: String, noteType: String = "টেক্সট নোট") {
        val activity = RecentActivityEntry(
            id = "note_update_${System.currentTimeMillis()}",
            type = ActivityType.NOTE_UPDATE,
            title = "নোট আপডেট",
            description = "$noteType আপডেট - $noteTitle",
            isCredit = false
        )
        logActivity(activity)
    }
    
    // Generic activity logger
    private fun logActivity(activity: RecentActivityEntry) {
        repository?.let { repo ->
            loggerScope.launch {
                repo.addActivity(activity)
            }
        }
    }
    
    // Get repository instance (for direct access if needed)
    fun getRepository(): RecentActivityRepository? = repository
}
