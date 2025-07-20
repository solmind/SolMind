package com.solana.ailedger.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solana.ailedger.data.model.SolanaWallet
import com.solana.ailedger.data.repository.LedgerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WalletViewModel @Inject constructor(
    private val repository: LedgerRepository
) : ViewModel() {
    
    private val _wallets = MutableStateFlow<List<SolanaWallet>>(emptyList())
    val wallets: StateFlow<List<SolanaWallet>> = _wallets.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
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
                
                val wallet = SolanaWallet(
                    address = address.trim(),
                    name = name.trim(),
                    status = "pending",
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                repository.addWallet(wallet)
                
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
                
                repository.syncSolanaTransactions(address)
                
                // Update wallet status and sync time
                repository.updateWalletSyncTime(address, Date())
                repository.updateWalletStatus(address, "active")
                
            } catch (e: Exception) {
                _error.value = "Failed to sync wallet: ${e.message}"
                repository.updateWalletStatus(address, "error")
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
                        repository.syncSolanaTransactions(wallet.address)
                        repository.updateWalletSyncTime(wallet.address, Date())
                        repository.updateWalletStatus(wallet.address, "active")
                    } catch (e: Exception) {
                        repository.updateWalletStatus(wallet.address, "error")
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
                
                repository.deleteWallet(address)
                
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