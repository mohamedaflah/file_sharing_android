package com.sharefast.presentation.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

private data class AccentPair(
    val lightPrimary: Color,
    val lightTertiary: Color,
    val darkPrimary: Color,
    val darkTertiary: Color,
)

private val accentPairs = listOf(
    AccentPair(BrandPrimary, BrandTertiary, Color(0xFF93C5FD), Color(0xFFF9A8D4)),
    AccentPair(Color(0xFF7C3AED), Color(0xFFEC4899), Color(0xFFC4B5FD), Color(0xFFF472B6)),
    AccentPair(Color(0xFF0891B2), Color(0xFF14B8A6), Color(0xFF22D3EE), Color(0xFF5EEAD4)),
    AccentPair(Color(0xFFEA580C), Color(0xFFFACC15), Color(0xFFFDBA74), Color(0xFFFDE047)),
    AccentPair(Color(0xFF059669), Color(0xFF10B981), Color(0xFF6EE7B7), Color(0xFF34D399)),
    AccentPair(Color(0xFFDB2777), Color(0xFFA855F7), Color(0xFFF9A8D4), Color(0xFFC084FC)),
)

val AccentPresetCount: Int get() = accentPairs.size

fun ColorScheme.withAccent(accentIndex: Int, useDarkPalette: Boolean): ColorScheme {
    val p = accentPairs[accentIndex.mod(accentPairs.size)]
    return if (useDarkPalette) {
        copy(primary = p.darkPrimary, tertiary = p.darkTertiary)
    } else {
        copy(primary = p.lightPrimary, tertiary = p.lightTertiary)
    }
}
