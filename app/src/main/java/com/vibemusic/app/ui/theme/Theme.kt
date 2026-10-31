package com.vibemusic.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VibeColorScheme = darkColorScheme(
    primary = VibeRed,
    onPrimary = Color.White,
    primaryContainer = VibeRedDeep,
    onPrimaryContainer = VibeCream,
    secondary = VibeGold,
    onSecondary = VibeBlack,
    tertiary = VibeRedSoft,
    background = VibeBlack,
    onBackground = VibeCream,
    surface = VibeSurface,
    onSurface = VibeCream,
    surfaceVariant = VibeSurfaceHigh,
    onSurfaceVariant = VibeMuted,
    outline = VibeLine,
    outlineVariant = VibeLine,
    error = Color(0xFFFF7A70),
    onError = VibeBlack,
    scrim = Color(0xCC000000)
)

@Composable
fun VibeMusicTheme(
    amoled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = if (amoled) {
        VibeColorScheme.copy(background = Color.Black, surface = Color(0xFF0B0B0E))
    } else {
        VibeColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = VibeTypography,
        content = content
    )
}
