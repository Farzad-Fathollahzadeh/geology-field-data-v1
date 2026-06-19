package com.example.ui.gis

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val GisDarkColorScheme = darkColorScheme(
    primary = GisDarkPrimary,
    secondary = GisDarkSecondary,
    tertiary = GisDarkAccent,
    background = GisDarkBackground,
    surface = GisDarkSurface,
    onPrimary = GisDarkBackground,
    onSecondary = GisDarkPrimary,
    onBackground = GisDarkPrimary,
    onSurface = GisDarkPrimary,
    outline = GisDarkSecondary
)

private val GisLightColorScheme = lightColorScheme(
    primary = GisLightPrimary,
    secondary = GisLightSecondary,
    tertiary = GisLightAccent,
    background = GisLightBackground,
    surface = GisLightSurface,
    onPrimary = GisLightBackground,
    onSecondary = GisLightPrimary,
    onBackground = GisLightPrimary,
    onSurface = GisLightPrimary,
    outline = GisLightSecondary
)

@Composable
fun GisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GisDarkColorScheme else GisLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content
    )
}
