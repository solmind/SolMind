package com.solana.solmind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.data.model.TransactionCategory
import com.solana.solmind.data.model.TransactionType
import com.solana.solmind.repository.LedgerRepository
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
    val date: Date = Date(),
    val confidence: Float = 0f,
    val amountError: String? = null,
    val descriptionError: String? = null,
    val error: String? = null,
    val isSaved: Boolean = false,
    val chatMessages: List<ChatMessage> = emptyList(),
    val showPreview: Boolean = false,
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
    private val repository: LedgerRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddEntryUiState())
    val uiState: StateFlow<AddEntryUiState> = _uiState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
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
                val suggestion = repository.createEntryFromText(currentState.description)
                
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
                    confidence = currentState.confidence
                )
                
                repository.insertEntry(entry)
                _uiState.value = currentState.copy(isSaved = true)
                
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
                // Simulate AI processing
                kotlinx.coroutines.delay(1500)
                
                // Use existing AI service to analyze the message
                val suggestion = repository.createEntryFromText(message)
                
                val aiResponse = if (suggestion != null) {
                    // Update the transaction data
                    _uiState.value = _uiState.value.copy(
                        amount = suggestion.amount.toString(),
                        description = suggestion.description,
                        transactionType = suggestion.type,
                        category = suggestion.category,
                        confidence = suggestion.confidence
                    )
                    
                    "I've analyzed your transaction! I found:\n\n" +
                    "💰 Amount: $${suggestion.amount}\n" +
                    "📝 Description: ${suggestion.description}\n" +
                    "📊 Type: ${suggestion.type.name.lowercase().capitalize()}\n" +
                    "🏷️ Category: ${suggestion.category.getDisplayName()}\n\n" +
                    "Would you like to review and confirm this transaction?"
                } else {
                    "I couldn't extract transaction details from your message. Could you please provide more specific information like the amount and what the transaction was for?"
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
                    confidence = 0.85f
                )
                
                _uiState.value = _uiState.value.copy(
                    amount = mockTransaction.amount.toString(),
                    description = mockTransaction.description,
                    transactionType = mockTransaction.type,
                    category = mockTransaction.category,
                    confidence = mockTransaction.confidence
                )
                
                val aiResponse = "I've analyzed your receipt! I found:\n\n" +
                "💰 Amount: $${mockTransaction.amount}\n" +
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
            showPreview = false
        )
        
        val assistantMessage = ChatMessage(
            content = "Sure! What would you like to change about this transaction?",
            isUser = false
        )
        val currentState = _uiState.value
        
        _uiState.value = currentState.copy(
            chatMessages = currentState.chatMessages + assistantMessage
        )
    }
    
    fun resetChat() {
        _uiState.value = AddEntryUiState()
    }
}