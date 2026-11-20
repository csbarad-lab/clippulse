package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ClipPulseDarkColorScheme = darkColorScheme(
    primary = CoralPulse,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF421221),
    onPrimaryContainer = CoralGlow,
    secondary = CyberCyan,
    onSecondary = StudioObsidian,
    secondaryContainer = Color(0xFF0A3440),
    onSecondaryContainer = CyberCyan,
    tertiary = AmberGold,
    onTertiary = StudioObsidian,
    tertiaryContainer = Color(0xFF3A2B05),
    onTertiaryContainer = AmberGold,
    background = StudioObsidian,
    onBackground = TextPrimary,
    surface = StudioSlate,
    onSurface = TextPrimary,
    surfaceVariant = StudioCard,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = StudioObsidian,
    surfaceContainerLow = StudioSlate,
    surfaceContainer = StudioCard,
    surfaceContainerHigh = StudioCardElevated,
    outline = StudioOutline,
    outlineVariant = Color(0xFF232A40),
    error = ErrorCrimson,
    onError = Color.White
)

private val ClipPulseShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipPulseTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipPulseDarkColorScheme,
        typography = ClipPulseTypography,
        shapes = ClipPulseShapes,
        content = content
    )
}
