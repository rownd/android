package io.rownd.android.models.domain

import io.rownd.android.Rownd
import io.rownd.android.models.json
import io.rownd.android.util.toBase64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames


@Serializable
data class SignInState @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("last_sign_in")
    @JsonNames("lastSignIn")
    val lastSignIn: String? = null,
    @SerialName("last_sign_in_date")
    @JsonNames("lastSignInDate")
    val lastSignInDate: String? = null,
) {

    internal fun toSignInInitHash(): String {
        var activeAccounts = mutableListOf<ActiveAccount>();
        for (account in Rownd.getActiveGmailAccounts()) {
            activeAccounts.add(ActiveAccount(email = account.name))
        }

        val rphInit = SignInInitHashObj(lastSignIn, lastSignInDate, android = SignInInitHashAndroidObj(activeAccounts))

        val encoded = json.encodeToString(SignInInitHashObj.serializer(), rphInit)
        return encoded.toByteArray().toBase64()
    }
}

@Serializable
data class ActiveAccount(
    @SerialName("email")
    val email: String
)

@Serializable
data class SignInInitObj(
    @SerialName("last_sign_in")
    val lastSignIn: String?,
    @SerialName("last_sign_in_date")
    val lastSignInDate: String?,
)

@Serializable
data class SignInInitHashObj(
    @SerialName("last_sign_in")
    val lastSignIn: String?,
    @SerialName("last_sign_in_date")
    val lastSignInDate: String?,
    @SerialName("android")
    val android: SignInInitHashAndroidObj,
)

@Serializable
data class SignInInitHashAndroidObj(
    @SerialName("active_accounts")
    val activeAccounts: List<ActiveAccount>
)