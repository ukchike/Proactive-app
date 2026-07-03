package com.unifiedproductivity.app.ui.util

import androidx.compose.ui.graphics.Color

/** Parse a "#RRGGBB" (or "#AARRGGBB") hex string into a Compose [Color]. */
fun parseHexColor(hex: String?, fallback: Color = Color(0xFF888888)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val cleaned = hex.removePrefix("#")
        val value = cleaned.toLong(16)
        when (cleaned.length) {
            6 -> Color(0xFF000000 or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (e: NumberFormatException) {
        fallback
    }
}
