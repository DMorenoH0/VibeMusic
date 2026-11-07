package com.vibemusic.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private fun schemeFor(palette: VibePalette) = if (palette.isLight) {
    lightColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryDeep,
        onPrimaryContainer = Color.White,
        secondary = palette.gold,
        onSecondary = Color.White,
        tertiary = palette.primarySoft,
        background = palette.background,
        onBackground = palette.cream,
        surface = palette.surface,
        onSurface = palette.cream,
        surfaceVariant = palette.surfaceHigh,
        onSurfaceVariant = palette.muted,
        outline = palette.line,
        outlineVariant = palette.line,
        error = Color(0xFFB3261E),
        onError = Color.White,
        scrim = Color(0x66000000)
    )
} else {
    darkColorScheme(
        primary = palette.primary,
        onPrimary = palette.onPrimary,
        primaryContainer = palette.primaryDeep,
        onPrimaryContainer = palette.cream,
        secondary = palette.gold,
        onSecondary = palette.background,
        tertiary = palette.primarySoft,
        background = palette.background,
        onBackground = palette.cream,
        surface = palette.surface,
        onSurface = palette.cream,
        surfaceVariant = palette.surfaceHigh,
        onSurfaceVariant = palette.muted,
        outline = palette.line,
        outlineVariant = palette.line,
        error = Color(0xFFFF7A70),
        onError = palette.background,
        scrim = Color(0xCC000000)
    )
}

@Composable
fun VibeMusicTheme(
    theme: VibeThemeStyle = VibeThemeStyle.AMOLED,
    content: @Composable () -> Unit
) {
    val palette = paletteForStyle(theme)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = palette.isLight
            controller.isAppearanceLightNavigationBars = palette.isLight
        }
    }
    CompositionLocalProvider(LocalVibePalette provides palette) {
        MaterialTheme(
            colorScheme = schemeFor(palette),
            typography = VibeTypography,
            content = content
        )
    }
}
