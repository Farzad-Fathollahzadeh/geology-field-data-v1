package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Dynamic Typography Scale generator based on user settings selection
fun getDynamicTypography(multiplier: Float, fontFamilyName: String): Typography {
    val selectedFont = when (fontFamilyName) {
        "Monospace" -> FontFamily.Monospace
        "Serif" -> FontFamily.Serif
        else -> FontFamily.SansSerif
    }

    return Typography(
        titleLarge = TextStyle(
            fontFamily = selectedFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (24 * multiplier).sp,
            lineHeight = (30 * multiplier).sp,
            letterSpacing = (-0.5).sp
        ),
        titleMedium = TextStyle(
            fontFamily = selectedFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (18 * multiplier).sp,
            lineHeight = (24 * multiplier).sp,
            letterSpacing = (-0.2).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = selectedFont,
            fontWeight = FontWeight.Medium,
            fontSize = (16 * multiplier).sp,
            lineHeight = (24 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = selectedFont,
            fontWeight = FontWeight.Medium,
            fontSize = (15 * multiplier).sp,
            lineHeight = (22 * multiplier).sp,
            letterSpacing = 0.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Monospace, // Keep coordinates and zone data aligned
            fontWeight = FontWeight.SemiBold,
            fontSize = (13 * multiplier).sp,
            lineHeight = (18 * multiplier).sp,
            letterSpacing = 0.1.sp
        )
    )
}

// Apple-inspired Typography Scale using Inter/System fallback (Default baseline)
val Typography = getDynamicTypography(1.0f, "SansSerif")
