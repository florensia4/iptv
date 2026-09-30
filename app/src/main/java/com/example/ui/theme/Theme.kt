package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val IPTVColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = MintDark,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    secondary = MintPrimary,
    onSecondary = MintDark,
    secondaryContainer = MidnightSurfaceHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberFavorite,
    onTertiary = MintDark,
    tertiaryContainer = AmberFavoriteContainer,
    onTertiaryContainer = AmberFavorite,
    background = MidnightBackground,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = BorderOutline,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep consistent sleek IPTV midnight aesthetic
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = IPTVColorScheme,
        typography = Typography,
        content = content
    )
}
