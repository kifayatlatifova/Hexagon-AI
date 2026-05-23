package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = CyberBlue,
    secondary = AccentSkyBlue,
    background = DeepSpaceDb,
    surface = SurfaceDb,
    onPrimary = DeepSpaceDb,
    onSecondary = DeepSpaceDb,
    onBackground = OnSurfaceDb,
    onSurface = OnSurfaceDb,
    surfaceVariant = SurfaceDb,
    onSurfaceVariant = OnSurfaceDb
)

private val LightColorScheme = lightColorScheme(
    primary = RoyalBlueMain,
    secondary = AccentSkyBlue,
    background = SoftBgBlue,
    surface = PureWhite,
    onPrimary = PureWhite,
    onSecondary = DeepNavyText,
    onBackground = DeepNavyText,
    onSurface = DeepNavyText,
    surfaceVariant = PureWhite,
    onSurfaceVariant = DeepNavyText
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic coloring so that the explicit white and blue design is guaranteed
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
