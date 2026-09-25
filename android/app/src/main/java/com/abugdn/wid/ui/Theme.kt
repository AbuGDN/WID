package com.abugdn.wid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import com.abugdn.wid.data.ThemeMode

// Identidade Argos, paleta "Ordem": preto profundo, ouro antigo, osso e vermelho-sangue.

/** Ouro antigo: destaque (o olho, títulos de seção, rótulos). */
val Accent = Color(0xFFC9A227)

/** Vermelho-sangue: só para alerta (urgente, anomalia, números divergentes, tensão crítica). */
val Alert = Color(0xFFB3122E)

val Ink = Color(0xFF050505)
val Bone = Color(0xFFE8E2D0)
val Ash = Color(0xFF8A8578)

private val dark = darkColorScheme(
    primary = Accent,
    onPrimary = Ink,
    secondary = Ash,
    onSecondary = Ink,
    tertiary = Alert,
    background = Ink,
    onBackground = Bone,
    surface = Ink,
    onSurface = Bone,
    surfaceVariant = Color(0xFF121214),
    onSurfaceVariant = Ash,
    surfaceContainer = Color(0xFF0E0E10),
    surfaceContainerLow = Color(0xFF0A0A0B),
    surfaceContainerHigh = Color(0xFF16161A),
    surfaceContainerHighest = Color(0xFF1C1C20),
    secondaryContainer = Color(0xFF2A2415),
    onSecondaryContainer = Accent,
    outline = Color(0xFF3A362C),
    outlineVariant = Color(0xFF26241F),
    error = Alert,
)

/** Preto puro: em telas OLED os pixels pretos ficam apagados e economizam bateria. */
private val amoled = dark.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF0C0C0D),
    surfaceContainer = Color(0xFF070708),
    surfaceContainerLow = Color.Black,
    surfaceContainerHigh = Color(0xFF101012),
)

/** Tema claro "pergaminho": papel envelhecido com tinta escura e ouro mais fechado. */
private val light = lightColorScheme(
    primary = Color(0xFF8C6E12),
    onPrimary = Color.White,
    secondary = Color(0xFF6B6558),
    tertiary = Alert,
    background = Color(0xFFF3EEDF),
    onBackground = Color(0xFF1A1814),
    surface = Color(0xFFF3EEDF),
    onSurface = Color(0xFF1A1814),
    surfaceVariant = Color(0xFFE7DFC8),
    onSurfaceVariant = Color(0xFF5E5849),
    secondaryContainer = Color(0xFFE9DDB5),
    onSecondaryContainer = Color(0xFF3D300A),
    outlineVariant = Color(0xFFD3C9AE),
    error = Alert,
)

/** Títulos em serifa (clima de documento antigo); texto corrido segue sem serifa para leitura. */
private fun argosTypography(): Typography {
    val base = Typography()
    val serif = FontFamily.Serif
    return base.copy(
        displaySmall = base.displaySmall.copy(fontFamily = serif),
        headlineLarge = base.headlineLarge.copy(fontFamily = serif),
        headlineMedium = base.headlineMedium.copy(fontFamily = serif),
        headlineSmall = base.headlineSmall.copy(fontFamily = serif),
        titleLarge = base.titleLarge.copy(fontFamily = serif),
        titleMedium = base.titleMedium.copy(fontFamily = serif),
        titleSmall = base.titleSmall.copy(fontFamily = serif),
    )
}

/** Economia de dados: quando verdadeiro, as telas não carregam imagens. */
val LocalDataSaver = staticCompositionLocalOf { false }

@Composable
fun WidTheme(mode: ThemeMode = ThemeMode.DARK, textScale: Float = 1f, content: @Composable () -> Unit) {
    val scheme = when (mode) {
        ThemeMode.AMOLED -> amoled
        ThemeMode.DARK -> dark
        ThemeMode.LIGHT -> light
        ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) dark else light
    }
    // Tamanho do texto dos Ajustes, aplicado por cima da escala de fonte do sistema.
    val density = LocalDensity.current
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * textScale)) {
        MaterialTheme(colorScheme = scheme, typography = argosTypography(), content = content)
    }
}
