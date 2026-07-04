package com.unifiedproductivity.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// iOS blue is the system-wide interactive color; red and blue back the Calendar
// and Reminders accents. Notes screens apply their gold accent explicitly.
private val LightColors = lightColorScheme(
    primary = RemindersAccent,
    secondary = CalendarAccent,
    tertiary = RemindersAccent,
    background = LightBackground,
    surface = LightSurface,
    onBackground = LightOnSurface,
    onSurface = LightOnSurface
)

private val DarkColors = darkColorScheme(
    primary = RemindersAccent,
    secondary = CalendarAccent,
    tertiary = RemindersAccent,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = DarkOnSurface,
    onSurface = DarkOnSurface
)

@Composable
fun UnifiedProductivityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic (Material You) color is off by default: it overrides the per-module
    // accent palette and breaks the Apple-like visual identity the spec asks for.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
