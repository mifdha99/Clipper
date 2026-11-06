package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = CrimsonRedPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4A0A19),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = NeonCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF004F55),
    onSecondaryContainer = Color(0xFFB5FAFF),
    tertiary = ViralYellow,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkSurfaceBorder
)

private val LightColorScheme = darkColorScheme(
    // For video creation apps, dark aesthetic is the industry standard for color accuracy and battery efficiency
    primary = CrimsonRedPrimary,
    onPrimary = Color.White,
    secondary = NeonCyan,
    background = DarkBackground,
    surface = DarkSurface
)

@Composable
fun TubeClipTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
