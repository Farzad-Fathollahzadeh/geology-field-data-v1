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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
