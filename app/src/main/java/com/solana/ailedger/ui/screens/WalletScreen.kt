package com.solana.ailedger.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solana.ailedger.data.model.SolanaWallet
import com.solana.ailedger.ui.theme.ExpenseColor
import com.solana.ailedger.ui.theme.IncomeColor
import com.solana.ailedger.ui.viewmodel.WalletViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletScreen(
    navController: NavController,
    viewModel: WalletViewModel = hiltViewModel()
) {
    val wallets by viewModel.wallets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadWallets()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Solana Wallets") },
                actions = {
                    IconButton(
                        onClick = { viewModel.syncAllWallets() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Sync All")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Wallet")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error Display
            error?.let { errorMessage ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Wallets List
            if (wallets.isEmpty() && !isLoading) {
                EmptyWalletsState(
                    onAddWallet = { showAddDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(wallets) { wallet ->
                        WalletCard(
                            wallet = wallet,
                            onSync = { viewModel.syncWallet(wallet.address) },
                            onDelete = { viewModel.deleteWallet(wallet.address) },
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
    
    // Add Wallet Dialog
    if (showAddDialog) {
        AddWalletDialog(
            onDismiss = { showAddDialog = false },
            onAddWallet = { address, name ->
                viewModel.addWallet(address, name)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun EmptyWalletsState(
    onAddWallet: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Wallet,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Wallets Added",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Add a Solana wallet address to start tracking your on-chain activities",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAddWallet
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Wallet")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalletCard(
    wallet: SolanaWallet,
    onSync: () -> Unit,
    onDelete: () -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = wallet.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${wallet.address.take(8)}...${wallet.address.takeLast(8)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row {
                    IconButton(
                        onClick = onSync,
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = !isLoading
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ExpenseColor
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status and Last Sync
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Status
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val statusColor = when (wallet.status) {
                        "active" -> IncomeColor
                        "error" -> ExpenseColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = statusColor,
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = wallet.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                
                // Last Sync
                wallet.lastSyncTime?.let { lastSync ->
                    Text(
                        text = "Last sync: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(lastSync)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Balance (if available)
            wallet.balance?.let { balance ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Balance: ${String.format("%.4f", balance)} SOL",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AddWalletDialog(
    onDismiss: () -> Unit,
    onAddWallet: (String, String) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var addressError by remember { mutableStateOf<String?>(null) }
    var nameError by remember { mutableStateOf<String?>(null) }
    
    fun validateInputs(): Boolean {
        addressError = when {
            address.isEmpty() -> "Address is required"
            address.length < 32 -> "Invalid Solana address"
            else -> null
        }
        
        nameError = when {
            name.isEmpty() -> "Name is required"
            name.length < 2 -> "Name must be at least 2 characters"
            else -> null
        }
        
        return addressError == null && nameError == null
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Solana Wallet") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        nameError = null
                    },
                    label = { Text("Wallet Name") },
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = address,
                    onValueChange = { 
                        address = it
                        addressError = null
                    },
                    label = { Text("Wallet Address") },
                    isError = addressError != null,
                    supportingText = addressError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter Solana wallet address...") }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (validateInputs()) {
                        onAddWallet(address.trim(), name.trim())
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}