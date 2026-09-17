package com.example.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * Modern Design System helper object for geological field and measurement cards.
 * Provides unified, high-contrast outdoor-safe elevations, borders, and color tokens.
 */
object GeoCardStyle {
    val CornerRadiusSmall = RoundedCornerShape(12.dp)
    val CornerRadiusMedium = RoundedCornerShape(16.dp)
    val CornerRadiusLarge = RoundedCornerShape(20.dp)
    val CornerRadiusExtra = RoundedCornerShape(24.dp)

    @Composable
    fun standardBorder(
        color: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
        width: Dp = 1.dp
    ): BorderStroke = BorderStroke(width, color)

    @Composable
    fun primaryBorder(
        alpha: Float = 0.35f,
        width: Dp = 1.2.dp
    ): BorderStroke = BorderStroke(width, MaterialTheme.colorScheme.primary.copy(alpha = alpha))

    @Composable
    fun accentBorder(
        color: Color,
        alpha: Float = 0.45f,
        width: Dp = 1.2.dp
    ): BorderStroke = BorderStroke(width, color.copy(alpha = alpha))

    @Composable
    fun cardElevation(
        defaultElevation: Dp = 1.5.dp,
        pressedElevation: Dp = 4.dp,
        focusedElevation: Dp = 2.dp,
        hoveredElevation: Dp = 3.dp
    ): CardElevation = CardDefaults.cardElevation(
        defaultElevation = defaultElevation,
        pressedElevation = pressedElevation,
        focusedElevation = focusedElevation,
        hoveredElevation = hoveredElevation
    )

    @Composable
    fun elevatedElevation(defaultElevation: Dp = 3.dp): CardElevation =
        CardDefaults.elevatedCardElevation(defaultElevation = defaultElevation)

    @Composable
    fun containerColor(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.surface
        }
    }

    @Composable
    fun elevatedContainerColor(): Color {
        val isDark = isSystemInDarkTheme()
        return if (isDark) {
            Color(0xFF222623)
        } else {
            Color(0xFFFFFFFF)
        }
    }

    @Composable
    fun standardCardColors(
        containerColor: Color = MaterialTheme.colorScheme.surface
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun subtleCardColors(
        containerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ): CardColors = CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface
    )

    @Composable
    fun primaryTonalColors(
        alpha: Float = 0.12f
    ): CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
}
