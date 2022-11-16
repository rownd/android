package io.rownd.android.models.network

import io.ktor.resources.*
import io.rownd.android.models.domain.AuthState
import io.rownd.android.util.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
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
)

interface TokenService {
    @POST("hub/auth/token")
    suspend fun exchangeToken(@Body requestBody: TokenRequestBody) : Response<Auth>
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
