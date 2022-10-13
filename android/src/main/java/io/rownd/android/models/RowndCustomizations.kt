package io.rownd.android.models

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rownd.android.Rownd
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

@Serializable
open class RowndCustomizations() {
    @Serializable(with = ColorAsHexStringSerializer::class)
    open val sheetBackgroundColor: Color
    get() {
        val uiMode = Rownd.appHandleWrapper.app.get()!!.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return if (uiMode == Configuration.UI_MODE_NIGHT_YES) {
            Color(0xff1c1c1e)
        } else {
            Color(0xffffffff)

        }
    }

    @Serializable(with = DpIntSerializer::class)
    open var sheetCornerBorderRadius: Dp = 25.dp

    open var loadingAnimation: Int? = null

    // The "standard" font scale on Android should be 1.0 and the font scale for the
    // Hub is about 12pt, so we'll multiply the Hub's baseline with the Android fontScale
    val defaultFontSize: Float = Resources.getSystem().configuration.fontScale * 12
}

class ColorAsHexStringSerializer : KSerializer<Color> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: Color) {
        val colorString = String.format("rgba(%d, %d, %d, %.1f)",
            (value.red * 255).roundToInt(),
            (value.green * 255).roundToInt(),
            (value.blue * 255).roundToInt(),
            value.alpha
        )
        return encoder.encodeString(colorString)
    }
//    override fun serialize(encoder: Encoder, value: Color) = encoder.encodeString(
//        String.format(
//            "#%06X",
//            0xFFFFFF and value.toArgb()
//        ))
    override fun deserialize(decoder: Decoder): Color = Color(
        android.graphics.Color.parseColor(decoder.decodeString())
    )
}

class DpIntSerializer : KSerializer<Dp> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Dp", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Dp) {
        val density = Rownd.appHandleWrapper.app.get()!!.resources.displayMetrics.density
        return encoder.encodeInt((value.value * density).roundToInt())
    }

    override fun deserialize(decoder: Decoder): Dp {
        return Dp(decoder.decodeInt().toFloat())
    }
}