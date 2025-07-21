package com.solana.solmind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.data.preferences.AccountModeManager
import com.solana.solmind.data.manager.ThemePreferenceManager
import com.solana.solmind.ui.navigation.SolMindNavigation
import com.solana.solmind.ui.theme.SolMindTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var accountModeManager: AccountModeManager
    
    @Inject
    lateinit var themePreferenceManager: ThemePreferenceManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val currentAccountMode by accountModeManager.currentAccountMode
                .stateIn(
                    scope = lifecycleScope,
                    started = SharingStarted.WhileSubscribed(5000),
                    initialValue = AccountMode.OFFCHAIN
                )
                .collectAsState()
            
            SolMindTheme(
                accountMode = currentAccountMode,
                themePreferenceManager = themePreferenceManager
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SolMindNavigation()
                }
            }
        }
    }
}