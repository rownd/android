package io.rownd.android.models.network

import io.rownd.android.models.domain.AuthState
import io.rownd.android.util.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET

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

interface TokenService {
    @GET("hub/auth/token")
    suspend fun exchangeToken() : Result<Auth>
}

object AuthApi {
    val client: TokenService = ApiClient.getInstance().create(TokenService::class.java)
}