package io.rownd.android.util

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

fun convertRGBtoString(R: Float, G: Float, B: Float): String {
    return "rgba(${(R*255).toInt()} ${(G*255).toInt()} ${(B*255).toInt()})"
}

fun convertStringToColor(color: String): Color {
    return Color(color.toColorInt())
}