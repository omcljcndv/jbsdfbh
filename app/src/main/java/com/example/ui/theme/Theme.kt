package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = TheaterRed,
    secondary = OrangeFlame,
    tertiary = EmeraldGreen,
    background = DarkGrey,
    surface = CardGrey,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = LightText,
    onSurface = LightText
)

// We want cinema apps to remain elegant dark by default even in "light" system settings,
// but we support a slightly softer warm dark look for light settings.
private val LightColorScheme = lightColorScheme(
    primary = TheaterRed,
    secondary = OrangeFlame,
    tertiary = EmeraldGreen,
    background = Color(0xFF141419),
    surface = Color(0xFF1F2026),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFFF0F0F5),
    onSurface = Color(0xFFF0F0F5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme by default for professional cinema environment
    dynamicColor: Boolean = false, // Keep the custom theater colors
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
