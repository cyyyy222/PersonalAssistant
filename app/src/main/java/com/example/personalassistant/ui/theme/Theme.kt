package com.example.personalassistant.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Fixed light palette: wallpaper and system dark mode must not tint this UI.
@Composable
fun PersonalAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green, onPrimary = Color.White,
            primaryContainer = SoftGreen, onPrimaryContainer = Green,
            secondary = Green, onSecondary = Color.White,
            secondaryContainer = SoftGreen, onSecondaryContainer = Green,
            background = Canvas, onBackground = Ink,
            surface = Canvas, onSurface = Ink,
            surfaceVariant = SoftGray, onSurfaceVariant = Muted,
            surfaceContainer = SoftGray, surfaceContainerHigh = SoftGray,
            outline = Muted, outlineVariant = Line, surfaceTint = Color.Transparent
        ),
        typography = Typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
            extraLarge = RoundedCornerShape(24.dp)
        ), content = content
    )
}
