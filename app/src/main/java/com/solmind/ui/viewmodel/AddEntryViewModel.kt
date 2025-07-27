package com.solmind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solmind.data.model.AccountMode
import com.solmind.data.model.LedgerEntry
import com.solmind.data.model.TransactionCategory
import com.solmind.data.model.TransactionType
import com.solmind.data.preferences.AccountModeManager
import com.solmind.data.manager.CurrencyPreferenceManager
import com.solmind.data.manager.CurrencyDisplayMode
import com.solmind.repository.LedgerRepository
import com.solmind.utils.CurrencyFormatter
import com.solmind.service.ModelManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AddEntryUiState(
    val amount: String = "",
    val description: String = "",
    val transactionType: TransactionType = TransactionType.EXPENSE,
    val category: TransactionCategory = TransactionCategory.OTHER,
    val customCategoryInput: String = "",
    val suggestedCategories: List<TransactionCategory> = emptyList(),
    val date: Date = Date(),
    val confidence: Float = 0f,
    val amountError: String? = null,
    val descriptionError: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val showPreview: Boolean = false,
    val showEditForm: Boolean = false,
    val isProcessingMessage: Boolean = false
) {
    val isValid: Boolean
        get() = amount.isNotEmpty() && 
                description.isNotEmpty() && 
                amountError == null && 
                descriptionError == null &&
                amount.toDoubleOrNull() != null &&
                amount.toDoubleOrNull()!! > 0
}

data class ChatMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@HiltViewModel
class AddEntryViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val accountModeManager: AccountModeManager,
    private val currencyPreferenceManager: CurrencyPreferenceManager,
    private val aiService: com.solmind.service.AIService,
    private val chatbotService: com.solmind.service.ChatbotService,
    private val modelManager: ModelManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _shouldShowModelSelection = MutableStateFlow(false)
    val shouldShowModelSelection: StateFlow<Boolean> = _shouldShowModelSelection.asStateFlow()
    
    val currentAccountMode = accountModeManager.currentAccountMode
    val currencyDisplayMode = currencyPreferenceManager.currencyDisplayMode
    
    fun updateAmount(amount: String) {
        val error = validateAmount(amount)
        _uiState.value = _uiState.value.copy(
            amount = amount,
            amountError = error
        )
    }
    
    fun updateDescription(description: String) {
        val error = validateDescription(description)
        _uiState.value = _uiState.value.copy(
            description = description,
            descriptionError = error
        )
    }
    
    fun updateTransactionType(type: TransactionType) {
        _uiState.value = _uiState.value.copy(transactionType = type)
    }
    
    fun updateCategory(category: TransactionCategory) {
        _uiState.value = _uiState.value.copy(category = category)
    }
    
    fun updateCustomCategoryInput(input: String) {
        _uiState.value = _uiState.value.copy(customCategoryInput = input)
        
        // Get AI suggestions for categories based on input
        if (input.isNotEmpty()) {
            getCategorySuggestions(input)
        } else {
            _uiState.value = _uiState.value.copy(suggestedCategories = emptyList())
        }
    }
    
    /**
     * Get AI-powered category suggestions based on user input
     * Uses the same AI service as the chatbox for consistent suggestions
     */
    private fun getCategorySuggestions(input: String) {
        viewModelScope.launch {
            try {
                // Use AI service to categorize the input
                val prediction = aiService.categorizeTransaction(input)
                
                // Create a list of suggested categories based on AI analysis
                val suggestions = mutableListOf<TransactionCategory>()
                
                // Add the primary AI suggestion
                suggestions.add(prediction.category)
                
                // Add related categories based on keywords
                val relatedCategories = getRelatedCategories(input, prediction.category)
                suggestions.addAll(relatedCategories)
                
                // Remove duplicates and limit to 5 suggestions
                val uniqueSuggestions = suggestions.distinct().take(5)
                
                _uiState.value = _uiState.value.copy(suggestedCategories = uniqueSuggestions)
                
            } catch (e: Exception) {
                // Fallback to keyword-based suggestions
                val fallbackSuggestions = getKeywordBasedSuggestions(input)
                _uiState.value = _uiState.value.copy(suggestedCategories = fallbackSuggestions)
            }
        }
    }
    
    /**
     * Get related categories based on the primary suggestion and input text
     */
    private fun getRelatedCategories(input: String, primaryCategory: TransactionCategory): List<TransactionCategory> {
        val normalizedInput = input.lowercase().trim()
        val related = mutableListOf<TransactionCategory>()
        
        // Add categories that might be related to the input
        when (primaryCategory) {
            TransactionCategory.FOOD_DINING -> {
                if (normalizedInput.contains("grocery") || normalizedInput.contains("supermarket")) {
                    related.add(TransactionCategory.SHOPPING)
                }
            }
            TransactionCategory.SHOPPING -> {
                if (normalizedInput.contains("food") || normalizedInput.contains("grocery")) {
                    related.add(TransactionCategory.FOOD_DINING)
                }
                if (normalizedInput.contains("clothes") || normalizedInput.contains("fashion")) {
                    related.add(TransactionCategory.OTHER)
                }
            }
            TransactionCategory.TRANSPORTATION -> {
                if (normalizedInput.contains("travel") || normalizedInput.contains("trip")) {
                    related.add(TransactionCategory.TRAVEL)
                }
            }
            TransactionCategory.ENTERTAINMENT -> {
                if (normalizedInput.contains("subscription") || normalizedInput.contains("streaming")) {
                    related.add(TransactionCategory.UTILITIES)
                }
            }
            else -> {
                // Add some common alternatives
                related.addAll(listOf(
                    TransactionCategory.OTHER,
                    TransactionCategory.SHOPPING
                ))
            }
        }
        
        return related.distinct()
    }
    
    /**
     * Fallback keyword-based category suggestions
     */
    private fun getKeywordBasedSuggestions(input: String): List<TransactionCategory> {
        val normalizedInput = input.lowercase().trim()
        val suggestions = mutableListOf<TransactionCategory>()
        
        // Food keywords
        if (normalizedInput.contains("food") || normalizedInput.contains("restaurant") || 
            normalizedInput.contains("coffee") || normalizedInput.contains("lunch")) {
            suggestions.add(TransactionCategory.FOOD_DINING)
        }
        
        // Transportation keywords
        if (normalizedInput.contains("gas") || normalizedInput.contains("uber") || 
            normalizedInput.contains("taxi") || normalizedInput.contains("bus")) {
            suggestions.add(TransactionCategory.TRANSPORTATION)
        }
        
        // Shopping keywords
        if (normalizedInput.contains("buy") || normalizedInput.contains("purchase") || 
            normalizedInput.contains("store") || normalizedInput.contains("shopping")) {
            suggestions.add(TransactionCategory.SHOPPING)
        }
        
        // Entertainment keywords
        if (normalizedInput.contains("movie") || normalizedInput.contains("game") || 
            normalizedInput.contains("entertainment") || normalizedInput.contains("netflix")) {
            suggestions.add(TransactionCategory.ENTERTAINMENT)
        }
        
        // Utilities keywords
        if (normalizedInput.contains("bill") || normalizedInput.contains("electric") || 
            normalizedInput.contains("internet") || normalizedInput.contains("phone")) {
            suggestions.add(TransactionCategory.UTILITIES)
        }
        
        // Healthcare keywords
        if (normalizedInput.contains("doctor") || normalizedInput.contains("medicine") || 
            normalizedInput.contains("hospital") || normalizedInput.contains("pharmacy")) {
            suggestions.add(TransactionCategory.HEALTHCARE)
        }
        
        // Add OTHER as fallback
        if (suggestions.isEmpty()) {
            suggestions.add(TransactionCategory.OTHER)
        }
        
        return suggestions.distinct().take(5)
    }
    
    fun updateDate(date: Date) {
        _uiState.value = _uiState.value.copy(date = date)
    }
    
    fun showDatePicker() {
        // This would typically trigger a date picker dialog
        // For now, we'll just set it to current date
        updateDate(Date())
    }
    
    fun getAISuggestion() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiState.value = _uiState.value.copy(error = null)
                
                val currentState = _uiState.value
                val suggestion = repository.createEntryFromText(
                    currentState.description, 
                    accountMode = accountModeManager.getCurrentAccountMode()
                )
                
                suggestion?.let { entry ->
                    _uiState.value = currentState.copy(
                        transactionType = entry.type,
                        category = entry.category,
                        confidence = entry.confidence
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to get AI suggestion: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Parse transaction text using FLAN-T5-small model
     * Uses the prompt: "You are given a piece of text describing a ledger change: [CONTENT], 
     * please choose appropriate category of: spend or income;amount;the category among one of 
     * [FOOD_DINING,TRANSPORTATION,SHOPPING,ENTERTAINMENT,UTILITIES,HEALTHCARE,EDUCATION,TRAVEL,INVESTMENT,SALARY,FREELANCE,BUSINESS,GIFTS,OTHER], 
     * output with 3 pieces split by ;"
     */
    fun parseWithFlanT5(content: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiState.value = _uiState.value.copy(error = null)
                
                // Use the FLAN-T5-small model directly
                val parseResult = aiService.parseTransactionWithAI(content)
                
                _uiState.value = _uiState.value.copy(
                    amount = if (parseResult.amount > 0.0) parseResult.amount.toString() else _uiState.value.amount,
                    description = content,
                    transactionType = parseResult.type,
                    category = parseResult.category,
                    confidence = parseResult.confidence
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to parse with FLAN-T5: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun saveEntry() {
        val currentState = _uiState.value
        if (!currentState.isValid) {
            return
        }
        
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _uiState.value = currentState.copy(error = null)
                
                val amount = currentState.amount.toDouble()
                val entry = LedgerEntry(
                    amount = amount,
                    description = currentState.description,
                    category = currentState.category,
                    type = currentState.transactionType,
                    date = currentState.date,
                    accountMode = accountModeManager.getCurrentAccountMode(),
                    confidence = currentState.confidence
                )
                
                repository.insertEntry(entry)
                _uiState.value = currentState.copy(
                    isSaved = true,
                    showPreview = false,
                    showEditForm = false
                )
                
            } catch (e: Exception) {
                _uiState.value = currentState.copy(
                    error = "Failed to save entry: ${e.message}"
                )
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun validateAmount(amount: String): String? {
        return when {
            amount.isEmpty() -> "Amount is required"
            amount.toDoubleOrNull() == null -> "Invalid amount format"
            amount.toDoubleOrNull()!! <= 0 -> "Amount must be greater than 0"
            else -> null
        }
    }
    
    private fun validateDescription(description: String): String? {
        return when {
            description.isEmpty() -> "Description is required"
            description.length < 3 -> "Description must be at least 3 characters"
            description.length > 200 -> "Description must be less than 200 characters"
            else -> null
        }
    }
    
    fun resetForm() {
        _uiState.value = AddEntryUiState()
    }
    
    fun sendMessage(message: String) {
        // Check if a local model is available and downloaded
        val selectedModel = modelManager.selectedModel.value
        if (selectedModel == null || !selectedModel.isLocal || !modelManager.isModelDownloaded(selectedModel.id)) {
            _shouldShowModelSelection.value = true
            return
        }
        
        val currentState = _uiState.value
        val userMessage = ChatMessage(content = message, isUser = true)
        
        // Add user message to chat
        _uiState.value = currentState.copy(
            chatMessages = currentState.chatMessages + userMessage,
            isProcessingMessage = true
        )
        
        // Process the message with AI
        processUserMessage(message)
    }
    
    fun selectImage() {
        // Handle image selection - for now, add a placeholder message
        val currentState = _uiState.value
        val userMessage = ChatMessage(content = "[Image uploaded]", isUser = true)
        
        _uiState.value = currentState.copy(
            chatMessages = currentState.chatMessages + userMessage,
            isProcessingMessage = true
        )
        
        // Process image (placeholder for now)
        processImageMessage()
    }
    
    private fun processUserMessage(message: String) {
        viewModelScope.launch {
            try {
                // First, try to extract transaction data
                val suggestion = repository.createEntryFromText(
                    message, 
                    accountMode = accountModeManager.getCurrentAccountMode()
                )
                
                // Generate response based on transaction detection
                val aiResponse = if (suggestion != null) {
                    // Update the transaction data
                    _uiState.value = _uiState.value.copy(
                        amount = suggestion.amount.toString(),
                        description = suggestion.description,
                        transactionType = suggestion.type,
                        category = suggestion.category,
                        date = suggestion.date,
                        confidence = suggestion.confidence
                    )
                    
                    // Reply with the transaction description instead of AI output
                    suggestion.description
                } else {
                    // Generate conversational response using the chatbot
                    chatbotService.generateResponse(message)
                }
                
                val assistantMessage = ChatMessage(content = aiResponse, isUser = false)
                val currentState = _uiState.value
                
                _uiState.value = currentState.copy(
                    chatMessages = currentState.chatMessages + assistantMessage,
                    isProcessingMessage = false,
                    showPreview = suggestion != null
                )
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "Sorry, I encountered an error processing your message. Please try again.",
                    isUser = false
                )
                val currentState = _uiState.value
                
                _uiState.value = currentState.copy(
                    chatMessages = currentState.chatMessages + errorMessage,
                    isProcessingMessage = false
                )
            }
        }
    }
    
    private fun processImageMessage() {
        viewModelScope.launch {
            try {
                // Simulate image processing
                kotlinx.coroutines.delay(2000)
                
                // For now, create a mock transaction from image
                val mockTransaction = LedgerEntry(
                    amount = 25.99,
                    description = "Coffee and pastry",
                    category = TransactionCategory.FOOD_DINING,
                    type = TransactionType.EXPENSE,
                    date = Date(),
                    accountMode = accountModeManager.getCurrentAccountMode(),
                    confidence = 0.85f
                )
                
                _uiState.value = _uiState.value.copy(
                    amount = mockTransaction.amount.toString(),
                    description = mockTransaction.description,
                    transactionType = mockTransaction.type,
                    category = mockTransaction.category,
                    confidence = mockTransaction.confidence
                )
                
                val currentCurrencyMode = currencyPreferenceManager.getCurrencyDisplayMode()
                val currentAccountMode = accountModeManager.getCurrentAccountMode()
                val aiResponse = "I've analyzed your receipt! I found:\n\n" +
                "💰 Amount: ${CurrencyFormatter.formatAmount(mockTransaction.amount, currentCurrencyMode, currentAccountMode)}\n" +
                "📝 Description: ${mockTransaction.description}\n" +
                "📊 Type: ${mockTransaction.type.name.lowercase().capitalize()}\n" +
                "🏷️ Category: ${mockTransaction.category.getDisplayName()}\n\n" +
                "Confidence: ${(mockTransaction.confidence * 100).toInt()}%\n\n" +
                "Would you like to review and confirm this transaction?"
                
                val assistantMessage = ChatMessage(content = aiResponse, isUser = false)
                val currentState = _uiState.value
                
                _uiState.value = currentState.copy(
                    chatMessages = currentState.chatMessages + assistantMessage,
                    isProcessingMessage = false,
                    showPreview = true
                )
                
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    content = "Sorry, I couldn't process the image. Please try again or describe the transaction manually.",
                    isUser = false
                )
                val currentState = _uiState.value
                
                _uiState.value = currentState.copy(
                    chatMessages = currentState.chatMessages + errorMessage,
                    isProcessingMessage = false
                )
            }
        }
    }
    
    fun confirmTransaction() {
        saveEntry()
    }
    
    fun editTransaction() {
        _uiState.value = _uiState.value.copy(
            showPreview = false,
            showEditForm = true
        )
    }
    
    fun cancelEdit() {
        _uiState.value = _uiState.value.copy(
            showEditForm = false,
            showPreview = true
        )
    }
    
    fun saveEditedTransaction() {
        val currentState = _uiState.value
        if (!currentState.isValid) {
            return
        }
        
        // Directly save the transaction without showing preview again
        saveEntry()
        
        // Reset to chat interface after saving
        _uiState.value = currentState.copy(
            showEditForm = false,
            showPreview = false
        )
    }
    
    fun resetChat() {
        _uiState.value = AddEntryUiState()
    }
    
    fun dismissModelSelection() {
        _shouldShowModelSelection.value = false
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up the chatbot service resources
        chatbotService.clearModel()
    }
}