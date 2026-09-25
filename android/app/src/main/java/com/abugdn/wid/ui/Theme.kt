package com.abugdn.wid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.graphics.Color
import com.abugdn.wid.data.ThemeMode

val Red = Color(0xFFE53935)

private val dark = darkColorScheme(
    primary = Red,
    onPrimary = Color.White,
    secondary = Color(0xFFB0BEC5),
    background = Color(0xFF121416),
    surface = Color(0xFF121416),
    surfaceVariant = Color(0xFF1E2226),
    onSurfaceVariant = Color(0xFFB8BEC4),
)

private val light = lightColorScheme(
    primary = Color(0xFFC62828),
    onPrimary = Color.White,
    secondary = Color(0xFF546E7A),
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFFEDEFF1),
    onSurfaceVariant = Color(0xFF5F6368),
)

/** Preto puro: em telas OLED os pixels pretos ficam apagados e economizam bateria. */
private val amoled = dark.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF111316),
    surfaceContainer = Color(0xFF0A0B0C),
    surfaceContainerLow = Color.Black,
    surfaceContainerHigh = Color(0xFF141619),
)

/** Economia de dados: quando verdadeiro, as telas não carregam imagens. */
val LocalDataSaver = staticCompositionLocalOf { false }

@Composable
fun WidTheme(mode: ThemeMode = ThemeMode.SYSTEM, textScale: Float = 1f, content: @Composable () -> Unit) {
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val scheme = when {
        mode == ThemeMode.AMOLED -> amoled
        isDark -> dark
        else -> light
    }
    // Tamanho do texto dos Ajustes, aplicado por cima da escala de fonte do sistema.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * textScale)) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
