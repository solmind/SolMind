package com.solana.solmind.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.solana.solmind.data.model.AccountMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountModeManager @Inject constructor(
    private val context: Context
) {
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("account_mode_prefs", Context.MODE_PRIVATE)
    
    private val _currentAccountMode = MutableStateFlow(
        AccountMode.valueOf(
            sharedPreferences.getString(KEY_ACCOUNT_MODE, AccountMode.OFFCHAIN.name) 
                ?: AccountMode.OFFCHAIN.name
        )
    )
    val currentAccountMode: StateFlow<AccountMode> = _currentAccountMode.asStateFlow()
    
    fun setAccountMode(accountMode: AccountMode) {
        _currentAccountMode.value = accountMode
        sharedPreferences.edit()
            .putString(KEY_ACCOUNT_MODE, accountMode.name)
            .apply()
    }
    
    fun getCurrentAccountMode(): AccountMode {
        return _currentAccountMode.value
    }
    
    companion object {
        private const val KEY_ACCOUNT_MODE = "current_account_mode"
    }
}