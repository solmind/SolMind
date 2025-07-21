package com.solana.solmind.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solana.solmind.data.model.AccountMode
import com.solana.solmind.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun AnimatedModeSwitcher(
    currentMode: AccountMode,
    onModeChange: (AccountMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var isTransitioning by remember { mutableStateOf(false) }
    var showTransitionOverlay by remember { mutableStateOf(false) }
    
    // Movie-like transition effect
    LaunchedEffect(currentMode) {
        if (isTransitioning) {
            showTransitionOverlay = true
            delay(400) // Half of the transition duration
            showTransitionOverlay = false
            isTransitioning = false
        }
    }
    
    Box(modifier = modifier) {
        // Main switcher
        ModeSwitcherButton(
            currentMode = currentMode,
            onClick = { newMode ->
                if (newMode != currentMode) {
                    isTransitioning = true
                    onModeChange(newMode)
                }
            }
        )
        
        // Movie-like transition overlay
        AnimatedVisibility(
            visible = showTransitionOverlay,
            enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = tween(200)
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 1.2f,
                animationSpec = tween(200)
            )
        ) {
            TransitionOverlay(targetMode = currentMode)
        }
    }
}

@Composable
private fun ModeSwitcherButton(
    currentMode: AccountMode,
    onClick: (AccountMode) -> Unit
) {
    val isOnchain = currentMode == AccountMode.ONCHAIN
    val rotation by animateFloatAsState(
        targetValue = if (isOnchain) 0f else 180f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "rotation"
    )
    
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )
    
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = if (isOnchain) {
                        listOf(OnchainPrimary, OnchainSecondary)
                    } else {
                        listOf(OffchainPrimary, OffchainSecondary)
                    }
                )
            )
            .clickable {
                onClick(if (isOnchain) AccountMode.OFFCHAIN else AccountMode.ONCHAIN)
            }
            .scale(scale)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isOnchain) Icons.Default.Send else Icons.Default.Home,
            contentDescription = if (isOnchain) "Blockchain Mode (On-chain) - Tap to switch to Local Mode" else "Local Mode (Off-chain) - Tap to switch to Blockchain Mode",
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
private fun TransitionOverlay(
    targetMode: AccountMode
) {
    val density = LocalDensity.current
    var animationProgress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        ) { value, _ ->
            animationProgress = value
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = if (targetMode == AccountMode.ONCHAIN) {
                        listOf(
                            OnchainBackground.copy(alpha = animationProgress),
                            OnchainPrimary.copy(alpha = animationProgress * 0.3f)
                        )
                    } else {
                        listOf(
                            OffchainBackground.copy(alpha = animationProgress),
                            OffchainPrimary.copy(alpha = animationProgress * 0.3f)
                        )
                    },
                    radius = with(density) { 1000.dp.toPx() * animationProgress }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Mode transition text
        AnimatedVisibility(
            visible = animationProgress > 0.3f,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Switching to",
                    fontSize = 16.sp,
                    color = if (targetMode == AccountMode.ONCHAIN) {
                        OnchainOnBackground.copy(alpha = 0.7f)
                    } else {
                        OffchainOnBackground.copy(alpha = 0.7f)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = targetMode.getDisplayName().uppercase(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (targetMode == AccountMode.ONCHAIN) {
                        OnchainPrimary
                    } else {
                        OffchainPrimary
                    }
                )
            }
        }
    }
}

// Extension function for AccountMode display name
fun AccountMode.getDisplayName(): String {
    return when (this) {
        AccountMode.ONCHAIN -> "Onchain"
        AccountMode.OFFCHAIN -> "Offchain"
    }
}