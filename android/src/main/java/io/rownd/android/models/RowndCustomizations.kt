package io.rownd.android.models

import android.content.res.Configuration
import android.content.res.Resources
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.rownd.android.Rownd
import io.rownd.android.util.Constants
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.math.roundToInt

@Serializable
open class RowndCustomizations() {
    @Transient
    open var sheetBackgroundColor: Color? = null

    internal fun isNightMode(): Boolean {
        val uiMode = Rownd.appHandleWrapper?.app?.get()?.resources?.configuration?.uiMode ?: Configuration.UI_MODE_NIGHT_UNDEFINED
        val isSystemInDarkTheme = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> isSystemInDarkTheme
        }
    }

    @Serializable(with = ColorAsHexStringSerializer::class)
    open val dynamicSheetBackgroundColor: Color
    get() {
        val darkMode = Rownd.stateRepo.state.value.appConfig.config.hub.customizations?.darkMode

        return when {
            sheetBackgroundColor != null -> sheetBackgroundColor!!
            darkMode == "disabled" -> Constants.BACKGROUND_LIGHT
            darkMode == "enabled" -> Constants.BACKGROUND_DARK
            isNightMode() -> Constants.BACKGROUND_DARK
            else -> Constants.BACKGROUND_LIGHT
        }
    }

    @Serializable(with = DpIntSerializer::class)
    open var sheetCornerBorderRadius: Dp = 24.dp

    open var loadingAnimation: Int? = null

    open var loadingAnimationJsonString: String? = null

    // The "standard" font scale on Android should be 1.0 and the font scale for the
    // Hub is about 12pt, so we'll multiply the Hub's baseline with the Android fontScale
    val defaultFontSize: Float = Resources.getSystem().configuration.fontScale * 12

    @Serializable(with = CustomStylesFlagSerializer::class)
    var customStylesFlag: Boolean = false

    @Serializable(with = FontFamilySerializer::class)
    var fontFamily: String = ""
}

class CustomStylesFlagSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("customStylesFlagSerializer", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean) {
        return encoder.encodeBoolean(Rownd?.state?.value?.appConfig?.config?.hub?.customStyles?.isNotEmpty() ?: false)
    }
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}

class FontFamilySerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("fontFamilyFlagSerializer", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) {
        return encoder.encodeString(Rownd?.state?.value?.appConfig?.config?.hub?.customizations?.fontFamily ?: "")
    }
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
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
        val density = Rownd.appHandleWrapper?.app?.get()!!.resources.displayMetrics.density
        return encoder.encodeInt((value.value * density).roundToInt())
    }

    override fun deserialize(decoder: Decoder): Dp {
        return Dp(decoder.decodeInt().toFloat())
    }
}
