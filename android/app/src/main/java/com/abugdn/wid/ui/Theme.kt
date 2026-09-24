package com.abugdn.wid.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Red = Color(0xFFE53935)

private val colors = darkColorScheme(
    primary = Red,
    onPrimary = Color.White,
    secondary = Color(0xFFB0BEC5),
    background = Color(0xFF121416),
    surface = Color(0xFF121416),
    surfaceVariant = Color(0xFF1E2226),
    onSurfaceVariant = Color(0xFFB8BEC4),
)

@Composable
fun WidTheme(content: @Composable () -> Unit) = MaterialTheme(colorScheme = colors, content = content)
