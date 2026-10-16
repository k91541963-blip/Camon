package com.camon.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OnAccentDark = Color(0xFF07231A)
private val OnAccentLight = Color(0xFFFFFFFF)

private val DarkScheme = darkColorScheme(
    primary = CamonGreen,
    onPrimary = OnAccentDark,
    secondary = CamonMint,
    onSecondary = OnAccentDark,
    tertiary = CamonBlue,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextMuted,
    outline = DarkOutline,
    error = CamonRed
)

private val LightScheme = lightColorScheme(
    primary = CamonGreenDark,
    onPrimary = OnAccentLight,
    secondary = CamonGreen,
    onSecondary = OnAccentDark,
    tertiary = CamonBlue,
    background = LightBackground,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = LightTextMuted,
    outline = LightOutline,
    error = CamonRed
)

@Composable
fun CamonTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = CamonTypography,
        content = content
    )
}
