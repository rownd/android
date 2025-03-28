package io.rownd.android.models.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true }

@Serializable
internal data class APIError(
    var error: String? = null,
    var message: String,
    var messages: List<String>? = null
)
