package io.rownd.android.models

import android.util.Base64
import android.util.Log
import io.rownd.android.Rownd.store
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val json = Json { encodeDefaults = true }

@Serializable
data class RowndConfig(
    var appKey: String? = null,
    var baseUrl: String = "https://hub.rownd.io",
    var apiUrl: String = "https://api.rownd.io",
    var postSignInRedirect: String? = null
) {
    fun hubLoaderUrl(): String {
        val jsonConfig = json.encodeToString(RowndConfig.serializer(), this)
        val base64Config = Base64.encodeToString(jsonConfig.encodeToByteArray(), Base64.DEFAULT)

        var rphInit: String? = ""
        try {
            val rphInitStr = store.currentState.auth.toRphInitHash()
            rphInit = "#rph_init=$rphInitStr"
        } catch (error: Exception) {
            Log.d("Rownd.config", "Couldn't compute requested init hash: ${error.message}")
        }

        return "$baseUrl/mobile_app?config=$base64Config$rphInit"
    }
}

