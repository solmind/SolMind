package com.solana.solmind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.data.model.LedgerEntry
import com.solana.solmind.data.model.TransactionType
import com.solana.solmind.data.preferences.AccountModeManager
import com.solana.solmind.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val accountModeManager: AccountModeManager,
    private val syncManager: com.solana.solmind.manager.SyncManager
) : ViewModel() {
    
    val currentAccountMode = accountModeManager.currentAccountMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AccountMode.OFFCHAIN
        )
    
    val entries = currentAccountMode
        .flatMapLatest { accountMode ->
            repository.getEntriesByAccountMode(accountMode)
        }
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
    
    val totalIncome = currentAccountMode
        .flatMapLatest { accountMode ->
            flow {
                emit(
                    repository.getTotalAmountByTypeAndDateRangeAndAccountMode(
                        TransactionType.INCOME,
                        currentMonth,
                        nextMonth,
                        accountMode
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )
    
    val totalExpenses = currentAccountMode
        .flatMapLatest { accountMode ->
            flow {
                emit(
                    repository.getTotalAmountByTypeAndDateRangeAndAccountMode(
                        TransactionType.EXPENSE,
                        currentMonth,
                        nextMonth,
                        accountMode
                    )
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )
    
    fun setAccountMode(accountMode: AccountMode) {
        val previousMode = accountModeManager.getCurrentAccountMode()
        accountModeManager.setAccountMode(accountMode)
        
        // Trigger immediate sync when switching to on-chain mode
        if (accountMode == AccountMode.ONCHAIN && previousMode == AccountMode.OFFCHAIN) {
            syncManager.triggerImmediateSync()
        }
    }
    
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
                    } catch (e: IllegalStateException) {
                        // Handle model availability issues - continue with other wallets
                        if (e.message?.contains("No model selected") == true || 
                            e.message?.contains("not downloaded") == true) {
                            // Log but continue - sync will work without AI categorization
                        } else {
                            // Handle other IllegalStateExceptions
                        }
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