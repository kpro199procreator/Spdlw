package com.spotdl.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Colores del tema claro
private val LightPrimary = Color(0xFF1DB954) // Verde Spotify
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFF90E0B0)
private val LightOnPrimaryContainer = Color(0xFF003920)

private val LightSecondary = Color(0xFF191414) // Negro Spotify
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFF535353)
private val LightOnSecondaryContainer = Color(0xFFE8E8E8)

private val LightTertiary = Color(0xFF2196F3)
private val LightOnTertiary = Color(0xFFFFFFFF)

private val LightError = Color(0xFFB00020)
private val LightOnError = Color(0xFFFFFFFF)

private val LightBackground = Color(0xFFFAFAFA)
private val LightOnBackground = Color(0xFF1C1B1F)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF1C1B1F)

// Colores del tema oscuro
private val DarkPrimary = Color(0xFF1DB954)
private val DarkOnPrimary = Color(0xFF003920)
private val DarkPrimaryContainer = Color(0xFF005230)
private val DarkOnPrimaryContainer = Color(0xFF90E0B0)

private val DarkSecondary = Color(0xFFB3B3B3)
private val DarkOnSecondary = Color(0xFF1C1C1C)
private val DarkSecondaryContainer = Color(0xFF383838)
private val DarkOnSecondaryContainer = Color(0xFFE8E8E8)

private val DarkTertiary = Color(0xFF64B5F6)
private val DarkOnTertiary = Color(0xFF003258)

private val DarkError = Color(0xFFCF6679)
private val DarkOnError = Color(0xFF690005)

private val DarkBackground = Color(0xFF121212)
private val DarkOnBackground = Color(0xFFE6E1E5)
private val DarkSurface = Color(0xFF1C1C1C)
private val DarkOnSurface = Color(0xFFE6E1E5)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    error = LightError,
    onError = LightOnError,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    error = DarkError,
    onError = DarkOnError,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface
)

@Composable
fun SpotDLTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
