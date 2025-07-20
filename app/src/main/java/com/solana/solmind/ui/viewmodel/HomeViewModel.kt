package com.solana.solmind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.data.model.TransactionType
import com.solana.solmind.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {
    
    val entries = repository.getAllEntries()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val currentMonth = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.time
    
    private val nextMonth = Calendar.getInstance().apply {
        time = currentMonth
        add(Calendar.MONTH, 1)
    }.time
    
    val totalIncome = flow {
        emit(
            repository.getTotalAmountByTypeAndDateRange(
                TransactionType.INCOME,
                currentMonth,
                nextMonth
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
    
    val totalExpenses = flow {
        emit(
            repository.getTotalAmountByTypeAndDateRange(
                TransactionType.EXPENSE,
                currentMonth,
                nextMonth
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0.0
    )
    
    fun refreshData() {
        viewModelScope.launch {
            // Trigger refresh of totals
            // The flows will automatically update when repository data changes
        }
    }
    
    fun syncAllWallets() {
        viewModelScope.launch {
            try {
                val wallets = repository.getActiveWallets().first()
                wallets.forEach { wallet ->
                    try {
                        repository.syncWalletTransactions(wallet.address)
                    } catch (e: Exception) {
                        // Handle individual wallet sync errors
                        // Could emit to a UI state for error handling
                    }
                }
            } catch (e: Exception) {
                // Handle general sync error
            }
        }
    }
}