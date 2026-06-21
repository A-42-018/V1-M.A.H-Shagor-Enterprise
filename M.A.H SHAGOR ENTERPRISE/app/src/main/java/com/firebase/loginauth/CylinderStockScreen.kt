package com.firebase.loginauth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.firebase.loginauth.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CylinderStockScreen(
    onBackClick: () -> Unit,
    viewModel: CylinderStockViewModel = viewModel()
) {
    val allStockItems by viewModel.stockItems.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    
    val stockItems = remember(allStockItems, searchQuery) { viewModel.getFilteredStockItems() }
    val stockStats = remember(allStockItems) { viewModel.getStockStatistics() }
    val lowStockItems = remember(allStockItems) { viewModel.getLowStockItems() }
    
    val showAddStockDialog by viewModel.showAddStockDialog.collectAsState()
    val showEditStockDialog by viewModel.showEditStockDialog.collectAsState()
    val showStockDetailsDialog by viewModel.showStockDetailsDialog.collectAsState()
    val showAdjustStockDialog by viewModel.showAdjustStockDialog.collectAsState()
    val selectedStock by viewModel.selectedStock.collectAsState()
    
    // Delete confirmation dialog state
    var showDeleteDialog by remember { mutableStateOf(false) }
    var stockToDelete by remember { mutableStateOf<CylinderStock?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DarkTeal, MediumTeal)
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "স্টক ম্যানেজমেন্ট",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.showAddStockDialog() }) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Stock",
                        tint = Color.White
                    )
                }
                IconButton(onClick = { viewModel.loadStockItems() }) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Refresh",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Content
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::setSearchQuery,
                    placeholder = "স্টক খুঁজুন..."
                )
            }

            // Statistics Cards
            item {
                StockStatisticsSection(stockStats, lowStockItems.size)
            }

            // Low Stock Alert (if any)
            if (lowStockItems.isNotEmpty()) {
                item {
                    LowStockAlert(lowStockItems)
                }
            }

            // Stock Items
            item {
                Text(
                    text = "স্টক তালিকা (${stockItems.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            } else if (stockItems.isEmpty()) {
                item {
                    EmptyStockMessage()
                }
            } else {
                items(stockItems) { stock ->
                    StockItemCard(
                        stock = stock,
                        onItemClick = { viewModel.showStockDetailsDialog(stock) },
                        onEditClick = { viewModel.showEditStockDialog(stock) },
                        onAdjustClick = { viewModel.showAdjustStockDialog(stock) },
                        onDeleteClick = {
                            stockToDelete = stock
                            showDeleteDialog = true
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && stockToDelete != null) {
        AlertDialog(
            onDismissRequest = { 
                showDeleteDialog = false
                stockToDelete = null
            },
            title = { Text("নিশ্চিত করুন") },
            text = { Text("আপনি কি এই স্টক আইটেমটি মুছে ফেলতে চান?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        stockToDelete?.let { stock ->
                            viewModel.deleteStockItem(stock.id)
                        }
                        showDeleteDialog = false
                        stockToDelete = null
                    }
                ) {
                    Text("হ্যাঁ", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        stockToDelete = null
                    }
                ) {
                    Text("না")
                }
            }
        )
    }

    // Error Snackbar
    error?.let { errorMessage ->
        LaunchedEffect(errorMessage) {
            // Show error message
            viewModel.clearError()
        }
    }

    // Dialogs
    if (showAddStockDialog) {
        AddStockDialog(
            onDismiss = { viewModel.hideAddStockDialog() },
            onConfirm = { stock -> viewModel.addStockItem(stock) }
        )
    }

    if (showEditStockDialog && selectedStock != null) {
        EditStockDialog(
            stock = selectedStock!!,
            onDismiss = { viewModel.hideEditStockDialog() },
            onConfirm = { stock -> viewModel.updateStockItem(stock) }
        )
    }

    if (showStockDetailsDialog && selectedStock != null) {
        StockDetailsDialog(
            stock = selectedStock!!,
            onDismiss = { viewModel.hideStockDetailsDialog() }
        )
    }

    if (showAdjustStockDialog && selectedStock != null) {
        AdjustStockDialog(
            stock = selectedStock!!,
            onDismiss = { viewModel.hideAdjustStockDialog() },
            onConfirm = { quantityChange, reason, notes ->
                viewModel.adjustStock(
                    selectedStock!!.id,
                    quantityChange,
                    reason,
                    notes = notes
                )
            }
        )
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = LightTeal,
            unfocusedBorderColor = Color.Gray
        )
    )
}

@Composable
private fun StockStatisticsSection(
    stats: StockStatistics,
    lowStockCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            title = "মোট আইটেম",
            value = stats.totalItems.toString(),
            icon = Icons.Filled.List,
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "মোট পরিমাণ",
            value = stats.totalQuantity.toString(),
            icon = Icons.Filled.Info,
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "কম স্টক",
            value = lowStockCount.toString(),
            icon = Icons.Filled.Warning,
            color = if (lowStockCount > 0) Color(0xFFFF9800) else Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun LowStockAlert(lowStockItems: List<CylinderStock>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = "Warning",
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "কম স্টক সতর্কতা",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD32F2F)
                )
                Text(
                    text = "${lowStockItems.size}টি আইটেমের স্টক কম",
                    fontSize = 14.sp,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

@Composable
private fun StockItemCard(
    stock: CylinderStock,
    onItemClick: () -> Unit,
    onEditClick: () -> Unit,
    onAdjustClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onItemClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stock.brand} - ${stock.cylinderType}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkTeal
                    )
                    Text(
                        text = "স্থান: ${stock.location}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
                
                // Stock level indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stock.getStockLevelColor().copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${stock.availableQuantity}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = stock.getStockLevelColor()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "মূল্য: ৳${stock.pricePerUnit}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Text(
                    text = "বিক্রিত: ${stock.soldQuantity}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onAdjustClick) {
                    Text("স্টক সমন্বয়", color = LightTeal)
                }
                TextButton(onClick = onEditClick) {
                    Text("সম্পাদনা", color = LightTeal)
                }
                TextButton(onClick = onDeleteClick) {
                    Text("মুছুন", color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun EmptyStockMessage() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = "Empty Stock",
                tint = LightTeal,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "কোন স্টক নেই",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DarkTeal
            )
            Text(
                text = "নতুন স্টক যোগ করতে + বাটনে ক্লিক করুন",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}
