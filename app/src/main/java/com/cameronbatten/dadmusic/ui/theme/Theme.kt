package com.cameronbatten.dadmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryPurpleVariant,
    secondary = SecondaryTeal,
    onSecondary = OnSecondary,
    background = DarkBackground,
    onBackground = OnBackground,
    surface = DarkSurface,
    onSurface = OnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    error = ErrorRed,
    onError = OnError,
)

/**
 * Dad Music App theme — dark only in Milestone 1.
 * No light theme toggle until M3+.
 */
@Composable
fun DadMusicTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
