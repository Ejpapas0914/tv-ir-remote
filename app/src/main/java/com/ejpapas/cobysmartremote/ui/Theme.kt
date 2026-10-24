package com.ejpapas.cobysmartremote.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CobyRed = Color(0xFFE53935)
val CobyGreen = Color(0xFF43A047)
val CobyAmber = Color(0xFFFFB300)

private val CobyColors = darkColorScheme(
    primary = CobyRed,
    onPrimary = Color.White,
    secondary = Color(0xFFFF6F60),
    background = Color(0xFF0E0E10),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF17171A),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFFC9C9CF),
)

@Composable
fun CobyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = CobyColors, content = content)
}
