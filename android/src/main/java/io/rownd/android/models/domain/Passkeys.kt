package io.rownd.android.models.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
data class PasskeysState @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("is_initialized")
    @JsonNames("isInitialized")
    val isInitialized: Boolean = false,
    val registrations: List<PasskeyRegistration> = emptyList(),
)

@Serializable
data class PasskeyRegistration(
    var id: String = "",
    @SerialName("user_agent")
    var userAgent: String? = null
)