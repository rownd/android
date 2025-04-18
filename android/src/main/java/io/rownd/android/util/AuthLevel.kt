package io.rownd.android.util

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class AuthLevel {
    @SerialName("instant")
    Instant,
    @SerialName("guest")
    Guest,
    @SerialName("unverified")
    Unverified,
    @SerialName("verified")
    Verified
}