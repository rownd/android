package io.rownd.android.models.domain

// import io.rownd.android.RowndSignInMethods
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
        val rphInit = SignInInitObj(
            lastSignIn,
            lastSignInDate
        )

        val encoded = json.encodeToString(SignInInitObj.serializer(), rphInit)
        return encoded.toByteArray().toBase64()
    }
}

@Serializable
data class SignInInitObj(
    @SerialName("last_sign_in")
    val lastSignIn: String?,
    @SerialName("last_sign_in_date")
    val lastSignInDate: String?,
)