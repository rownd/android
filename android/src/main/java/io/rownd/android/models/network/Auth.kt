package io.rownd.android.models.network

import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInUserType
import io.rownd.android.models.domain.AuthState
import io.rownd.android.util.AuthenticatedApi
import io.rownd.android.util.RowndContext
import io.rownd.android.util.TokenApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class Auth(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("is_verified_user")
    val isVerifiedUser: Boolean = false
) {
    fun asDomainModel(): AuthState {
        return AuthState(
            accessToken = accessToken,
            refreshToken = refreshToken,
            isVerifiedUser = isVerifiedUser
        )
    }
}

@Serializable
data class TokenRequestBody internal constructor(
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("id_token")
    val idToken: String? = null,
    @SerialName("app_id")
    val appId: String? = null,
    @SerialName("intent")
    val intent: RowndSignInIntent? = null,
    @SerialName("instant_user_id")
    var instantUserId: String? = null,
)

@Serializable
data class TokenResponse internal constructor(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("refresh_token")
    val refreshToken: String? = null,
    @SerialName("user_type")
    val userType: RowndSignInUserType? = null,
    @SerialName("app_variant_user_type")
    val appVariantUserType: RowndSignInUserType? = null,
)

@Serializable
data class SignOutRequestBody internal constructor(
    @SerialName("sign_out_all")
    val signOutAll: Boolean
)

@Serializable
data class SignOutResponse internal constructor(
    @SerialName("sign_out_all")
    val signOutAll: Boolean
)

class AuthApi @Inject constructor(rowndContext: RowndContext) {
    private val tokenApi: TokenApi by lazy { TokenApi(rowndContext) }
    private val authApi: AuthenticatedApi by lazy { AuthenticatedApi(rowndContext) }

    suspend fun exchangeToken(requestBody: TokenRequestBody) : TokenResponse {
        return tokenApi.client.post("hub/auth/token") {
            setBody(requestBody)
        }.body()
    }

    suspend fun signOutUser(appId: String, requestBody: SignOutRequestBody) : SignOutResponse {
        return authApi.client.post("me/applications/$appId/signout") {
            setBody(requestBody)
        }.body()
    }
}
