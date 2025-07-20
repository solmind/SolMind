package com.solana.ailedger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solana.ailedger.data.model.LedgerEntry
import com.solana.ailedger.data.model.TransactionType
import com.solana.ailedger.ui.theme.*
import com.solana.ailedger.ui.viewmodel.HomeViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val totalIncome by viewModel.totalIncome.collectAsState()
    val totalExpenses by viewModel.totalExpenses.collectAsState()
    val balance = totalIncome - totalExpenses
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Balance Overview Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = SolanaPurple
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current Balance",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = NumberFormat.getCurrencyInstance().format(balance),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        item {
            // Income/Expense Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Income Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = IncomeGreen.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Income",
                            tint = IncomeGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.bodyMedium,
                            color = IncomeGreen
                        )
                        Text(
                            text = NumberFormat.getCurrencyInstance().format(totalIncome),
                            style = MaterialTheme.typography.titleMedium,
                            color = IncomeGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Expense Card
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = ExpenseRed.copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingDown,
                            contentDescription = "Expenses",
                            tint = ExpenseRed
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ExpenseRed
                        )
                        Text(
                            text = NumberFormat.getCurrencyInstance().format(totalExpenses),
                            style = MaterialTheme.typography.titleMedium,
                            color = ExpenseRed,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        item {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        items(entries.take(10)) { entry ->
            TransactionItem(
                entry = entry,
                onClick = { /* Navigate to detail */ }
            )
        }
        
        if (entries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No transactions yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Start by adding your first transaction or connecting a Solana wallet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    entry: LedgerEntry,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val isIncome = entry.type == TransactionType.INCOME
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(getCategoryColor(entry.category).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entry.category.getEmoji(),
                    style = MaterialTheme.typography.titleLarge
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Transaction Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.description,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.category.getDisplayName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (entry.isAutoDetected) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AI",
                            style = MaterialTheme.typography.labelSmall,
                            color = SolanaPurple,
                            modifier = Modifier
                                .background(
                                    SolanaPurple.copy(alpha = 0.1f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dateFormat.format(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Amount
            Text(
                text = "${if (isIncome) "+" else "-"}${NumberFormat.getCurrencyInstance().format(entry.amount)}",
                style = MaterialTheme.typography.titleMedium,
                color = if (isIncome) IncomeGreen else ExpenseRed,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

fun getCategoryColor(category: com.solana.ailedger.data.model.TransactionCategory): Color {
    return when (category) {
        com.solana.ailedger.data.model.TransactionCategory.FOOD_DINING -> FoodColor
        com.solana.ailedger.data.model.TransactionCategory.TRANSPORTATION -> TransportColor
        com.solana.ailedger.data.model.TransactionCategory.SHOPPING -> ShoppingColor
        com.solana.ailedger.data.model.TransactionCategory.ENTERTAINMENT -> EntertainmentColor
        com.solana.ailedger.data.model.TransactionCategory.UTILITIES -> UtilitiesColor
        com.solana.ailedger.data.model.TransactionCategory.HEALTHCARE -> HealthcareColor
        com.solana.ailedger.data.model.TransactionCategory.EDUCATION -> EducationColor
        com.solana.ailedger.data.model.TransactionCategory.TRAVEL -> TravelColor
        com.solana.ailedger.data.model.TransactionCategory.INVESTMENT -> InvestmentColor
        com.solana.ailedger.data.model.TransactionCategory.SALARY -> SalaryColor
        com.solana.ailedger.data.model.TransactionCategory.BUSINESS -> BusinessColor
        com.solana.ailedger.data.model.TransactionCategory.GIFTS -> GiftsColor
        com.solana.ailedger.data.model.TransactionCategory.OTHER -> OtherColor
        else -> OtherColor
    }
}