package com.solmind.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.solmind.data.manager.ThemeMode
import com.solmind.data.manager.ThemePreferenceManager
import com.solmind.data.model.AccountMode

// Onchain Mode Color Scheme (Light Theme)
private val OnchainColorScheme = lightColorScheme(
    primary = OnchainPrimary,
    secondary = OnchainSecondary,
    tertiary = OnchainTertiary,
    background = OnchainBackground,
    surface = OnchainSurface,
    surfaceVariant = OnchainSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = OnchainOnBackground,
    onSurface = OnchainOnSurface
)

// Offchain Mode Color Scheme (Dark Theme)
private val OffchainColorScheme = darkColorScheme(
    primary = OffchainPrimary,
    secondary = OffchainSecondary,
    tertiary = OffchainTertiary,
    background = OffchainBackground,
    surface = OffchainSurface,
    surfaceVariant = OffchainSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = OffchainOnBackground,
    onSurface = OffchainOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = SolanaDark,
    surface = SolanaDarkSurface,
    onPrimary = Purple20,
    onSecondary = PurpleGrey20,
    onTertiary = Pink20,
    onBackground = SolanaLight,
    onSurface = SolanaLight
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = SolanaLight,
    surface = SolanaLightSurface,
    onPrimary = SolanaLight,
    onSecondary = SolanaLight,
    onTertiary = SolanaLight,
    onBackground = SolanaDark,
    onSurface = SolanaDark
)

@Composable
fun SolMindTheme(
    accountMode: AccountMode = AccountMode.OFFCHAIN,
    themePreferenceManager: ThemePreferenceManager? = null,
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    
    // Get user's theme preference for current account mode
    val userThemeMode by (themePreferenceManager?.getThemeModeForAccountMode(accountMode)
        ?.collectAsState(initial = when (accountMode) {
            AccountMode.ONCHAIN -> ThemeMode.LIGHT
            AccountMode.OFFCHAIN -> ThemeMode.DARK
        }) ?: androidx.compose.runtime.remember { 
            androidx.compose.runtime.mutableStateOf(
                when (accountMode) {
                    AccountMode.ONCHAIN -> ThemeMode.LIGHT
                    AccountMode.OFFCHAIN -> ThemeMode.DARK
                }
            ) 
        })
    
    // Determine if dark theme should be used based on user preference
    val shouldUseDarkTheme = when (userThemeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme
    }
    
    val baseColorScheme = when {
        shouldUseDarkTheme -> OffchainColorScheme
        else -> OnchainColorScheme
    }
    
    // Animate color transitions
    val animationSpec = tween<Color>(durationMillis = 600)
    
    val animatedPrimary by animateColorAsState(
        targetValue = baseColorScheme.primary,
        animationSpec = animationSpec,
        label = "primary_color"
    )
    val animatedBackground by animateColorAsState(
        targetValue = baseColorScheme.background,
        animationSpec = animationSpec,
        label = "background_color"
    )
    val animatedSurface by animateColorAsState(
        targetValue = baseColorScheme.surface,
        animationSpec = animationSpec,
        label = "surface_color"
    )
    val animatedOnBackground by animateColorAsState(
        targetValue = baseColorScheme.onBackground,
        animationSpec = animationSpec,
        label = "on_background_color"
    )
    val animatedOnSurface by animateColorAsState(
        targetValue = baseColorScheme.onSurface,
        animationSpec = animationSpec,
        label = "on_surface_color"
    )
    val animatedSurfaceVariant by animateColorAsState(
        targetValue = baseColorScheme.surfaceVariant,
        animationSpec = animationSpec,
        label = "surface_variant_color"
    )
    
    val animatedColorScheme = baseColorScheme.copy(
        primary = animatedPrimary,
        background = animatedBackground,
        surface = animatedSurface,
        onBackground = animatedOnBackground,
        onSurface = animatedOnSurface,
        surfaceVariant = animatedSurfaceVariant
    )
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = animatedColorScheme.primary.toArgb()
            val isLight = !shouldUseDarkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = isLight
        }
    }

    MaterialTheme(
        colorScheme = animatedColorScheme,
        typography = Typography,
        content = content
    )
}