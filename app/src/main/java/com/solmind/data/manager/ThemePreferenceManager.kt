package com.solmind.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.preferencesDataStore
import com.solmind.data.model.AccountMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_preferences")

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

@Singleton
class ThemePreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val onchainThemeKey = stringPreferencesKey("onchain_theme")
    private val offchainThemeKey = stringPreferencesKey("offchain_theme")
    
    val onchainThemeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences: Preferences ->
            val themeName = preferences[onchainThemeKey] ?: ThemeMode.LIGHT.name
            ThemeMode.valueOf(themeName)
        }
    
    val offchainThemeMode: Flow<ThemeMode> = context.themeDataStore.data
        .map { preferences: Preferences ->
            val themeName = preferences[offchainThemeKey] ?: ThemeMode.DARK.name
            ThemeMode.valueOf(themeName)
        }
    
    suspend fun setOnchainTheme(themeMode: ThemeMode) {
        context.themeDataStore.edit { preferences: MutablePreferences ->
            preferences[onchainThemeKey] = themeMode.name
        }
    }
    
    suspend fun setOffchainTheme(themeMode: ThemeMode) {
        context.themeDataStore.edit { preferences: MutablePreferences ->
            preferences[offchainThemeKey] = themeMode.name
        }
    }
    
    fun getThemeModeForAccountMode(accountMode: AccountMode): Flow<ThemeMode> {
        return when (accountMode) {
            AccountMode.ONCHAIN -> onchainThemeMode
            AccountMode.OFFCHAIN -> offchainThemeMode
        }
    }
}