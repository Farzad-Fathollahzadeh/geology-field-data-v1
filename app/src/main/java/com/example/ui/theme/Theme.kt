package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1A5F43),         // Deep Forest/Mineral Green
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFACF2CD),
    onPrimaryContainer = Color(0xFF002112),
    secondary = Color(0xFF4F6354),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D5),
    onSecondaryContainer = Color(0xFF0C1F13),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBFEAF7),
    onTertiaryContainer = Color(0xFF001F26),
    background = Color(0xFFF9F9F9),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF9F9F9),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDCE5DC),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF91D5B2),
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF005232),
    onPrimaryContainer = Color(0xFFACF2CD),
    secondary = Color(0xFFB6CCBA),
    onSecondary = Color(0xFF223528),
    secondaryContainer = Color(0xFF384C3E),
    onSecondaryContainer = Color(0xFFD2E8D5),
    tertiary = Color(0xFFA3CDDB),
    onTertiary = Color(0xFF033541),
    tertiaryContainer = Color(0xFF224D58),
    onTertiaryContainer = Color(0xFFBFEAF7),
    background = Color(0xFF191C1A),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF191C1A),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF404943),
    onSurfaceVariant = Color(0xFFDCE5DC),
    outline = Color(0xFF8A938C)
)

private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFF00FF00),         // Pure Green
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF004D00),
    onPrimaryContainer = Color(0xFF00FF00),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF000000),
    secondaryContainer = Color(0xFF333333),
    onSecondaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF000000),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF000000),
    onSurface = Color(0xFFFFFFFF),
    outline = Color(0xFFFFFFFF)
)

private val SilverColorScheme = lightColorScheme(
    primary = Color(0xFF2C3E50),         // Sleek Slate Blue
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6DBDF),
    onPrimaryContainer = Color(0xFF1B2631),
    secondary = Color(0xFF5D6D7E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEAEDED),
    onSecondaryContainer = Color(0xFF212F3D),
    tertiary = Color(0xFF7F8C8D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF2F4F4),
    onTertiaryContainer = Color(0xFF2C3E50),
    background = Color(0xFFF5F7FA),
    onBackground = Color(0xFF2C3E50),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF2C3E50),
    surfaceVariant = Color(0xFFEBEDEF),
    onSurfaceVariant = Color(0xFF566573),
    outline = Color(0xFFBDC3C7)
)

@Composable
fun MyApplicationTheme(
    themeSetting: String,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "Dark" -> true
        "HighContrast" -> true
        "Light" -> false
        "Silver" -> false
        else -> isSystemInDarkTheme()
    }
    
    val colorScheme = when (themeSetting) {
        "HighContrast" -> HighContrastColorScheme
        "Dark" -> DarkColorScheme
        "Silver" -> SilverColorScheme
        "Light" -> LightColorScheme
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
