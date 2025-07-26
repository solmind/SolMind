@file:OptIn(ExperimentalMaterial3Api::class)

package com.solana.solmind.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import com.solana.solmind.ui.viewmodel.AddEntryViewModel
import com.solana.solmind.ui.viewmodel.AddEntryUiState
import com.solana.solmind.ui.viewmodel.ChatMessage
import com.solana.solmind.ui.viewmodel.ModelManagerViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*
import com.solana.solmind.utils.CurrencyFormatter
import com.solana.solmind.data.manager.CurrencyPreferenceManager
import com.solana.solmind.data.manager.CurrencyDisplayMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.solana.solmind.service.SubscriptionManager
import com.solana.solmind.service.ModelManager
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class)
@Composable
fun AddEntryScreen(
    navController: NavController,
    viewModel: AddEntryViewModel = hiltViewModel(),
    modelManagerViewModel: ModelManagerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currencyPreferenceManager = remember { CurrencyPreferenceManager(context) }
    val subscriptionManager = remember { SubscriptionManager(context) }
    val currencyDisplayMode by currencyPreferenceManager.currencyDisplayMode.collectAsState(initial = CurrencyDisplayMode.SOL)
    
    val uiState by viewModel.uiState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val currentAccountMode by viewModel.currentAccountMode.collectAsState()
    val selectedModel by modelManagerViewModel.selectedModel.collectAsState()
    
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    // Removed automatic navigation on save to keep user on chatbot interface
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SolMind Assistant")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.showEditForm) {
            TransactionEditScreen(
                uiState = uiState,
                isLoading = isLoading,
                currencyDisplayMode = currencyDisplayMode,
                onAmountChange = { viewModel.updateAmount(it) },
                onDescriptionChange = { viewModel.updateDescription(it) },
                onTypeChange = { viewModel.updateTransactionType(it) },
                onCategoryChange = { viewModel.updateCategory(it) },
                onCustomCategoryInputChange = { viewModel.updateCustomCategoryInput(it) },
                onDateChange = { viewModel.updateDate(it) },
                onSave = { viewModel.saveEditedTransaction() },
                onCancel = { viewModel.cancelEdit() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else if (uiState.showPreview) {
            TransactionPreviewScreen(
                uiState = uiState,
                isLoading = isLoading,
                currencyDisplayMode = currencyDisplayMode,
                accountMode = currentAccountMode,
                onConfirm = { viewModel.confirmTransaction() },
                onEdit = { viewModel.editTransaction() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        } else {
            ChatInterface(
                chatMessages = uiState.chatMessages,
                isLoading = uiState.isProcessingMessage,
                onSendMessage = { message -> viewModel.sendMessage(message) },
                onSelectImage = { viewModel.selectImage() },
                onTestFlanT5 = { testMessage -> viewModel.parseWithFlanT5(testMessage) },
                onUpgradeClicked = { showUpgradeDialog = true },
                selectedModel = selectedModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
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
}

@Composable
fun ChatInterface(
    chatMessages: List<ChatMessage>,
    isLoading: Boolean,
    onSendMessage: (String) -> Unit,
    onSelectImage: () -> Unit,
    onTestFlanT5: ((String) -> Unit)? = null,
    onUpgradeClicked: (() -> Unit)? = null,
    selectedModel: com.solana.solmind.service.LanguageModel? = null,
    modifier: Modifier = Modifier
) {
    var messageText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Welcome message if no chat messages
        if (chatMessages.isEmpty()) {
            WelcomeMessage(
                onTestFlanT5 = onTestFlanT5,
                onUpgradeClicked = onUpgradeClicked,
                selectedModel = selectedModel
            )
        }
        
        // Chat messages
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            reverseLayout = false
        ) {
            items(chatMessages) { message ->
                ChatMessageItem(message = message)
            }
            
            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Input area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = messageText,
                onValueChange = { messageText = it },
                placeholder = { Text("Describe your transaction or upload a receipt...") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (messageText.isNotBlank()) {
                            onSendMessage(messageText)
                            messageText = ""
                            keyboardController?.hide()
                        }
                    }
                ),
                maxLines = 3
            )
            
            // Image selection button
            IconButton(
                onClick = onSelectImage,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Select Image",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            // Send button
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSendMessage(messageText)
                        messageText = ""
                        keyboardController?.hide()
                    }
                },
                enabled = messageText.isNotBlank() && !isLoading,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WelcomeMessage(
    onTestFlanT5: ((String) -> Unit)? = null,
    onUpgradeClicked: (() -> Unit)? = null,
    selectedModel: com.solana.solmind.service.LanguageModel? = null
) {
    // Agent selection state - Transaction Parser is always selected since it's the only available agent
    var selectedAgent by remember { mutableStateOf("transaction_parser") }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Hi! I'm your SolMind Assistant",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "I can help you add transactions by analyzing your text descriptions or receipt images. Choose your agent mode below!",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            if (onTestFlanT5 != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "✨ Powered by ${selectedModel?.name ?: "FLAN-T5-small"} AI Model",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Select Agent Mode:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Agent Mode Selection
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Transaction Parser Agent
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                selectedAgent = "transaction_parser"
                                onTestFlanT5("Bought coffee for $4.50 at Starbucks")
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedAgent == "transaction_parser") 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (selectedAgent == "transaction_parser") 
                            null 
                        else 
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Analytics,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selectedAgent == "transaction_parser") 
                                        MaterialTheme.colorScheme.onPrimary 
                                    else 
                                        MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                if (selectedAgent == "transaction_parser") {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Selected",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Transaction Parser",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                fontWeight = if (selectedAgent == "transaction_parser") FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedAgent == "transaction_parser") 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Available",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedAgent == "transaction_parser") 
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) 
                                else 
                                    MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Finance Advisor Agent
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { 
                                onUpgradeClicked?.invoke()
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = "Coming Soon",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Finance Advisor",
                                style = MaterialTheme.typography.labelSmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "COMING SOON",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Currently using: ${if (selectedAgent == "transaction_parser") "Transaction Parser" else "None"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ChatMessageItem(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            Icon(
                Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    )
                    .padding(6.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) 
                    MaterialTheme.colorScheme.primary 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            )
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(12.dp),
                color = if (message.isUser) 
                    MaterialTheme.colorScheme.onPrimary 
                else 
                    MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (message.isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "You",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier
                .size(32.dp)
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    CircleShape
                )
                .padding(6.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                CircleShape
                            )
                    )
                    if (index < 2) Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
fun TransactionPreviewScreen(
    uiState: AddEntryUiState,
    isLoading: Boolean,
    currencyDisplayMode: CurrencyDisplayMode,
    accountMode: AccountMode,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Transaction Preview",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Please review and confirm the details",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        // Transaction details
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = CurrencyFormatter.formatAmount(uiState.amount.toDoubleOrNull() ?: 0.0, currencyDisplayMode, accountMode),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.transactionType == TransactionType.INCOME) 
                            Color(0xFF4CAF50) else Color(0xFFE53E3E)
                    )
                }
                
                Divider()
                
                // Description
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = uiState.description,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.End
                    )
                }
                
                Divider()
                
                // Type
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Type",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        onClick = { },
                        label = { Text(uiState.transactionType.name.lowercase().capitalize()) },
                        selected = true,
                        enabled = false
                    )
                }
                
                Divider()
                
                // Category
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.category.getEmoji(),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.category.getDisplayName(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Divider()
                
                // Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(uiState.date),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                
                // AI Confidence
                if (uiState.confidence > 0f) {
                    Divider()
                    Column {
                        Text(
                            text = "AI Confidence",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = uiState.confidence,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(uiState.confidence * 100).toInt()}% confident",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                 }
             }
         }
         
         // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEdit,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit")
            }
            
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionEditScreen(
    uiState: AddEntryUiState,
    isLoading: Boolean,
    currencyDisplayMode: CurrencyDisplayMode,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onCategoryChange: (TransactionCategory) -> Unit,
    onCustomCategoryInputChange: (String) -> Unit,
    onDateChange: (Date) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
    ) {
        LazyColumn(
             modifier = Modifier
                 .weight(1f)
                 .padding(16.dp),
             verticalArrangement = Arrangement.spacedBy(16.dp)
         ) {
            item {
                // Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Edit Transaction",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Modify the transaction details",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            
            item {
                // Form fields
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                // Amount field
                OutlinedTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount (${if (currencyDisplayMode == CurrencyDisplayMode.USD) "USD" else "SOL"})") },
                    leadingIcon = {
                        Icon(Icons.Default.AttachMoney, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    isError = uiState.amountError != null,
                    supportingText = uiState.amountError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Description field
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Next
                    ),
                    isError = uiState.descriptionError != null,
                    supportingText = uiState.descriptionError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Transaction Type
                Column {
                    Text(
                        text = "Transaction Type",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TransactionType.values().forEach { type ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { onTypeChange(type) }
                                    .weight(1f)
                            ) {
                                RadioButton(
                                    selected = uiState.transactionType == type,
                                    onClick = { onTypeChange(type) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = type.name.lowercase().capitalize(),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                
                // Category selection with AI suggestions
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Box {
                        OutlinedTextField(
                            value = uiState.customCategoryInput.ifEmpty { "${uiState.category.getEmoji()} ${uiState.category.getDisplayName()}" },
                            onValueChange = { input ->
                                onCustomCategoryInputChange(input)
                                showCategoryDropdown = input.isNotEmpty()
                            },
                            label = { Text("Type to search or select category") },
                            trailingIcon = {
                                Row {
                                    if (uiState.customCategoryInput.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                onCustomCategoryInputChange("")
                                                showCategoryDropdown = false
                                            }
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear")
                                        }
                                    }
                                    Icon(
                                        if (showCategoryDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.clickable {
                                            showCategoryDropdown = !showCategoryDropdown
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done
                            )
                        )
                        
                        DropdownMenu(
                            expanded = showCategoryDropdown,
                            onDismissRequest = { showCategoryDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Show AI suggested categories first if available
                            if (uiState.suggestedCategories.isNotEmpty() && uiState.customCategoryInput.isNotEmpty()) {
                                Text(
                                    text = "🤖 AI Suggestions",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                                
                                uiState.suggestedCategories.forEach { category ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "✨",
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = category.getEmoji(),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = category.getDisplayName(),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        },
                                        onClick = {
                                            onCategoryChange(category)
                                            onCustomCategoryInputChange("")
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                                
                                Divider(modifier = Modifier.padding(vertical = 4.dp))
                            }
                            
                            // Show all categories
                            if (uiState.customCategoryInput.isEmpty()) {
                                Text(
                                    text = "All Categories",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            TransactionCategory.values().forEach { category ->
                                // Filter categories based on input if there's custom input
                                val shouldShow = uiState.customCategoryInput.isEmpty() || 
                                    category.getDisplayName().contains(uiState.customCategoryInput, ignoreCase = true) ||
                                    uiState.suggestedCategories.contains(category)
                                
                                if (shouldShow) {
                                    DropdownMenuItem(
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = category.getEmoji(),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = category.getDisplayName(),
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        },
                                        onClick = {
                                            onCategoryChange(category)
                                            onCustomCategoryInputChange("")
                                            showCategoryDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Date selection
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    OutlinedTextField(
                        value = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(uiState.date),
                        onValueChange = { },
                        readOnly = true,
                        leadingIcon = {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    )
                }
                
                // AI Confidence display (if available)
                if (uiState.confidence > 0f) {
                    Column {
                        Text(
                            text = "AI Confidence",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = uiState.confidence,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(uiState.confidence * 100).toInt()}% confident",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                    }
                }
            }
        }
         
         // Action buttons
         Row(
             modifier = Modifier
                 .fillMaxWidth()
                 .padding(16.dp),
             horizontalArrangement = Arrangement.spacedBy(12.dp)
         ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cancel")
            }
            
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                enabled = !isLoading && uiState.isValid
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = null)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Changes")
            }
        }
    }
    
    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.date.time
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChange(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ChatMessage is now defined in AddEntryViewModel.kt