package com.firebase.loginauth

import android.content.Context
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseUser
import com.firebase.loginauth.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)

// Utility function to extract and format nickname from email
fun extractNickname(email: String?): String {
    if (email.isNullOrBlank()) return "ব্যবহারকারী"
    
    val username = email.split("@").firstOrNull() ?: return "ব্যবহারকারী"
    
    // Remove common separators and numbers, then capitalize
    val cleanName = username
        .replace("[._-]".toRegex(), " ")
        .replace("\\d".toRegex(), "")
        .trim()
        .split(" ")
        .joinToString(" ") { word ->
            if (word.isNotEmpty()) {
                word.lowercase().replaceFirstChar { it.uppercase() }
            } else word
        }
    
    return if (cleanName.isNotBlank()) cleanName else "ব্যবহারকারী"
}

// Data classes for dashboard features
data class DashboardFeature(
    val id: Int,
    val icon: ImageVector,
    val titleEn: String,
    val titleBn: String,
    val onClick: () -> Unit = {}
)



@Composable
fun DashboardScreen(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showNotesScreen by remember { mutableStateOf(false) }
    var showCashDepositScreen by remember { mutableStateOf(false) }
    var showCustomerManagementScreen by remember { mutableStateOf(false) }
    var showOrderManagementScreen by remember { mutableStateOf(false) }
    var showCylinderStockScreen by remember { mutableStateOf(false) }
    var showSalesReportScreen by remember { mutableStateOf(false) }
    var showExpenseScreen by remember { mutableStateOf(false) }
    var showDeliveryStatusScreen by remember { mutableStateOf(false) }
    var showDueAccountScreen by remember { mutableStateOf(false) }
    var showActivityHistoryScreen by remember { mutableStateOf(false) }
    var showAutomatedReportsScreen by remember { mutableStateOf(false) }
    
    // Handle back navigation - navigate to previous screen instead of closing app
    BackHandler(
        enabled = showNotesScreen || showCashDepositScreen || showCustomerManagementScreen || 
                 showOrderManagementScreen || showCylinderStockScreen || showSalesReportScreen || 
                 showExpenseScreen || showDeliveryStatusScreen || showDueAccountScreen || showActivityHistoryScreen ||
                 showAutomatedReportsScreen || selectedTab != 0  // Handle tab navigation back gesture
    ) {
        // Close any open screen and return to dashboard
        when {
            showNotesScreen -> showNotesScreen = false
            showCashDepositScreen -> showCashDepositScreen = false
            showCustomerManagementScreen -> showCustomerManagementScreen = false
            showOrderManagementScreen -> showOrderManagementScreen = false
            showCylinderStockScreen -> showCylinderStockScreen = false
            showSalesReportScreen -> showSalesReportScreen = false
            showExpenseScreen -> showExpenseScreen = false
            showDeliveryStatusScreen -> showDeliveryStatusScreen = false
            showDueAccountScreen -> showDueAccountScreen = false
            showActivityHistoryScreen -> showActivityHistoryScreen = false
            showAutomatedReportsScreen -> showAutomatedReportsScreen = false
            selectedTab != 0 -> selectedTab = 0  // Return to home tab from notepad/settings
        }
    }
    
    // Initialize Recent Activity Repository and ActivityLogger
    val recentActivityRepository = remember { RecentActivityRepository.getInstance(context) }
    val recentActivities by recentActivityRepository.recentActivities.collectAsState()
    
    // Initialize ActivityLogger
    LaunchedEffect(Unit) {
        ActivityLogger.initialize(context)
    }
    
    // Dashboard features data
    val features = listOf(
        DashboardFeature(1, Icons.Filled.List, "Cylinder Stock", "স্টক তালিকা") {
            showCylinderStockScreen = true
        },
        DashboardFeature(2, Icons.Filled.Add, "New Order", "নতুন অর্ডার") {
            showOrderManagementScreen = true
        },
        DashboardFeature(3, Icons.Filled.Person, "Customers", "কাস্টমার তালিকা") {
            showCustomerManagementScreen = true
        },
        DashboardFeature(4, Icons.Filled.Info, "Sales Report", "বিক্রির হিসাব") {
            showSalesReportScreen = true
        },
        DashboardFeature(5, Icons.Filled.LocationOn, "Delivery Status", "ডেলিভারি আপডেট") {
            showDeliveryStatusScreen = true
        },
        DashboardFeature(6, Icons.Filled.Refresh, "Refill Request", "বাকীর হিসেব") {
            showDueAccountScreen = true
        },
        DashboardFeature(7, Icons.Filled.Info, "Automated Reports", "স্বয়ংক্রিয় রিপোর্ট") {
            showAutomatedReportsScreen = true
        },
        DashboardFeature(8, Icons.Filled.ShoppingCart, "Expenses", "খরচ") {
            showExpenseScreen = true
        },
        DashboardFeature(9, Icons.Filled.KeyboardArrowUp, "Cash Deposit", "ক্যাশ জমা") {
            showCashDepositScreen = true
        }
    )
    
    // Get recent transactions from repository - reactive to activity changes
    val recentTransactions by remember {
        derivedStateOf {
            recentActivities.take(5).map { it.toTransaction() }
        }
    }
    
    if (showNotesScreen) {
        NotesScreen(
            onBackClick = { showNotesScreen = false }
        )
    } else if (showCashDepositScreen) {
        CashDepositScreen(
            onBackClick = { showCashDepositScreen = false }
        )
    } else if (showCustomerManagementScreen) {
        CustomerManagementScreen(
            onBackClick = { showCustomerManagementScreen = false }
        )
    } else if (showOrderManagementScreen) {
        OrderManagementScreen(
            onBackClick = { showOrderManagementScreen = false }
        )
    } else if (showCylinderStockScreen) {
        CylinderStockScreen(
            onBackClick = { showCylinderStockScreen = false }
        )
    } else if (showSalesReportScreen) {
        EnhancedSalesReportScreen(
            onBackClick = { showSalesReportScreen = false }
        )
    } else if (showExpenseScreen) {
        ExpenseScreen(
            expenseViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
            onBackClick = { showExpenseScreen = false }
        )
    } else if (showDeliveryStatusScreen) {
        DeliveryStatusScreen(
            onBackClick = { showDeliveryStatusScreen = false },
            deliveryViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        )
    } else if (showDueAccountScreen) {
        DueAccountScreen(
            onBackClick = { showDueAccountScreen = false }
        )
    } else if (showActivityHistoryScreen) {
        ActivityHistoryScreen(
            activitiesFlow = recentActivityRepository.recentActivities,
            onBackClick = { showActivityHistoryScreen = false }
        )
    } else if (showAutomatedReportsScreen) {
        AutomatedReportsScreen(
            onBackClick = { showAutomatedReportsScreen = false }
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundGray)
                .statusBarsPadding() // Add system status bar padding
        ) {
            // Animated content transitions between navigation tabs
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    // Slide animation based on tab direction
                    val slideDirection = if (targetState > initialState) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }
                    
                    slideIntoContainer(
                        towards = slideDirection,
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeIn(
                        animationSpec = tween(
                            durationMillis = 300,
                            delayMillis = 50
                        )
                    ) togetherWith slideOutOfContainer(
                        towards = slideDirection,
                        animationSpec = tween(
                            durationMillis = 350,
                            easing = FastOutSlowInEasing
                        )
                    ) + fadeOut(
                        animationSpec = tween(
                            durationMillis = 250
                        )
                    )
                },
                label = "NavigationContentAnimation"
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> HomeContent(
                        user = user,
                        features = features,
                        transactions = recentTransactions,
                        onViewAllClick = { showActivityHistoryScreen = true }
                    )
                    1 -> CalculatorScreen(
                        onBackClick = { selectedTab = 0 } // Go back to home tab
                    )
                    2 -> NotesScreen(
                        onBackClick = { selectedTab = 0 } // Go back to home tab
                    )
                    3 -> SettingsContent(
                        user = user,
                        onSignOut = onSignOut
                    )
                }
            }
            
            // Modern Blue Gradient Navigation Bar - Hidden on Calculator Screen
            if (selectedTab != 1) {
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(28.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF1E3A8A), // Deep blue
                                        Color(0xFF3B82F6), // Medium blue
                                        Color(0xFF60A5FA)  // Light blue
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(1000f, 300f)
                                ),
                                shape = RoundedCornerShape(28.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Home Tab
                            ModernNavItem(
                                icon = Icons.Filled.Home,
                                label = "হোম",
                                isSelected = selectedTab == 0,
                                onClick = { selectedTab = 0 }
                            )
                            
                            // Calculator Tab
                            ModernNavItem(
                                icon = Icons.Filled.Star,
                                label = "ক্যালকুলেটর",
                                isSelected = selectedTab == 1,
                                onClick = { selectedTab = 1 }
                            )
                            
                            // Notes Tab
                            ModernNavItem(
                                icon = Icons.Filled.Edit,
                                label = "নোট",
                                isSelected = selectedTab == 2,
                                onClick = { selectedTab = 2 }
                            )
                            
                            // Settings Tab
                            ModernNavItem(
                                icon = Icons.Filled.Settings,
                                label = "সেটিংস",
                                isSelected = selectedTab == 3,
                                onClick = { selectedTab = 3 }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    user: FirebaseUser,
    features: List<DashboardFeature>,
    transactions: List<Transaction>,
    onViewAllClick: () -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding() // Handle system bars including notch
            .padding(bottom = 80.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 16.dp, // Reduced since statusBarsPadding handles safe area
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Title with gradient and stylish font
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Dashboard",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = androidx.compose.ui.text.TextStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF1E3A8A), // Deep blue
                                Color(0xFF3B82F6), // Medium blue
                                Color(0xFF60A5FA), // Light blue
                                Color(0xFF93C5FD)  // Very light blue
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(300f, 100f)
                        )
                    ),
                    letterSpacing = 1.2.sp,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                )
            }
        }
        
        // Top Header Section
        item {
            HeaderSection(user = user)
        }
        
        // Feature Grid
        item {
            FeatureGrid(features = features)
        }
        
        // Recent Transactions
        item {
            RecentTransactionsSection(
                transactions = transactions,
                onViewAllClick = onViewAllClick
            )
        }
    }
}

@Composable
fun HeaderSection(user: FirebaseUser) {
    val context = LocalContext.current
    val cashDepositRepository = remember { CashDepositRepository(context) }
    val cylinderStockRepository = remember { CylinderStockRepository(context) }
    val expenseRepository = remember { ExpenseRepository(context) }
    
    // State for real calculated data
    var totalCashAmount by remember { mutableStateOf(0.0) }
    var totalCylinderStock by remember { mutableStateOf(0) }
    var totalExpenseAmount by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Load and calculate real data
    LaunchedEffect(Unit) {
        try {
            // Calculate total cash deposit amount (all time)
            cashDepositRepository.cashDeposits.collect { deposits ->
                totalCashAmount = deposits.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                Log.d("DashboardScreen", "Total cash amount calculated: $totalCashAmount from ${deposits.size} deposits")
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error loading cash deposits", e)
            totalCashAmount = 0.0
        }
    }
    
    // Separate LaunchedEffect for expense data
    LaunchedEffect(Unit) {
        try {
            // Calculate total expense amount (all time)
            expenseRepository.expenses.collect { expenses ->
                totalExpenseAmount = expenses.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                Log.d("DashboardScreen", "Total expense amount calculated: $totalExpenseAmount from ${expenses.size} expenses")
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error loading expenses", e)
            totalExpenseAmount = 0.0
        }
    }
    
    // Separate LaunchedEffect for stock data to ensure proper loading
    LaunchedEffect(Unit) {
        try {
            Log.d("DashboardScreen", "Starting to load stock data...")
            
            // Force sync with cloud first
            cylinderStockRepository.syncWithCloud()
            
            // Load stock items
            val result = cylinderStockRepository.loadStockItems()
            Log.d("DashboardScreen", "Load stock result: $result")
            
            // Collect stock items with proper error handling
            cylinderStockRepository.stockItems.collect { stockItems ->
                Log.d("DashboardScreen", "Received ${stockItems.size} stock items")
                
                val totalStock = stockItems.sumOf { item ->
                    Log.d("DashboardScreen", "Stock item: ${item.cylinderType} - Available: ${item.availableQuantity}")
                    item.availableQuantity
                }
                
                totalCylinderStock = totalStock
                Log.d("DashboardScreen", "Total cylinder stock calculated: $totalStock")
                isLoading = false
            }
        } catch (e: Exception) {
            Log.e("DashboardScreen", "Error loading stock data", e)
            // If stock loading fails, show zero but don't hide the error
            totalCylinderStock = 0
            isLoading = false
        }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        ),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            DarkTeal,
                            MediumTeal,
                            LightTeal
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(1000f, 1000f)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Hi, ${extractNickname(user.email)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Business Overview Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Total Cash Deposit
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isLoading) {
                            Text(
                                text = "লোড হচ্ছে...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "৳${formatCurrencyBengali(totalCashAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "মোট ক্যাশ জমা",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Total Cylinder Stock
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isLoading) {
                            Text(
                                text = "--",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "${totalCylinderStock}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "মোট সিলিন্ডার",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // Total Expenses
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        if (isLoading) {
                            Text(
                                text = "লোড হচ্ছে...",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        } else {
                            Text(
                                text = "৳${formatCurrencyBengali(totalExpenseAmount)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFAB91) // Light orange for expenses
                            )
                        }
                        
                        Text(
                            text = "মোট খরচ",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.End
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Current Date
                Text(
                    text = "আজ: ${getCurrentDateForDashboard()}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun FeatureGrid(features: List<DashboardFeature>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(bottom = 20.dp)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.height(320.dp)
            ) {
                items(features) { feature ->
                    FeatureItem(feature = feature)
                }
            }
        }
    }
}

@Composable
fun FeatureItem(feature: DashboardFeature) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { feature.onClick() }
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(64.dp),
            colors = CardDefaults.cardColors(
                containerColor = when (feature.id) {
                    1 -> Color(0xFFF3F4F6)
                    2 -> Color(0xFFFEF3C7)
                    3 -> Color(0xFFECFDF5)
                    4 -> Color(0xFFF0F9FF)
                    5 -> Color(0xFFFDF2F8)
                    6 -> Color(0xFFF5F3FF)
                    else -> BackgroundGray
                }
            ),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = feature.icon,
                    contentDescription = feature.titleEn,
                    tint = when (feature.id) {
                        1 -> Color(0xFF6B7280)
                        2 -> AccentOrange
                        3 -> SuccessGreen
                        4 -> PrimaryBlue
                        5 -> Color(0xFFEC4899)
                        6 -> AccentPurple
                        else -> LightBlue
                    },
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = feature.titleBn,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextDark,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun RecentTransactionsSection(
    transactions: List<Transaction>,
    onViewAllClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )
                
                Text(
                    text = "View All",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = PrimaryBlue,
                    modifier = Modifier.clickable { onViewAllClick() }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            transactions.take(3).forEach { transaction ->
                TransactionItem(transaction = transaction)
                if (transaction != transactions.take(3).last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Card(
                modifier = Modifier.size(52.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (transaction.type == "credit") 
                        Color(0xFFECFDF5) 
                    else 
                        Color(0xFFFEF2F2)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (transaction.type == "credit") 
                            Icons.Filled.Add 
                        else 
                            Icons.Filled.Close,
                        contentDescription = transaction.type,
                        tint = if (transaction.type == "credit") SuccessGreen else ErrorRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = transaction.description,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
                
                Text(
                    text = transaction.date,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            }
        }
        
        Text(
            text = if (transaction.type == "credit") "+৳${transaction.amount}" else "-৳${transaction.amount}",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (transaction.type == "credit") SuccessGreen else ErrorRed
        )
    }
}

@Composable
fun AddNoteContent() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MediumTeal),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Add Note",
                    tint = LightGray,
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "নোট যোগ করুন",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = LightGray
                )
                
                Text(
                    text = "Coming Soon",
                    fontSize = 14.sp,
                    color = LightGray.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SettingsContent(
    user: FirebaseUser,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    
    // Dialog states
    var showBackupDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .padding(bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Data Management Section
        item {
            SettingsSection(
                title = "ডেটা ম্যানেজমেন্ট",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Filled.Add,
                        title = "ব্যাকআপ ও সিঙ্ক",
                        subtitle = "ডেটা ব্যাকআপ ও ক্লাউড সিঙ্ক সেটিংস",
                        onClick = { showBackupDialog = true }
                    )
                )
            )
        }
        
        // Account Section
        item {
            SettingsSection(
                title = "অ্যাকাউন্ট",
                items = listOf(
                    SettingsItem(
                        icon = Icons.Filled.ExitToApp,
                        title = "সাইন আউট",
                        subtitle = "অ্যাকাউন্ট থেকে সাইন আউট করুন",
                        onClick = onSignOut
                    )
                )
            )
        }
    }
    
    // Dialog Components - Only functional backup dialog
    if (showBackupDialog) {
        val backupViewModel = remember { BackupViewModel(context) }
        BackupDialog(
            onDismiss = { showBackupDialog = false },
            backupViewModel = backupViewModel,
            context = context
        )
    }
}





@Composable
private fun UserInfoRow(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF4A5568),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            color = Color(0xFF2D3748),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Divider(
            color = Color(0xFFE2E8F0),
            thickness = 1.dp
        )
    }
}

private fun formatDate(timestamp: Long?): String {
    return if (timestamp != null) {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        sdf.format(Date(timestamp))
    } else {
        "Unknown"
    }
}

// Utility function to format currency for Bengali locale
fun formatCurrencyBengali(amount: Double): String {
    return NumberFormat.getNumberInstance(Locale("bn", "BD")).format(amount.toInt())
}

// Utility function to get current date in Bengali for dashboard
fun getCurrentDateForDashboard(): String {
    val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("bn", "BD"))
    return sdf.format(Date())
}

// Simple placeholder for notes functionality
@Composable
fun NotesPlaceholderContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Create,
            contentDescription = "Notes",
            modifier = Modifier.size(64.dp),
            tint = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Notes Feature",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Coming Soon...",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text(
                text = "Create Note",
                color = Color.White
            )
        }
    }
}

// Activity History Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryScreen(
    activitiesFlow: kotlinx.coroutines.flow.StateFlow<List<RecentActivityEntry>>,
    onBackClick: () -> Unit
) {
    val activities by activitiesFlow.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = "সকল কার্যক্রম",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.width(48.dp)) // Balance the back button
            }
            
            // Activity List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                if (activities.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = "No Activities",
                                    modifier = Modifier.size(48.dp),
                                    tint = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "কোনো কার্যক্রম নেই",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "আপনার ব্যবসায়িক কার্যক্রম এখানে দেখানো হবে",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(activities.size) { index ->
                        ActivityHistoryCard(activity = activities[index])
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityHistoryCard(activity: RecentActivityEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activity.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = activity.description,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                // Activity Type Icon
                Card(
                    modifier = Modifier.size(40.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = when (activity.type) {
                            ActivityType.CASH_DEPOSIT -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                            ActivityType.EXPENSE -> Color(0xFFFF5722).copy(alpha = 0.1f)
                            ActivityType.ORDER_CREATE, ActivityType.ORDER_UPDATE -> Color(0xFF2196F3).copy(alpha = 0.1f)
                            ActivityType.CUSTOMER_CREATE, ActivityType.CUSTOMER_UPDATE -> Color(0xFF9C27B0).copy(alpha = 0.1f)
                            ActivityType.STOCK_ADD, ActivityType.STOCK_UPDATE -> Color(0xFFFF9800).copy(alpha = 0.1f)
                            ActivityType.DUE_ACCOUNT -> Color(0xFF607D8B).copy(alpha = 0.1f)
                            ActivityType.NOTE_CREATE, ActivityType.NOTE_UPDATE -> Color(0xFF795548).copy(alpha = 0.1f)
                        }
                    ),
                    shape = CircleShape
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when (activity.type) {
                                ActivityType.CASH_DEPOSIT -> "💰"
                                ActivityType.EXPENSE -> "💸"
                                ActivityType.ORDER_CREATE, ActivityType.ORDER_UPDATE -> "📋"
                                ActivityType.CUSTOMER_CREATE, ActivityType.CUSTOMER_UPDATE -> "👤"
                                ActivityType.STOCK_ADD, ActivityType.STOCK_UPDATE -> "📦"
                                ActivityType.DUE_ACCOUNT -> "💳"
                                ActivityType.NOTE_CREATE, ActivityType.NOTE_UPDATE -> "📝"
                            },
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = activity.getBengaliTime(),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                
                activity.amount?.let { amount ->
                    Text(
                        text = if (activity.isCredit) "+৳${String.format("%.0f", amount)}" else "-৳${String.format("%.0f", amount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activity.isCredit) Color(0xFF4CAF50) else Color(0xFFFF5722)
                    )
                }
            }
            
            activity.customerName?.let { customerName ->
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Customer",
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = customerName,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}
