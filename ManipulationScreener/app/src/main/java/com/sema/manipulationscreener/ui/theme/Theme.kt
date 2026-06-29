package com.sema.manipulationscreener.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val DarkBackground = Color(0xFF0D0D0D)
val DarkSurface = Color(0xFF1A1A2E)
val DarkCard = Color(0xFF16213E)
val AccentGreen = Color(0xFF00C853)
val AccentRed = Color(0xFFFF1744)
val AccentOrange = Color(0xFFFF9100)
val AccentYellow = Color(0xFFFFD600)
val TextPrimary = Color(0xFFE0E0E0)
val TextSecondary = Color(0xFF9E9E9E)

private val DarkColorScheme = darkColorScheme(
    primary = AccentOrange,
    secondary = AccentGreen,
    tertiary = AccentRed,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard
)

@Composable
fun ManipulationScreenerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = DarkBackground.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
