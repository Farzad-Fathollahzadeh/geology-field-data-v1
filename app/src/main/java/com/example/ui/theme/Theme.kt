package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp

data class TouchSizing(
    val buttonMinHeight: Dp,
    val paddingSmall: Dp,
    val paddingMedium: Dp,
    val iconSize: Dp,
) {
    companion object {
        fun getProfile(profile: String): TouchSizing {
            return when (profile) {
                "LARGE" -> TouchSizing(
                    buttonMinHeight = 56.dp,
                    paddingSmall = 12.dp,
                    paddingMedium = 20.dp,
                    iconSize = 28.dp
                )
                "GLOVE_FRIENDLY" -> TouchSizing(
                    buttonMinHeight = 64.dp,
                    paddingSmall = 16.dp,
                    paddingMedium = 24.dp,
                    iconSize = 32.dp
                )
                else -> TouchSizing( // "STANDARD"
                    buttonMinHeight = 44.dp,
                    paddingSmall = 8.dp,
                    paddingMedium = 16.dp,
                    iconSize = 24.dp
                )
            }
        }
    }
}

val LocalTouchSizing = staticCompositionLocalOf { TouchSizing.getProfile("STANDARD") }

private val DarkColorScheme = darkColorScheme(
    primary = GeoAccentCopperDark,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF321F12),
    onPrimaryContainer = GeoAccentCopperDark,
    secondary = GeoActiveBlue,
    onSecondary = Color.White,
    secondaryContainer = GeoCardBackgroundDark,
    onSecondaryContainer = GeoTextPrimaryDark,
    background = GeoBackgroundDark,
    onBackground = GeoTextPrimaryDark,
    surface = GeoCardBackgroundDark,
    onSurface = GeoTextPrimaryDark,
    surfaceVariant = GeoSurfaceVariantDark,
    onSurfaceVariant = GeoTextSecondaryDark,
    outline = GeoBorderDark,
    outlineVariant = GeoBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = GeoAccentCopper,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFDF6F0), // ultra-soft tint of copper/cream for highlights
    onPrimaryContainer = GeoAccentCopper,
    secondary = GeoActiveBlue,
    onSecondary = Color.White,
    secondaryContainer = GeoCardBackground,
    onSecondaryContainer = GeoTextPrimary,
    background = GeoBackground,
    onBackground = GeoTextPrimary,
    surface = GeoCardBackground,
    onSurface = GeoTextPrimary,
    surfaceVariant = GeoSurfaceVariant,
    onSurfaceVariant = GeoTextSecondary,
    outline = GeoBorder,
    outlineVariant = GeoBorder
)

// Ultra contrast, vivid color scheme optimized for field reading in direct sunlight
private val OutdoorColorScheme = lightColorScheme(
    primary = Color(0xFF0033FF), // Saturated blue
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6EDFF),
    onPrimaryContainer = Color(0xFF001180),
    secondary = Color(0xFF006600), // Pure saturated forest-green
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2FFE2),
    onSecondaryContainer = Color(0xFF003300),
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFDFDFDF),
    onSurfaceVariant = Color.Black,
    outline = Color.Black,
    outlineVariant = Color.Black
)

@Composable
fun MyApplicationTheme(
    themeSelection: String = "DARK",
    dynamicColor: Boolean = false,
    fontSizeMultiplier: Float = 1.0f,
    fontFamilySel: String = "SansSerif",
    touchSizingProfile: String = "STANDARD",
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val colorScheme = when (themeSelection) {
        "DARK" -> DarkColorScheme
        "LIGHT" -> LightColorScheme
        "OUTDOOR" -> OutdoorColorScheme
        else -> if (isSystemDark) DarkColorScheme else LightColorScheme
    }

    CompositionLocalProvider(LocalTouchSizing provides TouchSizing.getProfile(touchSizingProfile)) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = getDynamicTypography(fontSizeMultiplier, fontFamilySel),
            content = content
        )
    }
}
