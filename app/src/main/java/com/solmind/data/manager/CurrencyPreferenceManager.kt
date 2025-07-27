package com.solmind.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

val Context.currencyDataStore: DataStore<Preferences> by preferencesDataStore(name = "currency_preferences")

enum class CurrencyDisplayMode {
    USD, SOL
}

@Singleton
class CurrencyPreferenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val currencyDisplayKey = stringPreferencesKey("currency_display_mode")
    
    val currencyDisplayMode: Flow<CurrencyDisplayMode> = context.currencyDataStore.data
        .map { preferences: Preferences ->
            val modeName = preferences[currencyDisplayKey] ?: CurrencyDisplayMode.SOL.name
            CurrencyDisplayMode.valueOf(modeName)
        }
    
    suspend fun setCurrencyDisplayMode(mode: CurrencyDisplayMode) {
        context.currencyDataStore.edit { preferences: MutablePreferences ->
            preferences[currencyDisplayKey] = mode.name
        }
    }
    
    suspend fun getCurrencyDisplayMode(): CurrencyDisplayMode {
        return currencyDisplayMode.first()
    }
}