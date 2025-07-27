package com.solmind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solmind.data.manager.ThemeMode
import com.solmind.data.manager.ThemePreferenceManager
import com.solmind.data.manager.CurrencyPreferenceManager
import com.solmind.data.manager.CurrencyDisplayMode
import com.solmind.data.model.AccountMode
import com.solmind.service.LanguageModel
import com.solmind.service.ModelDownloadStatus
import com.solmind.service.SubscriptionManager
import com.solmind.service.SubscriptionTier
import com.solmind.service.SubscriptionBenefits
import com.solmind.ui.viewmodel.ModelManagerViewModel
import com.solmind.ui.viewmodel.WalletViewModel
import com.solmind.ui.viewmodel.SettingsViewModel
import com.solmind.ui.viewmodel.ExportStatus
import com.solmind.ui.viewmodel.ClearDataStatus
import com.solmind.manager.SyncManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    walletViewModel: WalletViewModel = hiltViewModel(),
    modelManagerViewModel: ModelManagerViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val themePreferenceManager = remember { ThemePreferenceManager(context) }
    val currencyPreferenceManager = remember { CurrencyPreferenceManager(context) }
    val subscriptionManager = remember { SubscriptionManager(context) }
    val syncManager = remember { SyncManager(context) }
    
    var enableNotifications by remember { mutableStateOf(true) }
    val autoSync by syncManager.isAutoSyncEnabled.collectAsState()
    val currentSyncInterval by syncManager.syncInterval.collectAsState()
    var showSyncIntervalDialog by remember { mutableStateOf(false) }
    var showConfidenceScore by remember { mutableStateOf(true) }
    var showAddWalletDialog by remember { mutableStateOf(false) }
    var showOnchainThemeDialog by remember { mutableStateOf(false) }
    var showOffchainThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showModelSelectionDialog by remember { mutableStateOf(false) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    val currentAccountMode by walletViewModel.currentAccountMode.collectAsState()
    val onchainThemeMode by themePreferenceManager.onchainThemeMode.collectAsState(initial = ThemeMode.LIGHT)
    val offchainThemeMode by themePreferenceManager.offchainThemeMode.collectAsState(initial = ThemeMode.DARK)
    val currencyDisplayMode by currencyPreferenceManager.currencyDisplayMode.collectAsState(initial = CurrencyDisplayMode.SOL)
    val selectedModel by modelManagerViewModel.selectedModel.collectAsState()
    val modelStates by modelManagerViewModel.modelStates.collectAsState()
    val isSubscribed by subscriptionManager.isSubscribed.collectAsState()
    val subscriptionTier by subscriptionManager.subscriptionTier.collectAsState()
    val exportStatus by settingsViewModel.exportStatus.collectAsState()
    val clearDataStatus by settingsViewModel.clearDataStatus.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    // Clear export status after success or error
    LaunchedEffect(exportStatus) {
        when (exportStatus) {
            is ExportStatus.Success, is ExportStatus.Error -> {
                kotlinx.coroutines.delay(3000) // Show status for 3 seconds
                settingsViewModel.clearExportStatus()
            }
            else -> {}
        }
    }
    
    // Clear clear data status after delay
    LaunchedEffect(clearDataStatus) {
        when (clearDataStatus) {
            is ClearDataStatus.Success, is ClearDataStatus.Error -> {
                kotlinx.coroutines.delay(3000) // Show status for 3 seconds
                settingsViewModel.clearClearDataStatus()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Subscription Section
            if (!isSubscribed) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .clickable { showUpgradeDialog = true },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upgrade to SolMind Master",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Unlock cloud AI models and premium features",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Icon(
                            Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "SolMind Master Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            // Theme Settings Section
            SettingsSection(title = "Theme Settings") {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Onchain Mode Theme",
                    subtitle = "Theme for onchain mode: ${onchainThemeMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    onClick = { showOnchainThemeDialog = true }
                )
                
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Offchain Mode Theme",
                    subtitle = "Theme for offchain mode: ${offchainThemeMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    onClick = { showOffchainThemeDialog = true }
                )
            }
            
            // Currency Settings Section (only show in onchain mode)
            if (currentAccountMode == AccountMode.ONCHAIN) {
                SettingsSection(title = "Currency Settings") {
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Display Currency",
                        subtitle = "Show amounts in: ${if (currencyDisplayMode == CurrencyDisplayMode.USD) "USD ($)" else "SOL (◎)"}",
                        onClick = { showCurrencyDialog = true }
                    )
                }
            }
            
            // App Settings Section
            SettingsSection(title = "App Settings") {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Enable push notifications",
                    trailing = {
                        Switch(
                            checked = enableNotifications,
                            onCheckedChange = { enableNotifications = it }
                        )
                    }
                )
            }
            
            // Wallet Management Section
            if (currentAccountMode == AccountMode.ONCHAIN) {
                SettingsSection(title = "Wallet Management") {
                    SettingsItem(
                        icon = Icons.Default.Add,
                        title = "Add Wallet",
                        subtitle = "Add a new Solana wallet address",
                        onClick = { showAddWalletDialog = true }
                    )
                    
                    SettingsItem(
                        icon = Icons.Default.AccountCircle,
                        title = "Manage Wallets",
                        subtitle = "View and manage your wallets",
                        onClick = {
                            navController.navigate("wallet")
                        }
                    )
                }
            }
            
            // Sync Settings Section
            SettingsSection(title = "Sync Settings") {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Auto Sync",
                    subtitle = "Automatically sync wallet transactions",
                    trailing = {
                        Switch(
                                    checked = autoSync,
                                    onCheckedChange = { enabled ->
                                        syncManager.setAutoSyncEnabled(enabled)
                                    }
                                )
                    }
                )
                
                if (autoSync) {
                    SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Sync Interval",
                    subtitle = currentSyncInterval.displayName,
                    onClick = {
                        showSyncIntervalDialog = true
                    }
                )
                }
            }
            
            // AI Settings Section
            SettingsSection(title = "AI Settings") {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Language Model",
                    subtitle = selectedModel?.let { "Current: ${it.name} (${if (modelManagerViewModel.isModelDownloaded(it.id)) "Downloaded" else "Not Downloaded"})" } ?: "No model selected",
                    onClick = { 
                        modelManagerViewModel.loadModelSizes()
                        showModelSelectionDialog = true 
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Star,
                    title = "Show Confidence Score",
                    subtitle = "Display AI confidence for categorization",
                    trailing = {
                        Switch(
                            checked = showConfidenceScore,
                            onCheckedChange = { showConfidenceScore = it }
                        )
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Check,
                    title = "Auto-categorize",
                    subtitle = "Automatically categorize transactions with high confidence"
                )
            }
            
            // Data Management Section
            SettingsSection(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "Export Data",
                    subtitle = when (val status = exportStatus) {
                        is ExportStatus.Loading -> "Exporting..."
                        is ExportStatus.Success -> status.message
                        is ExportStatus.Error -> "Export failed: ${status.message}"
                        else -> "Export transactions for current ${currentAccountMode.getDisplayName()} mode to CSV"
                    },
                    onClick = {
                        settingsViewModel.exportTransactionsToCSV(context, currentAccountMode)
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear All Data",
                    subtitle = when (val status = clearDataStatus) {
                        is ClearDataStatus.Loading -> "Clearing data..."
                        is ClearDataStatus.Success -> status.message
                        is ClearDataStatus.Error -> status.message
                        else -> "Delete all ledger entries and wallets"
                    },
                    onClick = {
                        settingsViewModel.showClearDataConfirmation()
                    }
                )
            }
            
            // About Section
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "Version",
                    subtitle = "1.0.0 (Beta)"
                )
                
                SettingsItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Open Source",
                    subtitle = "View source code on GitHub",
                    onClick = {
                        // Open GitHub link
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Warning,
                    title = "Report Bug",
                    subtitle = "Help us improve the app",
                    onClick = {
                        // Open bug report
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AI Ledger for Solana",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Built for Solana Mobile Hackathon",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
    
    // Theme Selection Dialogs
    if (showOnchainThemeDialog) {
        ThemeSelectionDialog(
            title = "Onchain Mode Theme",
            currentTheme = onchainThemeMode,
            onDismiss = { showOnchainThemeDialog = false },
            onThemeSelected = { theme ->
                coroutineScope.launch {
                    themePreferenceManager.setOnchainTheme(theme)
                }
                showOnchainThemeDialog = false
            }
        )
    }
    
    if (showOffchainThemeDialog) {
        ThemeSelectionDialog(
            title = "Offchain Mode Theme",
            currentTheme = offchainThemeMode,
            onDismiss = { showOffchainThemeDialog = false },
            onThemeSelected = { theme ->
                coroutineScope.launch {
                    themePreferenceManager.setOffchainTheme(theme)
                }
                showOffchainThemeDialog = false
            }
        )
    }
    
    // Currency Selection Dialog
    if (showCurrencyDialog) {
        CurrencySelectionDialog(
            currentMode = currencyDisplayMode,
            onDismiss = { showCurrencyDialog = false },
            onModeSelected = { mode ->
                coroutineScope.launch {
                    currencyPreferenceManager.setCurrencyDisplayMode(mode)
                }
                showCurrencyDialog = false
            }
        )
    }
    
    // Add Wallet Dialog
    if (showAddWalletDialog) {
        AddWalletDialog(
            onDismiss = { showAddWalletDialog = false },
            onAddWallet = { address, name ->
                walletViewModel.addWallet(address, name)
                showAddWalletDialog = false
            }
        )
    }
    
    // Model Selection Dialog
    if (showModelSelectionDialog) {
        ModelSelectionDialog(
            modelManagerViewModel = modelManagerViewModel,
            modelStates = modelStates,
            selectedModel = selectedModel,
            isSubscribed = isSubscribed,
            onDismiss = { showModelSelectionDialog = false },
            onModelSelected = { model ->
                if (model.requiresSubscription && !isSubscribed) {
                    showModelSelectionDialog = false
                    showUpgradeDialog = true
                } else {
                    modelManagerViewModel.selectModel(model)
                    showModelSelectionDialog = false
                }
            },
            onDownloadModel = { modelId ->
                modelManagerViewModel.downloadModel(modelId)
            },
            onDeleteModel = { modelId ->
                modelManagerViewModel.deleteModel(modelId)
            },
            onUpgradeClicked = {
                showModelSelectionDialog = false
                showUpgradeDialog = true
            }
        )
    }
    
    // Upgrade Dialog
    if (showUpgradeDialog) {
        UpgradeDialog(
            onDismiss = { showUpgradeDialog = false },
            onUpgrade = {
                subscriptionManager.upgradeToMaster()
                showUpgradeDialog = false
            }
        )
    }
    
    // Sync Interval Dialog
    if (showSyncIntervalDialog) {
        SyncIntervalDialog(
            currentInterval = currentSyncInterval,
            onDismiss = { showSyncIntervalDialog = false },
            onIntervalSelected = { interval ->
                syncManager.setSyncInterval(interval)
                showSyncIntervalDialog = false
            }
        )
    }
    
    // Clear Data Confirmation Dialog
    if (clearDataStatus is ClearDataStatus.AwaitingConfirmation) {
        AlertDialog(
            onDismissRequest = { settingsViewModel.cancelClearData() },
            title = { Text("Clear All Data") },
            text = {
                Text(
                    text = "This will permanently delete all ledger entries and wallets. This action cannot be undone.\n\nAre you sure you want to continue?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { settingsViewModel.clearAllData() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { settingsViewModel.cancelClearData() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SyncIntervalDialog(
    currentInterval: SyncManager.SyncInterval,
    onDismiss: () -> Unit,
    onIntervalSelected: (SyncManager.SyncInterval) -> Unit
) {
    val intervals = SyncManager.SyncInterval.values()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync Interval") },
        text = {
            Column {
                Text(
                    text = "Choose how often to automatically sync wallet data:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                for (interval in intervals) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onIntervalSelected(interval) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentInterval == interval,
                            onClick = { onIntervalSelected(interval) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = interval.displayName,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun CurrencySelectionDialog(
    currentMode: CurrencyDisplayMode,
    onDismiss: () -> Unit,
    onModeSelected: (CurrencyDisplayMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Display Currency") },
        text = {
            Column {
                Text(
                    text = "Choose how to display transaction amounts:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                CurrencyDisplayMode.values().forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentMode == mode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = when (mode) {
                                    CurrencyDisplayMode.USD -> "USD ($)"
                                    CurrencyDisplayMode.SOL -> "SOL (◎)"
                                },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (mode) {
                                    CurrencyDisplayMode.USD -> "Show amounts in US Dollars"
                                    CurrencyDisplayMode.SOL -> "Show amounts in SOL tokens"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun UpgradeDialog(
    onDismiss: () -> Unit,
    onUpgrade: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Upgrade to SolMind Master",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Unlock premium features and support SolMind development:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                SubscriptionBenefits.masterBenefits.forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            when (benefit.icon) {
                                "cloud" -> Icons.Default.CloudDone
                                "favorite" -> Icons.Default.Favorite
                                "star" -> Icons.Default.Star
                                "support" -> Icons.Default.SupportAgent
                                "analytics" -> Icons.Default.Analytics
                                else -> Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = benefit.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = benefit.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Early Bird Special",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "1 SOL",
                                style = MaterialTheme.typography.titleMedium,
                                textDecoration = TextDecoration.LineThrough,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "0.5 SOL",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "(~$90 USD)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Regular price: ~$180 USD",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpgrade,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Upgrade Now")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Maybe Later")
            }
        }
    )
}

@Composable
fun ModelSelectionDialog(
    modelManagerViewModel: ModelManagerViewModel,
    modelStates: List<com.solmind.service.ModelState>,
    selectedModel: LanguageModel?,
    isSubscribed: Boolean,
    onDismiss: () -> Unit,
    onModelSelected: (LanguageModel) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onUpgradeClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                text = "Select Language Model",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Choose a local AI model for transaction parsing:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(modelStates) { modelState ->
                        ModelItem(
                            modelState = modelState,
                            isSelected = selectedModel?.id == modelState.model.id,
                            isSubscribed = isSubscribed,
                            onModelSelected = onModelSelected,
                            onDownloadModel = onDownloadModel,
                            onDeleteModel = onDeleteModel,
                            onUpgradeClicked = onUpgradeClicked
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun ModelItem(
    modelState: com.solmind.service.ModelState,
    isSelected: Boolean,
    isSubscribed: Boolean,
    onModelSelected: (LanguageModel) -> Unit,
    onDownloadModel: (String) -> Unit,
    onDeleteModel: (String) -> Unit,
    onUpgradeClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (!modelState.model.isLocal) {
                    // Cloud model - redirect to upgrade page
                    onUpgradeClicked()
                } else if (modelState.status == ModelDownloadStatus.DOWNLOADED) {
                    onModelSelected(modelState.model)
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { 
                                if (!modelState.model.isLocal) {
                                    // Cloud model - redirect to upgrade page
                                    onUpgradeClicked()
                                } else if (modelState.status == ModelDownloadStatus.DOWNLOADED) {
                                    onModelSelected(modelState.model)
                                }
                            },
                            enabled = modelState.model.isLocal && modelState.status == ModelDownloadStatus.DOWNLOADED
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = modelState.model.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!modelState.model.isLocal) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "COMING",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .background(
                                                MaterialTheme.colorScheme.tertiaryContainer,
                                                RoundedCornerShape(4.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (modelState.model.isDefault) {
                                Text(
                                    text = "Default",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = modelState.model.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "Size: ${modelState.model.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Action button
                if (!modelState.model.isLocal) {
                    // Cloud model - show as coming soon
                    Row {
                        Icon(
                            Icons.Default.CloudQueue,
                            contentDescription = "Coming soon",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    when (modelState.status) {
                        ModelDownloadStatus.NOT_DOWNLOADED -> {
                            IconButton(
                                onClick = { onDownloadModel(modelState.model.id) }
                            ) {
                                Icon(
                                    Icons.Default.GetApp,
                                    contentDescription = "Download model",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    
                    ModelDownloadStatus.DOWNLOADING -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                progress = modelState.progress,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${(modelState.progress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                    
                    ModelDownloadStatus.DOWNLOADED -> {
                        Row {
                            if (!isSelected) {
                                IconButton(
                                    onClick = { onDeleteModel(modelState.model.id) }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete model",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Downloaded",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                        ModelDownloadStatus.ERROR -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                     Icons.Default.Warning,
                                     contentDescription = "Error",
                                     tint = MaterialTheme.colorScheme.error,
                                     modifier = Modifier.size(24.dp)
                                 )
                                Text(
                                    text = "Error",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionDialog(
    title: String,
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ThemeMode.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentTheme == theme,
                            onClick = { onThemeSelected(theme) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = theme.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = when (theme) {
                                    ThemeMode.LIGHT -> "Always use light theme"
                                    ThemeMode.DARK -> "Always use dark theme"
                                    ThemeMode.SYSTEM -> "Follow system setting"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                content()
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val modifier = if (onClick != null) {
        Modifier.clickable { onClick() }
    } else {
        Modifier
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        trailing?.invoke()
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