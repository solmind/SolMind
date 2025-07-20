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
    val isSaved: Boolean = false
) {
    val isValid: Boolean
        get() = amount.isNotEmpty() && 
                description.isNotEmpty() && 
                amountError == null && 
                descriptionError == null &&
                amount.toDoubleOrNull() != null &&
                amount.toDoubleOrNull()!! > 0
}

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
}