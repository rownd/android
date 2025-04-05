package io.rownd.android.util

import android.os.Build
import androidx.compose.ui.graphics.Color
import io.rownd.android.BuildConfig
import io.rownd.android.models.SupportedFeature
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object Constants {
    val DEFAULT_API_USER_AGENT = "Rownd SDK for Android/${BuildConfig.VERSION_NAME} (Language: Kotlin; Platform=Android ${Build.VERSION.BASE_OS} ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT});)"
    val DEFAULT_WEB_USER_AGENT = "Rownd SDK for Android/${BuildConfig.VERSION_NAME} (Language: Kotlin; Platform=Android ${Build.VERSION.BASE_OS} ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT});)"

    val BACKGROUND_LIGHT = Color(0xffffffff)
    val BACKGROUND_DARK = Color(0xff1c1c1e)

    fun getSupportedFeatures(): String {
        val json = Json { encodeDefaults = true }
        val supportedFeatures = listOf(
            SupportedFeature(name = "openEmailInbox", enabled = "true"),
            SupportedFeature(name = "can_receive_event_messages", enabled = "true")
        )

        return json.encodeToString(supportedFeatures)
    }
}