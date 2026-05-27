package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val ImmersiveColorScheme = darkColorScheme(
    primary = ImmersivePrimary,
    onPrimary = ImmersiveOnPrimary,
    primaryContainer = ImmersivePrimary,
    onPrimaryContainer = ImmersiveOnPrimary,
    secondary = ImmersiveSecondary,
    onSecondary = ImmersiveOnPrimary,
    background = ImmersiveBg,
    onBackground = ImmersiveOnSurface,
    surface = ImmersiveSurface,
    onSurface = ImmersiveOnSurface,
    surfaceVariant = ImmersiveSecondaryContainer,
    onSurfaceVariant = ImmersiveOnSurfaceVariant,
    outline = ImmersiveOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark immersive mode by default for developers
    dynamicColor: Boolean = false, // Set false to ensure our custom theme applies perfectly
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) ImmersiveColorScheme else ImmersiveColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
