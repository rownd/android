package io.rownd.android.models.network

import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInUserType
import io.rownd.android.models.domain.AuthState
import io.rownd.android.util.ApiClient
import io.rownd.android.util.RequireAccessToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
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

interface TokenService {
    @POST("hub/auth/token")
    suspend fun exchangeToken(@Body requestBody: TokenRequestBody) : Response<TokenResponse>

    @RequireAccessToken
    @POST("me/applications/{app}/signout")
    suspend fun signOutUser(@Path("app") appId: String, @Body requestBody: SignOutRequestBody) : Response<SignOutResponse>
}

class AuthApi @Inject constructor(apiClient: ApiClient) {
    var apiClient: ApiClient

    init {
        this.apiClient = apiClient
    }

    internal val client: TokenService by lazy {
        apiClient.client.get().create(TokenService::class.java)
    }
}
