@file:OptIn(ExperimentalMaterial3Api::class)

package com.solana.solmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import com.solana.solmind.ui.viewmodel.AddEntryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryScreen(
    navController: NavController,
    viewModel: AddEntryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.popBackStack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Transaction") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveEntry() },
                        enabled = !isLoading && uiState.isValid
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount Input
            OutlinedTextField(
                value = uiState.amount,
                onValueChange = viewModel::updateAmount,
                label = { Text("Amount") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.amountError != null,
                supportingText = uiState.amountError?.let { { Text(it) } }
            )
            
            // Description Input
            OutlinedTextField(
                value = uiState.description,
                onValueChange = viewModel::updateDescription,
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.descriptionError != null,
                supportingText = uiState.descriptionError?.let { { Text(it) } }
            )
            
            // AI Suggestion Button
            if (uiState.description.isNotEmpty()) {
                Button(
                    onClick = { viewModel.getAISuggestion() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    Text("Get AI Suggestion")
                }
            }
            
            // Transaction Type Selection
            Text(
                text = "Transaction Type",
                style = MaterialTheme.typography.titleMedium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TransactionType.values().forEach { type ->
                    FilterChip(
                        onClick = { viewModel.updateTransactionType(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        selected = uiState.transactionType == type,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            
            // Category Selection
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleMedium
            )
            
            // Simple grid layout for categories
            val categories = TransactionCategory.values().toList()
            val chunkedCategories = categories.chunked(2)
            
            Column(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chunkedCategories.forEach { rowCategories: List<TransactionCategory> ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCategories.forEach { category: TransactionCategory ->
                            CategoryChip(
                                category = category,
                                isSelected = uiState.category == category,
                                onClick = { viewModel.updateCategory(category) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Fill remaining space if row is not complete
                        if (rowCategories.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            
            // Date Selection
            OutlinedTextField(
                value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(uiState.date),
                onValueChange = { },
                label = { Text("Date") },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    TextButton(onClick = { viewModel.showDatePicker() }) {
                        Text("Change")
                    }
                }
            )
            
            // AI Confidence Indicator
            if (uiState.confidence > 0f) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "AI Confidence",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = uiState.confidence,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(uiState.confidence * 100).toInt()}% confident",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            
            // Error Display
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: TransactionCategory,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(category.getEmoji())
                Text(
                    text = category.getDisplayName(),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        selected = isSelected,
        modifier = modifier.fillMaxWidth(),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = getCategoryColor(category).copy(alpha = 0.2f)
        )
    )
}

@Composable
fun getCategoryColor(category: TransactionCategory): Color {
    return when (category) {
        TransactionCategory.FOOD_DINING -> Color(0xFFFF6B6B)
        TransactionCategory.TRANSPORTATION -> Color(0xFF4ECDC4)
        TransactionCategory.SHOPPING -> Color(0xFFFFE66D)
        TransactionCategory.ENTERTAINMENT -> Color(0xFFFF8B94)
        TransactionCategory.UTILITIES -> Color(0xFFA8E6CF)
        TransactionCategory.HEALTHCARE -> Color(0xFFFFAB91)
        TransactionCategory.EDUCATION -> Color(0xFF81C784)
        TransactionCategory.TRAVEL -> Color(0xFF9C27B0)
        TransactionCategory.INVESTMENT -> Color(0xFF2196F3)
        TransactionCategory.SALARY -> Color(0xFF4CAF50)
        TransactionCategory.FREELANCE -> Color(0xFF00BCD4)
        TransactionCategory.BUSINESS -> Color(0xFF795548)
        TransactionCategory.GIFTS -> Color(0xFFE91E63)
        TransactionCategory.OTHER -> Color(0xFF9E9E9E)
    }
}