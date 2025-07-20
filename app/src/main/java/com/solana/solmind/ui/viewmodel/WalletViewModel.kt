package com.solana.solmind.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.data.model.SolanaWallet
import com.solana.solmind.data.preferences.AccountModeManager
import com.solana.solmind.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: LedgerRepository,
    private val accountModeManager: AccountModeManager
) : ViewModel() {
    
    private val _wallets = MutableStateFlow<List<SolanaWallet>>(emptyList())
    val wallets: StateFlow<List<SolanaWallet>> = _wallets.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    val currentAccountMode = accountModeManager.currentAccountMode
    
    fun loadWallets() {
        viewModelScope.launch {
            try {
                _error.value = null
                repository.getAllWallets().collect { walletList ->
                    _wallets.value = walletList
                }
            } catch (e: Exception) {
                _error.value = "Failed to load wallets: ${e.message}"
            }
        }
    }
    
    fun addWallet(address: String, name: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Validate address format (basic validation)
                if (!isValidSolanaAddress(address)) {
                    _error.value = "Invalid Solana address format"
                    return@launch
                }
                
                // Check if wallet already exists
                val existingWallet = repository.getWalletByAddress(address)
                if (existingWallet != null) {
                    _error.value = "Wallet with this address already exists"
                    return@launch
                }
                
                val success = repository.addWallet(address.trim(), name.trim())
                if (!success) {
                    _error.value = "Failed to add wallet - invalid address or network error"
                    return@launch
                }
                
                // Start initial sync
                syncWallet(address)
                
            } catch (e: Exception) {
                _error.value = "Failed to add wallet: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun syncWallet(address: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                repository.syncWalletTransactions(address)
                
                // Update wallet sync time
                repository.updateWalletSyncTime(address, Date())
                
            } catch (e: Exception) {
                _error.value = "Failed to sync wallet: ${e.message}"
                // Error handling - could update wallet status if needed
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun syncAllWallets() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val currentWallets = _wallets.value
                
                currentWallets.forEach { wallet ->
                    try {
                        repository.syncWalletTransactions(wallet.address)
                        repository.updateWalletSyncTime(wallet.address, Date())
                    } catch (e: Exception) {
                        // Error handling - could update wallet status if needed
                    }
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to sync wallets: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun deleteWallet(address: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val wallet = repository.getWalletByAddress(address)
                if (wallet != null) {
                    repository.deleteWallet(wallet)
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to delete wallet: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    private fun isValidSolanaAddress(address: String): Boolean {
        // Basic Solana address validation
        // Solana addresses are base58 encoded and typically 32-44 characters
        val trimmedAddress = address.trim()
        
        return when {
            trimmedAddress.length < 32 -> false
            trimmedAddress.length > 44 -> false
            !trimmedAddress.matches(Regex("^[1-9A-HJ-NP-Za-km-z]+$")) -> false // Base58 characters
            else -> true
        }
    }
    
    fun refreshWallets() {
        loadWallets()
    }
}