package com.firebase.loginauth

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

// Data class for calculation history
data class CalculationHistory(
    val expression: String = "",
    val result: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun CalculatorScreen(
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    
    // Calculator state - Two-row display system
    var inputExpression by remember { mutableStateOf("") }  // Top row: user input
    var liveResult by remember { mutableStateOf("0") }      // Bottom row: real-time result
    var previousNumber by remember { mutableStateOf("") }
    var operation by remember { mutableStateOf("") }
    var currentNumber by remember { mutableStateOf("") }
    var waitingForOperand by remember { mutableStateOf(false) }
    var shouldResetInput by remember { mutableStateOf(false) }
    
    // UI state
    var showHistory by remember { mutableStateOf(false) }
    var buttonPressed by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(listOf<CalculationHistory>()) }
    
    // Load history on start
    LaunchedEffect(Unit) {
        history = loadCalculationHistory(context)
    }
    
    // Handle system back gesture
    BackHandler {
        if (showHistory) {
            showHistory = false
        } else {
            onBackClick()
        }
    }
    
    // Handle button press animation timing
    LaunchedEffect(buttonPressed) {
        if (buttonPressed.isNotEmpty()) {
            delay(150)
            buttonPressed = ""
        }
    }
    
    // Calculator functions with two-row display system
    fun updateDisplay() {
        // Update input expression (top row) - what user is typing
        inputExpression = when {
            previousNumber.isNotEmpty() && operation.isNotEmpty() && currentNumber.isNotEmpty() -> {
                "$previousNumber $operation $currentNumber"
            }
            previousNumber.isNotEmpty() && operation.isNotEmpty() -> {
                "$previousNumber $operation"
            }
            else -> ""
        }
        
        // Update live result (bottom row) - current display value
        liveResult = when {
            // If we have a complete expression, show the calculated result
            previousNumber.isNotEmpty() && operation.isNotEmpty() && currentNumber.isNotEmpty() -> {
                performCalculation(previousNumber, currentNumber, operation)
            }
            // If we're typing a number, show that number
            currentNumber.isNotEmpty() -> currentNumber
            // If we have previous number but no current operation, show previous number
            previousNumber.isNotEmpty() && operation.isEmpty() -> previousNumber
            // Default
            else -> "0"
        }
    }
    
    fun inputNumber(number: String) {
        if (shouldResetInput) {
            currentNumber = number
            shouldResetInput = false
        } else if (waitingForOperand) {
            currentNumber = number
            waitingForOperand = false
        } else {
            currentNumber = if (currentNumber == "0" || currentNumber.isEmpty()) number else currentNumber + number
        }
        updateDisplay()
    }
    
    fun inputOperation(nextOperation: String) {
        if (currentNumber.isNotEmpty()) {
            if (previousNumber.isEmpty()) {
                previousNumber = currentNumber
            } else if (operation.isNotEmpty()) {
                // Perform intermediate calculation
                val result = performCalculation(previousNumber, currentNumber, operation)
                previousNumber = result
                liveResult = result
            }
        }
        
        operation = nextOperation
        currentNumber = ""
        waitingForOperand = true
        updateDisplay()
    }
    
    fun calculate() {
        if (previousNumber.isNotEmpty() && operation.isNotEmpty() && currentNumber.isNotEmpty()) {
            val result = performCalculation(previousNumber, currentNumber, operation)
            val fullExpression = "$previousNumber $operation $currentNumber"
            
            // Add to history
            val newEntry = CalculationHistory(
                expression = fullExpression,
                result = result,
                timestamp = System.currentTimeMillis()
            )
            history = (listOf(newEntry) + history).take(20) // Keep last 20
            
            // Save to storage
            saveHistoryToLocal(context, history)
            saveHistoryToCloud(history)
            
            // Reset for next calculation - result becomes the new starting number
            previousNumber = result
            currentNumber = ""
            operation = ""
            inputExpression = ""
            liveResult = result
            waitingForOperand = false
            shouldResetInput = true
        }
    }
    
    fun clear() {
        inputExpression = ""
        liveResult = "0"
        currentNumber = ""
        previousNumber = ""
        operation = ""
        waitingForOperand = false
        shouldResetInput = false
    }
    
    fun clearHistory() {
        history = emptyList()
        saveHistoryToLocal(context, history)
        clearHistoryFromCloud()
    }
    
    fun inputDecimal() {
        if (!currentNumber.contains(".")) {
            if (shouldResetInput) {
                currentNumber = "0."
                shouldResetInput = false
            } else if (waitingForOperand) {
                currentNumber = "0."
                waitingForOperand = false
            } else {
                currentNumber = if (currentNumber.isEmpty()) "0." else currentNumber + "."
            }
            updateDisplay()
        }
    }
    
    fun toggleSign() {
        if (currentNumber.isNotEmpty() && currentNumber != "0") {
            currentNumber = if (currentNumber.startsWith("-")) {
                currentNumber.substring(1)
            } else {
                "-$currentNumber"
            }
            updateDisplay()
        }
    }
    
    fun percentage() {
        val value = currentNumber.toDoubleOrNull() ?: 0.0
        val result = value / 100
        currentNumber = if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.10f", result).trimEnd('0').trimEnd('.')
        }
        updateDisplay()
    }

    // Full-screen black background with transparent status bar
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Main calculator content with blur when history is shown
        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showHistory) Modifier.blur(8.dp) else Modifier
                )
        ) {
            // Top bar with back button and history button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp), // Further reduced padding to move display up
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "পেছনে যান",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                IconButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "History",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Modern Display Area with real-time calculation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .padding(top = 8.dp, bottom = 16.dp), // Further reduced top padding to move display higher
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Display Area with improved spacing
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 24.dp, vertical = 8.dp), // Further reduced vertical padding to move display up
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.End
                ) {
                    // Top row: User input expression (smaller, white) with better spacing
                    if (inputExpression.isNotEmpty()) {
                        Text(
                            text = inputExpression,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Light,
                            color = Color.White.copy(alpha = 0.8f),
                            textAlign = TextAlign.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp) // Optimized spacing between rows
                        )
                    }
                    
                    // Bottom row: Real-time calculation result (larger) with better spacing
                    Text(
                        text = liveResult,
                        fontSize = if (inputExpression.isNotEmpty()) 44.sp else 64.sp,
                        fontWeight = FontWeight.Light,
                        color = if (inputExpression.isNotEmpty()) Color.Gray else Color.White,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp) // Optimized bottom padding to separate from buttons
                    )
                }

                // Calculator button grid with proper callbacks
                CalculatorButtonGrid(
                    onNumberClick = { number ->
                        buttonPressed = number
                        inputNumber(number)
                    },
                    onOperationClick = { op ->
                        buttonPressed = op
                        inputOperation(op)
                    },
                    onEqualsClick = {
                        buttonPressed = "="
                        calculate()
                    },
                    onClearClick = {
                        buttonPressed = "C"
                        clear()
                    },
                    onDecimalClick = {
                        buttonPressed = "."
                        inputDecimal()
                    },
                    onSignToggleClick = {
                        buttonPressed = "+/-"
                        toggleSign()
                    },
                    onPercentageClick = {
                        buttonPressed = "%"
                        percentage()
                    },
                    buttonPressed = buttonPressed
                )
            }
        }
        
        // Clickable overlay to close history when clicking outside
        if (showHistory) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) {
                        showHistory = false
                    }
            )
        }
        
        // Modern History Panel with blur background
        AnimatedVisibility(
            visible = showHistory,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .padding(16.dp),
                color = Color(0xCC1C1C1E), // Semi-transparent background
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Calculation History",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { clearHistory() }
                        ) {
                            Text(
                                text = "Clear All",
                                color = Color(0xFFFF9500),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    LazyColumn {
                        items(history.take(20)) { calculation ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        currentNumber = calculation.result
                                        inputExpression = calculation.result
                                        liveResult = calculation.result
                                        showHistory = false
                                        shouldResetInput = true
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF2C2C2E)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = calculation.expression,
                                        color = Color.Gray,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "= ${calculation.result}",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.End,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CalculatorButtonGrid(
    onNumberClick: (String) -> Unit,
    onOperationClick: (String) -> Unit,
    onEqualsClick: () -> Unit,
    onClearClick: () -> Unit,
    onDecimalClick: () -> Unit,
    onSignToggleClick: () -> Unit,
    onPercentageClick: () -> Unit,
    buttonPressed: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp), // Increased padding around entire grid
        verticalArrangement = Arrangement.spacedBy(18.dp) // Increased vertical spacing between button rows
    ) {
        // Row 1: C, +/-, %, ÷
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp) // Increased horizontal spacing between buttons
        ) {
            ModernAnimatedButton(
                text = "C",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF2C2C2E),
                textColor = Color.White,
                isPressed = buttonPressed == "C"
            ) { 
                onClearClick()
            }
            ModernAnimatedButton(
                text = "+/-",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF2C2C2E),
                textColor = Color.White,
                isPressed = buttonPressed == "+/-"
            ) { 
                onSignToggleClick()
            }
            ModernAnimatedButton(
                text = "%",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFF2C2C2E),
                textColor = Color.White,
                isPressed = buttonPressed == "%"
            ) { 
                onPercentageClick()
            }
            ModernAnimatedButton(
                text = "÷",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFF9500),
                textColor = Color.White,
                isPressed = buttonPressed == "÷"
            ) { 
                onOperationClick("÷")
            }
        }

        // Row 2: 7, 8, 9, ×
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp) // Increased horizontal spacing between buttons
        ) {
            ModernAnimatedButton("7", Modifier.weight(1f), isPressed = buttonPressed == "7") { 
                onNumberClick("7")
            }
            ModernAnimatedButton("8", Modifier.weight(1f), isPressed = buttonPressed == "8") { 
                onNumberClick("8")
            }
            ModernAnimatedButton("9", Modifier.weight(1f), isPressed = buttonPressed == "9") { 
                onNumberClick("9")
            }
            ModernAnimatedButton(
                text = "×",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFF9500),
                textColor = Color.White,
                isPressed = buttonPressed == "×"
            ) { 
                onOperationClick("×")
            }
        }

        // Row 3: 4, 5, 6, -
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp) // Increased horizontal spacing between buttons
        ) {
            ModernAnimatedButton("4", Modifier.weight(1f), isPressed = buttonPressed == "4") { 
                onNumberClick("4")
            }
            ModernAnimatedButton("5", Modifier.weight(1f), isPressed = buttonPressed == "5") { 
                onNumberClick("5")
            }
            ModernAnimatedButton("6", Modifier.weight(1f), isPressed = buttonPressed == "6") { 
                onNumberClick("6")
            }
            ModernAnimatedButton(
                text = "-",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFF9500),
                textColor = Color.White,
                isPressed = buttonPressed == "-"
            ) { 
                onOperationClick("-")
            }
        }

        // Row 4: 1, 2, 3, +
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp) // Increased horizontal spacing between buttons
        ) {
            ModernAnimatedButton("1", Modifier.weight(1f), isPressed = buttonPressed == "1") { 
                onNumberClick("1")
            }
            ModernAnimatedButton("2", Modifier.weight(1f), isPressed = buttonPressed == "2") { 
                onNumberClick("2")
            }
            ModernAnimatedButton("3", Modifier.weight(1f), isPressed = buttonPressed == "3") { 
                onNumberClick("3")
            }
            ModernAnimatedButton(
                text = "+",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFF9500),
                textColor = Color.White,
                isPressed = buttonPressed == "+"
            ) { 
                onOperationClick("+")
            }
        }

        // Row 5: 0, ., =
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp) // Increased horizontal spacing between buttons
        ) {
            ModernAnimatedButton(
                text = "0",
                modifier = Modifier.weight(2f),
                isWide = true,
                isPressed = buttonPressed == "0"
            ) { 
                onNumberClick("0")
            }
            ModernAnimatedButton(
                ".", 
                Modifier.weight(1f),
                isPressed = buttonPressed == "."
            ) { 
                onDecimalClick()
            }
            ModernAnimatedButton(
                text = "=",
                modifier = Modifier.weight(1f),
                backgroundColor = Color(0xFFFF9500),
                textColor = Color.White,
                isPressed = buttonPressed == "="
            ) { 
                onEqualsClick()
            }
        }
    }
}



@Composable
fun ModernAnimatedButton(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF333333),
    textColor: Color = Color.White,
    isWide: Boolean = false,
    isPressed: Boolean = false,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(120, easing = EaseOutQuart)
    )
    
    val elevation by animateFloatAsState(
        targetValue = if (isPressed) 1.dp.value else 6.dp.value,
        animationSpec = tween(120)
    )
    
    Surface(
        onClick = onClick,
        modifier = modifier
            .height(80.dp) // Increased button height for better touch targets
            .scale(scale)
            .then(
                if (isWide) Modifier.fillMaxWidth() else Modifier.aspectRatio(1f)
            ),
        color = backgroundColor,
        shape = if (isWide) RoundedCornerShape(40.dp) else RoundedCornerShape(20.dp), // Changed from CircleShape for consistency
        shadowElevation = elevation.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp) // Added internal padding for better visual balance
        ) {
            Text(
                text = text,
                fontSize = 26.sp, // Slightly reduced for better fit
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
    }
}

// Helper functions for calculation logic
fun performCalculation(num1: String, num2: String, operation: String): String {
    return try {
        val n1 = num1.toDouble()
        val n2 = num2.toDouble()
        val result = when (operation) {
            "+" -> n1 + n2
            "-" -> n1 - n2
            "×" -> n1 * n2
            "÷" -> if (n2 != 0.0) n1 / n2 else Double.POSITIVE_INFINITY
            else -> n2
        }
        
        // Format result to remove unnecessary decimal places
        if (result == result.toLong().toDouble()) {
            result.toLong().toString()
        } else {
            String.format("%.10f", result).trimEnd('0').trimEnd('.')
        }
    } catch (e: Exception) {
        "Error"
    }
}

// Helper functions for persistent storage
fun saveHistoryToLocal(context: Context, history: List<CalculationHistory>) {
    try {
        val sharedPrefs = context.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(history)
        sharedPrefs.edit().putString("calculation_history", json).apply()
    } catch (e: Exception) {
        android.util.Log.e("Calculator", "Error saving history locally: ${e.message}")
    }
}

fun loadHistoryFromLocal(context: Context): List<CalculationHistory> {
    return try {
        val sharedPrefs = context.getSharedPreferences("calculator_prefs", Context.MODE_PRIVATE)
        val json = sharedPrefs.getString("calculation_history", null)
        if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<CalculationHistory>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    } catch (e: Exception) {
        android.util.Log.e("Calculator", "Error loading history locally: ${e.message}")
        emptyList()
    }
}

fun loadCalculationHistory(context: Context): List<CalculationHistory> {
    return loadHistoryFromLocal(context)
}

fun saveHistoryToCloud(history: List<CalculationHistory>) {
    try {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid
        
        if (userId != null) {
            val historyData = hashMapOf(
                "history" to history.map { 
                    hashMapOf(
                        "expression" to it.expression,
                        "result" to it.result,
                        "timestamp" to it.timestamp
                    )
                },
                "lastUpdated" to System.currentTimeMillis()
            )
            
            firestore.collection("calculator_history")
                .document(userId)
                .set(historyData)
                .addOnSuccessListener {
                    android.util.Log.d("Calculator", "History saved to cloud successfully")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Calculator", "Error saving history to cloud: ${e.message}")
                }
        }
    } catch (e: Exception) {
        android.util.Log.e("Calculator", "Error in cloud save: ${e.message}")
    }
}

fun clearHistoryFromCloud() {
    try {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val userId = auth.currentUser?.uid
        
        if (userId != null) {
            firestore.collection("calculator_history")
                .document(userId)
                .delete()
                .addOnSuccessListener {
                    android.util.Log.d("Calculator", "Cloud history cleared successfully")
                }
                .addOnFailureListener { e ->
                    android.util.Log.e("Calculator", "Error clearing cloud history: ${e.message}")
                }
        }
    } catch (e: Exception) {
        android.util.Log.e("Calculator", "Error in cloud clear: ${e.message}")
    }
}
