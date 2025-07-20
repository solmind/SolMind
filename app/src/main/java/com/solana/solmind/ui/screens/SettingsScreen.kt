package com.solana.solmind.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    var isDarkTheme by remember { mutableStateOf(false) }
    var enableNotifications by remember { mutableStateOf(true) }
    var autoSync by remember { mutableStateOf(true) }
    var syncInterval by remember { mutableStateOf("15 minutes") }
    var showConfidenceScore by remember { mutableStateOf(true) }
    
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
            // App Settings Section
            SettingsSection(title = "App Settings") {
                SettingsItem(
                    icon = Icons.Default.Settings,
                    title = "Dark Theme",
                    subtitle = "Use dark theme for the app",
                    trailing = {
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = { isDarkTheme = it }
                        )
                    }
                )
                
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
            
            // Sync Settings Section
            SettingsSection(title = "Sync Settings") {
                SettingsItem(
                    icon = Icons.Default.Refresh,
                    title = "Auto Sync",
                    subtitle = "Automatically sync wallet transactions",
                    trailing = {
                        Switch(
                            checked = autoSync,
                            onCheckedChange = { autoSync = it }
                        )
                    }
                )
                
                if (autoSync) {
                    SettingsItem(
                        icon = Icons.Default.Settings,
                        title = "Sync Interval",
                        subtitle = syncInterval,
                        onClick = {
                            // Show interval selection dialog
                        }
                    )
                }
            }
            
            // AI Settings Section
            SettingsSection(title = "AI Settings") {
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
                    subtitle = "Export your ledger data to CSV",
                    onClick = {
                        // Handle export
                    }
                )
                
                SettingsItem(
                    icon = Icons.Default.Delete,
                    title = "Clear All Data",
                    subtitle = "Delete all ledger entries and wallets",
                    onClick = {
                        // Show confirmation dialog
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
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
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                content()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
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