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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark Scheme Colors
val PrimaryDark = Color(0xFF109D96)
val OnPrimaryDark = Color(0xFFFFFFFF)
val PrimaryContainerDark = Color(0xFF003735)
val OnPrimaryContainerDark = Color(0xFFBCECE9)
val SecondaryDark = Color(0xFF4DB6AC)
val OnSecondaryDark = Color(0xFF003732)
val SecondaryContainerDark = Color(0xFF005047)
val OnSecondaryContainerDark = Color(0xFF80E2D6)
val TertiaryDark = Color(0xFFFFB020)
val OnTertiaryDark = Color(0xFF452B00)
val TertiaryContainerDark = Color(0xFF633F00)
val OnTertiaryContainerDark = Color(0xFFFFE0B2)
val BackgroundDark = Color(0xFF121212)
val OnBackgroundDark = Color(0xFFE3E3E3)
val SurfaceDark = Color(0xFF1C1C1C)
val OnSurfaceDark = Color(0xFFE3E3E3)
val SurfaceVariantDark = Color(0xFF2A2A2A)
val OnSurfaceVariantDark = Color(0xFFC4C4C4)
val OutlineDark = Color(0xFF8A8A8A)

// Light Scheme Colors
val PrimaryLight = Color(0xFF006A66)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFF70F7F0)
val OnPrimaryContainerLight = Color(0xFF00201E)
val SecondaryLight = Color(0xFF006A61)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFF6FF7E7)
val OnSecondaryContainerLight = Color(0xFF00201C)
val TertiaryLight = Color(0xFF825500)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDDB3)
val OnTertiaryContainerLight = Color(0xFF291800)
val BackgroundLight = Color(0xFFFAFDFB)
val OnBackgroundLight = Color(0xFF191C1C)
val SurfaceLight = Color(0xFFFAFDFB)
val OnSurfaceLight = Color(0xFF191C1C)
val SurfaceVariantLight = Color(0xFFDAE5E3)
val OnSurfaceVariantLight = Color(0xFF3F4947)
val OutlineLight = Color(0xFF6F7978)

// High Contrast Dark Scheme Colors
val PrimaryContrast = Color(0xFF00FFEA)
val OnPrimaryContrast = Color(0xFF000000)
val PrimaryContainerContrast = Color(0xFF00504A)
val OnPrimaryContainerContrast = Color(0xFF00FFEA)
val SecondaryContrast = Color(0xFF00FFC4)
val OnSecondaryContrast = Color(0xFF000000)
val SecondaryContainerContrast = Color(0xFF00503D)
val OnSecondaryContainerContrast = Color(0xFF00FFC4)
val BackgroundContrast = Color(0xFF000000)
val OnBackgroundContrast = Color(0xFFFFFFFF)
val SurfaceContrast = Color(0xFF0D0D0D)
val OnSurfaceContrast = Color(0xFFFFFFFF)
val SurfaceVariantContrast = Color(0xFF1A1A1A)
val OnSurfaceVariantContrast = Color(0xFFFFFFFF)
val OutlineContrast = Color(0xFFFFFFFF)

// Silver Metallic Scheme Colors
val PrimarySilver = Color(0xFF5A6268)
val OnPrimarySilver = Color(0xFFFFFFFF)
val PrimaryContainerSilver = Color(0xFFE2E4E6)
val OnPrimaryContainerSilver = Color(0xFF1D2124)
val SecondarySilver = Color(0xFF6C757D)
val OnSecondarySilver = Color(0xFFFFFFFF)
val SecondaryContainerSilver = Color(0xFFF1F3F5)
val OnSecondaryContainerSilver = Color(0xFF343A40)
val BackgroundSilver = Color(0xFFE9ECEF)
val OnBackgroundSilver = Color(0xFF212529)
val SurfaceSilver = Color(0xFFF8F9FA)
val OnSurfaceSilver = Color(0xFF212529)
val SurfaceVariantSilver = Color(0xFFDEE2E6)
val OnSurfaceVariantSilver = Color(0xFF495057)
val OutlineSilver = Color(0xFFADB5BD)

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
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
