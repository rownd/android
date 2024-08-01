package io.rownd.android.models

import io.rownd.android.RowndSignInOptionsBase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class PasskeyStatus {
    @SerialName("loading")
    Loading,
    @SerialName("success")
    Success,
    @SerialName("failed")
    Failed,
}

@Serializable
enum class BiometricType() {
    @SerialName("touchID")
    Touch
}

@Serializable
enum class AuthenticatorType {
    @SerialName("passkey")
    Passkey
}

@Serializable
data class RowndAuthenticatorRegistrationOptions(
    var status: PasskeyStatus? = null,
    @SerialName("biometric_type")
    var biometricType: BiometricType = BiometricType.Touch,
    var type: AuthenticatorType = AuthenticatorType.Passkey,
    @SerialName("error")
    var error: String? = null
): RowndSignInOptionsBase() {
    override fun toJsonString(): String {
        return json.encodeToString(serializer(), this)
    }
}