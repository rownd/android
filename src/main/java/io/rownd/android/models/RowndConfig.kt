package io.rownd.android.models

import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }

@Serializable
data class RowndConfig(
    var appKey: String? = null,
    var baseUrl: String = "https://hub.rownd.io",
    var apiUrl: String = "https://api.rownd.io"
) {
    fun hubLoaderUrl(): String {
        val jsonConfig = json.encodeToString(RowndConfig.serializer(), this)
        val base64Config = Base64.encodeToString(jsonConfig.encodeToByteArray(), Base64.DEFAULT)
        return "$baseUrl/mobile_app?config=$base64Config"
    }
}

