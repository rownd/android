package io.rownd.android.models.network

import io.rownd.android.models.domain.PasskeyRegistration as DomainPasskeyRegistration
import io.rownd.android.util.ApiClient
import io.rownd.android.util.RequireAccessToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import javax.inject.Inject

@Serializable
data class PasskeyRegistration (
    var id: String = "",
    @SerialName("user_agent")
    var userAgent: String? = null
) {
    fun asDomainModel(): DomainPasskeyRegistration {
        return DomainPasskeyRegistration(
            id = id,
            userAgent = userAgent
        )
    }
}

@Serializable
data class PasskeyRegistrationResponse (
    var passkeys: List<PasskeyRegistration>? = null
)


interface PasskeysRegistrationService {
    @RequireAccessToken
    @GET("me/auth/passkeys")
    suspend fun fetchRegistration() : Result<PasskeyRegistrationResponse>
}

class PasskeysApi @Inject constructor(apiClient: ApiClient) {
    var apiClient: ApiClient

    init {
        this.apiClient = apiClient
    }

    internal val client: PasskeysRegistrationService by lazy {
        apiClient.client.get().create(PasskeysRegistrationService::class.java)
    }
}
