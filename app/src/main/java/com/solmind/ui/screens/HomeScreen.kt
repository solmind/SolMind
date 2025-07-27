package com.solmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solmind.data.model.AccountMode
import com.solmind.data.model.LedgerEntry
import com.solmind.data.model.TransactionCategory
import com.solmind.data.model.TransactionType
import com.solmind.ui.components.AnimatedModeSwitcher
import com.solmind.ui.theme.*
import com.solmind.ui.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.Calendar
import com.solmind.utils.CurrencyFormatter
import com.solmind.data.manager.CurrencyPreferenceManager
import com.solmind.data.manager.CurrencyDisplayMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import com.solmind.data.manager.ThemeMode
import com.solmind.data.manager.ThemePreferenceManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currencyPreferenceManager = remember { CurrencyPreferenceManager(context) }
    val themePreferenceManager = remember { ThemePreferenceManager(context) }
    val currencyDisplayMode by currencyPreferenceManager.currencyDisplayMode.collectAsState(initial = CurrencyDisplayMode.SOL)
    
    val entries by viewModel.entries.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val currentAccountMode by viewModel.currentAccountMode.collectAsState()
    val balance = totalIncome - totalExpenses
    
    // Get current theme mode
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val userThemeMode by themePreferenceManager.getThemeModeForAccountMode(currentAccountMode)
        .collectAsState(initial = when (currentAccountMode) {
            AccountMode.ONCHAIN -> ThemeMode.LIGHT
            AccountMode.OFFCHAIN -> ThemeMode.DARK
        })
    
    // Determine if we should use light theme
    val isLightTheme = when (userThemeMode) {
        ThemeMode.LIGHT -> true
        ThemeMode.DARK -> false
        ThemeMode.SYSTEM -> !isSystemInDarkTheme
    }
    
    // Pixel-inspired background with gradient (theme-aware)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isLightTheme) {
                        listOf(
                            Color(0xFFF5F5F5),
                            Color(0xFFE8E8F0),
                            Color(0xFFDCDCE8)
                        )
                    } else {
                        listOf(
                            Color(0xFF0A0A0A),
                            Color(0xFF1A1A2E),
                            Color(0xFF16213E)
                        )
                    }
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        item {
            // Header with Account Mode Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App title/greeting
                Column {
                    Text(
                        text = "SolMind",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isLightTheme) Color.Black else Color.White
                    )
                    Text(
                        text = if (currentAccountMode == AccountMode.ONCHAIN) "Onchain Mode" else "Offchain Mode",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isLightTheme) Color.Gray else Color.Gray
                    )
                }
                
                // Compact mode switcher
                AnimatedModeSwitcher(
                    currentMode = currentAccountMode,
                    onModeChange = { newMode ->
                        viewModel.setAccountMode(newMode)
                    },
                    compact = true
                )
            }
        }
        
        item {
            // Pixel-style Balance Overview Card (theme-aware)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 0.dp,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .border(
                        width = 3.dp,
                        color = SolanaPurple,
                        shape = RoundedCornerShape(0.dp)
                    ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF0F0F23)
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Header with wallet icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = SolanaPurple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "WALLET BALANCE",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = if (isLightTheme) Color.Black else Color.White
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Main balance with pixel-style border (theme-aware)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 2.dp,
                                color = if (balance >= 0) IncomeGreen else ExpenseRed,
                                shape = RoundedCornerShape(0.dp)
                            )
                            .background(
                                color = if (isLightTheme) Color(0xFFF0F0F0) else Color(0xFF1A1A2E),
                                shape = RoundedCornerShape(0.dp)
                            )
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = CurrencyFormatter.formatAmount(balance, currencyDisplayMode, currentAccountMode),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            color = if (balance >= 0) IncomeGreen else ExpenseRed,
                            textAlign = TextAlign.Center
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Income and Expenses with pixel-style cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Income Card (theme-aware)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 2.dp,
                                    color = IncomeGreen,
                                    shape = RoundedCornerShape(0.dp)
                                )
                                .background(
                                    color = if (isLightTheme) Color(0xFFE8F5E8) else Color(0xFF0A2A0A),
                                    shape = RoundedCornerShape(0.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = IncomeGreen,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "INCOME",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = IncomeGreen
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = CurrencyFormatter.formatAmount(totalIncome, currencyDisplayMode, currentAccountMode),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = IncomeGreen
                                )
                            }
                        }
                        
                        // Expenses Card (theme-aware)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = 2.dp,
                                    color = ExpenseRed,
                                    shape = RoundedCornerShape(0.dp)
                                )
                                .background(
                                    color = if (isLightTheme) Color(0xFFFDE8E8) else Color(0xFF2A0A0A),
                                    shape = RoundedCornerShape(0.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingDown,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "EXPENSES",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        ),
                                        color = ExpenseRed
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = CurrencyFormatter.formatAmount(totalExpenses, currencyDisplayMode, currentAccountMode),
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = ExpenseRed
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            // Quick Stats Overview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Total Transactions
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = SolanaPurple,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .background(
                            color = if (isLightTheme) Color(0xFFF8F8FF) else Color(0xFF1A1A2E),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = entries.size.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = SolanaPurple
                        )
                        Text(
                            text = "TOTAL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isLightTheme) Color.DarkGray else Color.Gray
                        )
                    }
                }
                
                // This Month
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = IncomeGreen,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .background(
                            color = if (isLightTheme) Color(0xFFF0FFF0) else Color(0xFF0A2A0A),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = entries.filter { 
                                val cal = Calendar.getInstance()
                                val entryCal = Calendar.getInstance().apply { time = it.date }
                                cal.get(Calendar.MONTH) == entryCal.get(Calendar.MONTH) &&
                                cal.get(Calendar.YEAR) == entryCal.get(Calendar.YEAR)
                            }.size.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = IncomeGreen
                        )
                        Text(
                            text = "THIS MONTH",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isLightTheme) Color.DarkGray else Color.Gray
                        )
                    }
                }
                
                // Average per day
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .border(
                            width = 1.dp,
                            color = ExpenseRed,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .background(
                            color = if (isLightTheme) Color(0xFFFFF0F0) else Color(0xFF2A0A0A),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (entries.isNotEmpty()) {
                                val avgExpense = entries.filter { it.type == TransactionType.EXPENSE }
                                    .map { it.amount }.average()
                                if (avgExpense.isNaN()) "0" else String.format("%.1f", avgExpense)
                            } else "0",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = ExpenseRed
                        )
                        Text(
                            text = "AVG EXP",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isLightTheme) Color.DarkGray else Color.Gray
                        )
                    }
                }
            }
        }
        
        item {
            // Recent Transactions Section Header (theme-aware)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 2.dp,
                        color = SolanaPurple,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .background(
                        color = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF0F0F23),
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = ">>> RECENT TRANSACTIONS",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    ),
                    color = if (isLightTheme) Color.Black else Color.White
                )
            }
        }
        
        items(entries.take(15)) { entry ->
            TransactionItem(
                entry = entry,
                currencyDisplayMode = currencyDisplayMode,
                accountMode = currentAccountMode,
                isLightTheme = isLightTheme,
                onClick = { /* Navigate to detail */ }
            )
        }
        
        if (entries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = Color.Gray,
                            shape = RoundedCornerShape(0.dp)
                        )
                        .background(
                            color = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF0F0F23),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = ">>> NO TRANSACTIONS YET <<<",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = if (isLightTheme) Color.DarkGray else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "START TRACKING YOUR EXPENSES AND INCOME",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            ),
                            color = if (isLightTheme) Color.DarkGray else Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
 }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionItem(
    entry: LedgerEntry,
    currencyDisplayMode: CurrencyDisplayMode,
    accountMode: AccountMode,
    isLightTheme: Boolean,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val isIncome = entry.type == TransactionType.INCOME
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = if (isIncome) IncomeGreen else ExpenseRed,
                shape = RoundedCornerShape(0.dp)
            )
            .background(
                color = if (isLightTheme) Color(0xFFFAFAFA) else Color(0xFF0F0F23),
                shape = RoundedCornerShape(0.dp)
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Pixel-style Category Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 2.dp,
                            color = getCategoryColor(entry.category),
                            shape = RoundedCornerShape(0.dp)
                        )
                        .background(
                            color = getCategoryColor(entry.category).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(0.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entry.category.getEmoji(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Transaction Details
                Column {
                    Text(
                        text = entry.description.uppercase(),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = if (isLightTheme) Color.Black else Color.White
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.category.getDisplayName().uppercase(),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isLightTheme) Color.DarkGray else Color.Gray
                        )
                        if (entry.isAutoDetected) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = SolanaPurple,
                                        shape = RoundedCornerShape(0.dp)
                                    )
                                    .background(
                                        SolanaPurple.copy(alpha = 0.1f),
                                        RoundedCornerShape(0.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "AI",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = SolanaPurple
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = dateFormat.format(entry.date).uppercase(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        ),
                        color = if (isLightTheme) Color.DarkGray else Color.Gray
                    )
                }
            }
            
            // Amount with pixel-style background (theme-aware)
            Box(
                modifier = Modifier
                    .border(
                        width = 1.dp,
                        color = if (isIncome) IncomeGreen else ExpenseRed,
                        shape = RoundedCornerShape(0.dp)
                    )
                    .background(
                        color = if (isLightTheme) {
                            if (isIncome) Color(0xFFE8F5E8) else Color(0xFFFDE8E8)
                        } else {
                            if (isIncome) Color(0xFF0A2A0A) else Color(0xFF2A0A0A)
                        },
                        shape = RoundedCornerShape(0.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = CurrencyFormatter.formatTransactionAmount(entry.amount, isIncome, currencyDisplayMode, accountMode),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (isIncome) IncomeGreen else ExpenseRed
                )
            }
        }
    }
}

fun getCategoryColor(category: TransactionCategory): androidx.compose.ui.graphics.Color {
    return when (category) {
        TransactionCategory.FOOD_DINING -> androidx.compose.ui.graphics.Color(0xFFFF6B6B)
        TransactionCategory.TRANSPORTATION -> androidx.compose.ui.graphics.Color(0xFF4ECDC4)
        TransactionCategory.SHOPPING -> androidx.compose.ui.graphics.Color(0xFFFFE66D)
        TransactionCategory.ENTERTAINMENT -> androidx.compose.ui.graphics.Color(0xFFFF8B94)
        TransactionCategory.UTILITIES -> androidx.compose.ui.graphics.Color(0xFF95E1D3)
        TransactionCategory.HEALTHCARE -> androidx.compose.ui.graphics.Color(0xFFFCE38A)
        TransactionCategory.EDUCATION -> androidx.compose.ui.graphics.Color(0xFF6C5CE7)
        TransactionCategory.TRAVEL -> androidx.compose.ui.graphics.Color(0xFF74B9FF)
        TransactionCategory.INVESTMENT -> androidx.compose.ui.graphics.Color(0xFF00B894)
        TransactionCategory.SALARY -> androidx.compose.ui.graphics.Color(0xFF00CEC9)
        TransactionCategory.FREELANCE -> androidx.compose.ui.graphics.Color(0xFFFD79A8)
        TransactionCategory.BUSINESS -> androidx.compose.ui.graphics.Color(0xFFE17055)
        TransactionCategory.GIFTS -> androidx.compose.ui.graphics.Color(0xFFA29BFE)
        TransactionCategory.OTHER -> androidx.compose.ui.graphics.Color(0xFF636E72)
        // Blockchain-specific category colors
        TransactionCategory.DEFI_SWAP -> androidx.compose.ui.graphics.Color(0xFF9B59B6)
        TransactionCategory.DEFI_LENDING -> androidx.compose.ui.graphics.Color(0xFF3498DB)
        TransactionCategory.DEFI_STAKING -> androidx.compose.ui.graphics.Color(0xFF2ECC71)
        TransactionCategory.NFT_PURCHASE -> androidx.compose.ui.graphics.Color(0xFFE74C3C)
        TransactionCategory.NFT_SALE -> androidx.compose.ui.graphics.Color(0xFFF39C12)
        TransactionCategory.TOKEN_TRANSFER -> androidx.compose.ui.graphics.Color(0xFF1ABC9C)
        TransactionCategory.BRIDGE -> androidx.compose.ui.graphics.Color(0xFF34495E)
        TransactionCategory.GAMING -> androidx.compose.ui.graphics.Color(0xFFE67E22)
        TransactionCategory.MINTING -> androidx.compose.ui.graphics.Color(0xFF8E44AD)
        TransactionCategory.TRADING -> androidx.compose.ui.graphics.Color(0xFF27AE60)
    }
}