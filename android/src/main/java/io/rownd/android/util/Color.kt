package io.rownd.android.util

fun convertRGBtoString(R: Float, G: Float, B: Float): String {
    return "rgba(${(R*255).toInt()} ${(G*255).toInt()} ${(B*255).toInt()})"
}
