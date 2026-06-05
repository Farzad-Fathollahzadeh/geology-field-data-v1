package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val HighContrastColorScheme = darkColorScheme(
    primary = PrimaryContrast,
    onPrimary = OnPrimaryContrast,
    primaryContainer = PrimaryContainerContrast,
    onPrimaryContainer = OnPrimaryContainerContrast,
    secondary = SecondaryContrast,
    onSecondary = OnSecondaryContrast,
    secondaryContainer = SecondaryContainerContrast,
    onSecondaryContainer = OnSecondaryContainerContrast,
    tertiary = PrimaryContrast,
    onTertiary = OnPrimaryContrast,
    tertiaryContainer = PrimaryContainerContrast,
    onTertiaryContainer = OnPrimaryContainerContrast,
    background = BackgroundContrast,
    onBackground = OnBackgroundContrast,
    surface = SurfaceContrast,
    onSurface = OnSurfaceContrast,
    surfaceVariant = SurfaceVariantContrast,
    onSurfaceVariant = OnSurfaceVariantContrast,
    outline = OutlineContrast
)

private val SilverColorScheme = lightColorScheme(
    primary = PrimarySilver,
    onPrimary = OnPrimarySilver,
    primaryContainer = PrimaryContainerSilver,
    onPrimaryContainer = OnPrimaryContainerSilver,
    secondary = SecondarySilver,
    onSecondary = OnSecondarySilver,
    secondaryContainer = SecondaryContainerSilver,
    onSecondaryContainer = OnSecondaryContainerSilver,
    tertiary = SecondarySilver,
    onTertiary = OnSecondarySilver,
    tertiaryContainer = SecondaryContainerSilver,
    onTertiaryContainer = OnSecondaryContainerSilver,
    background = BackgroundSilver,
    onBackground = OnBackgroundSilver,
    surface = SurfaceSilver,
    onSurface = OnSurfaceSilver,
    surfaceVariant = SurfaceVariantSilver,
    onSurfaceVariant = OnSurfaceVariantSilver,
    outline = OutlineSilver
)

@Composable
fun MyApplicationTheme(
    themeSetting: String = "Dark", // "Light", "Dark", "HighContrast", "Silver"
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeSetting) {
        "Light" -> LightColorScheme
        "HighContrast" -> HighContrastColorScheme
        "Silver" -> SilverColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            activity?.window?.let { window ->
                window.statusBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = 
                    (themeSetting == "Light" || themeSetting == "Silver")
            }
        }
    }


    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
