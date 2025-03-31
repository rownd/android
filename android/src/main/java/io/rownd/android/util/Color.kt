package io.rownd.android.util

import androidx.compose.ui.graphics.Color

fun convertRGBtoString(R: Float, G: Float, B: Float): String {
    return "rgba(${(R*255).toInt()} ${(G*255).toInt()} ${(B*255).toInt()})"
}

fun convertStringToColor(color: String): Color {
    return Color(hexToColor(color))
}

fun hexToColor(hex: String): Int {
    val hexColor = hex.replace("#", "").uppercase()
    return if (hexColor.length == 6) {
        0xFF000000.toInt() or hexColor.toInt(16)
    } else if (hexColor.length == 8) {
        hexColor.toInt(16)
    } else {
        return 0xFF000000.toInt()
    }
}