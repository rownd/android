package io.rownd.android.models.domain

import com.auth0.android.jwt.JWT
import io.rownd.android.Rownd
import io.rownd.android.models.json
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.toBase64
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames

@Serializable
data class AuthState @OptIn(ExperimentalSerializationApi::class) constructor(
    val isLoading: Boolean = false,
    @SerialName("access_token")
    @JsonNames("accessToken")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    @JsonNames("refreshToken")
    val refreshToken: String? = null,
    val isVerifiedUser: Boolean = false,

    @Transient
    val isAuthenticated: Boolean = accessToken != null
) {
    val isAccessTokenValid: Boolean
        get() {
            if (accessToken == null) {
                return false
            }

            val jwt = JWT(accessToken)

            return !jwt.isExpired(0)
        }

    internal fun toRphInitHash(userRepo: UserRepo): String {
        val userId: String? = userRepo.get("user_id") as? String

        val rphInit = RphInitObj(
            accessToken,
            refreshToken,
            Rownd.store.currentState.appConfig.id,
            userId
        )

        val encoded = json.encodeToString(RphInitObj.serializer(), rphInit)
        return encoded.toByteArray().toBase64()
    }
}

@Serializable
data class RphInitObj(
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("app_id")
    val appId: String?,
    @SerialName("app_user_id")
    val appUserId: String?
)