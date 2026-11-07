package com.vibemusic.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * A complete, self-consistent colour set for one visual style. The rest of the
 * UI reads these values through the [VibeBlack], [VibeCream]… composable
 * accessors so that switching a theme repaints the whole app without touching
 * individual screens.
 */
data class VibePalette(
    val background: Color,
    val blackSoft: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryDeep: Color,
    val primarySoft: Color,
    val cream: Color,
    val muted: Color,
    val line: Color,
    val gold: Color,
    val isLight: Boolean
)

enum class VibeThemeStyle(val label: String, val description: String) {
    AMOLED("Dark AMOLED", "Negro puro y rojo ember"),
    MIDNIGHT("Midnight", "Azul profundo y tonos fríos"),
    OCEAN("Ocean", "Azules y turquesas"),
    PURPLE("Purple", "Morados y violetas"),
    CRIMSON("Crimson", "Rojos intensos"),
    EMERALD("Emerald", "Verdes y esmeralda"),
    LIGHT("Light", "Tema claro y cálido")
}

val AmoledPalette = VibePalette(
    background = Color(0xFF000000),
    blackSoft = Color(0xFF070709),
    surface = Color(0xFF0B0B0E),
    surfaceHigh = Color(0xFF141419),
    primary = Color(0xFFFF3B30),
    onPrimary = Color.White,
    primaryDeep = Color(0xFFB31217),
    primarySoft = Color(0xFFFF6B5E),
    cream = Color(0xFFF7F2EC),
    muted = Color(0xFF9B9BA6),
    line = Color(0xFF2A2A33),
    gold = Color(0xFFE8B86D),
    isLight = false
)

val MidnightPalette = VibePalette(
    background = Color(0xFF060A14),
    blackSoft = Color(0xFF080D18),
    surface = Color(0xFF0C1322),
    surfaceHigh = Color(0xFF151F33),
    primary = Color(0xFF4C8DFF),
    onPrimary = Color.White,
    primaryDeep = Color(0xFF1F4FA8),
    primarySoft = Color(0xFF8AB8FF),
    cream = Color(0xFFEEF3FF),
    muted = Color(0xFF8E9AB3),
    line = Color(0xFF223049),
    gold = Color(0xFF7FD1FF),
    isLight = false
)

val OceanPalette = VibePalette(
    background = Color(0xFF04100F),
    blackSoft = Color(0xFF051413),
    surface = Color(0xFF0A1A1A),
    surfaceHigh = Color(0xFF112827),
    primary = Color(0xFF2FD4C0),
    onPrimary = Color(0xFF00201D),
    primaryDeep = Color(0xFF0E6B66),
    primarySoft = Color(0xFF6EE7D8),
    cream = Color(0xFFE8FFFB),
    muted = Color(0xFF8AA9A6),
    line = Color(0xFF1D3A38),
    gold = Color(0xFF7FE8D8),
    isLight = false
)

val PurplePalette = VibePalette(
    background = Color(0xFF0A0614),
    blackSoft = Color(0xFF0C071A),
    surface = Color(0xFF120C22),
    surfaceHigh = Color(0xFF1D1433),
    primary = Color(0xFFB58CFF),
    onPrimary = Color(0xFF160B2B),
    primaryDeep = Color(0xFF4B2A8A),
    primarySoft = Color(0xFFC9A7FF),
    cream = Color(0xFFF4EEFF),
    muted = Color(0xFF9B8FB5),
    line = Color(0xFF2C2145),
    gold = Color(0xFFD9B8FF),
    isLight = false
)

val CrimsonPalette = VibePalette(
    background = Color(0xFF0B0507),
    blackSoft = Color(0xFF0D0608),
    surface = Color(0xFF160A0E),
    surfaceHigh = Color(0xFF221016),
    primary = Color(0xFFFF4D6D),
    onPrimary = Color.White,
    primaryDeep = Color(0xFF8E0E00),
    primarySoft = Color(0xFFFF6B5E),
    cream = Color(0xFFFFF0F3),
    muted = Color(0xFFB08A93),
    line = Color(0xFF3A1D25),
    gold = Color(0xFFFF9AAF),
    isLight = false
)

val EmeraldPalette = VibePalette(
    background = Color(0xFF04100A),
    blackSoft = Color(0xFF06130C),
    surface = Color(0xFF0A1A12),
    surfaceHigh = Color(0xFF12291D),
    primary = Color(0xFF35D07F),
    onPrimary = Color(0xFF00210F),
    primaryDeep = Color(0xFF146B3A),
    primarySoft = Color(0xFF6EE7A0),
    cream = Color(0xFFE9FFF2),
    muted = Color(0xFF86A796),
    line = Color(0xFF1D3A2A),
    gold = Color(0xFF9BE8B7),
    isLight = false
)

val LightPalette = VibePalette(
    background = Color(0xFFF7F4EF),
    blackSoft = Color(0xFFFFFDF9),
    surface = Color(0xFFFFFFFF),
    surfaceHigh = Color(0xFFEFEAE2),
    primary = Color(0xFFD62828),
    onPrimary = Color.White,
    primaryDeep = Color(0xFFA31217),
    primarySoft = Color(0xFFFF6B5E),
    cream = Color(0xFF1A1A1F),
    muted = Color(0xFF6B6B76),
    line = Color(0xFFDDD5CA),
    gold = Color(0xFFA86A1F),
    isLight = true
)

fun paletteForStyle(style: VibeThemeStyle): VibePalette = when (style) {
    VibeThemeStyle.AMOLED -> AmoledPalette
    VibeThemeStyle.MIDNIGHT -> MidnightPalette
    VibeThemeStyle.OCEAN -> OceanPalette
    VibeThemeStyle.PURPLE -> PurplePalette
    VibeThemeStyle.CRIMSON -> CrimsonPalette
    VibeThemeStyle.EMERALD -> EmeraldPalette
    VibeThemeStyle.LIGHT -> LightPalette
}

val LocalVibePalette = staticCompositionLocalOf { AmoledPalette }

// Semantic accessors. They intentionally keep the historical names so every
// existing screen follows the active theme automatically.
val VibeBlack: Color @Composable get() = LocalVibePalette.current.background
val VibeBlackSoft: Color @Composable get() = LocalVibePalette.current.blackSoft
val VibeSurface: Color @Composable get() = LocalVibePalette.current.surface
val VibeSurfaceHigh: Color @Composable get() = LocalVibePalette.current.surfaceHigh
val VibeRed: Color @Composable get() = LocalVibePalette.current.primary
val VibeRedDeep: Color @Composable get() = LocalVibePalette.current.primaryDeep
val VibeRedSoft: Color @Composable get() = LocalVibePalette.current.primarySoft
val VibeCream: Color @Composable get() = LocalVibePalette.current.cream
val VibeMuted: Color @Composable get() = LocalVibePalette.current.muted
val VibeLine: Color @Composable get() = LocalVibePalette.current.line
val VibeGold: Color @Composable get() = LocalVibePalette.current.gold

// Raw brand colours used only where a composable context is not available
// (for example while building the static preview palette list).
val VibeRedRaw = Color(0xFFFF3B30)
val VibeCreamRaw = Color(0xFFF7F2EC)
val VibeBlackRaw = Color(0xFF07070A)

val AccentPalettes = listOf(
    listOf(Color(0xFFFF3B30), Color(0xFF8E0E00), Color(0xFF2A0508)),
    listOf(Color(0xFFFF7A45), Color(0xFFB31217), Color(0xFF1A0710)),
    listOf(Color(0xFFF2C14E), Color(0xFFB3541E), Color(0xFF140A08)),
    listOf(Color(0xFFFF5E8A), Color(0xFF7A1030), Color(0xFF12070D)),
    listOf(Color(0xFF6EE7D8), Color(0xFF0E6B66), Color(0xFF04100F)),
    listOf(Color(0xFFB58CFF), Color(0xFF4B2A8A), Color(0xFF0A0614)),
    listOf(Color(0xFF4C8DFF), Color(0xFF1F4FA8), Color(0xFF060A14)),
    listOf(Color(0xFF35D07F), Color(0xFF146B3A), Color(0xFF04100A))
)
