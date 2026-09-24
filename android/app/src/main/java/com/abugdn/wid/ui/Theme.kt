package com.abugdn.wid.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

@Composable
fun WidTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(colorScheme = if (isDark) dark else light, content = content)
}
