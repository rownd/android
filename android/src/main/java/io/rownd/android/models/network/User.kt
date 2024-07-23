package io.rownd.android.models.network

import android.util.Log
import io.rownd.android.models.domain.AppSchemaEncryptionState
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.ApiClient
import io.rownd.android.util.Encryption
import io.rownd.android.util.RequireAccessToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import javax.inject.Inject
import io.rownd.android.models.domain.User as DomainUser

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>,
    val redacted: List<String> = listOf(),
    val state: String? = "enabled",
    @SerialName("auth_level")
    val authLevel: String? = null
) {
    fun asDomainModel(stateRepo: StateRepo, userRepo: UserRepo): DomainUser {
        return DomainUser(
            data = dataAsDecrypted(stateRepo, userRepo),
            redacted = redacted.toMutableList()
        )
    }

    internal fun dataAsDecrypted(stateRepo: StateRepo, userRepo: UserRepo): Map<String, Any?> {
        val encKeyId = userRepo.ensureEncryptionKey(DomainUser(data = data)) ?: return data

        val data = data.toMutableMap()

        // Decrypt user fields
        for (entry in data.entries) {
            val (key, _) = entry
            if (stateRepo.state.value.appConfig.schema[key]?.encryption?.state == AppSchemaEncryptionState.Enabled && entry.value is String) {
                val value = entry.value as String
                try {
                    val decrypted: String = Encryption.decrypt(value, encKeyId)
                    data[key] = decrypted
                } catch (error: Exception) {
                    Log.d(
                        "RowndUserNetwork",
                        "Failed to decrypt user data value. Error: ${error.message} ${error.stackTraceToString()}"
                    )
                }
            }
        }
        return data
    }
}

interface UserService {
    @RequireAccessToken
    @GET("me/applications/{app}/data")
    suspend fun fetchUser(@Path("app") appId: String): Result<User>

    @RequireAccessToken
    @POST("me/applications/{app}/data")
    suspend fun saveUser(@Path("app") appId: String, @Body requestBody: User): Result<User>
}

class UserApi @Inject constructor(apiClient: ApiClient) {
    private val apiClient: ApiClient

    init {
        this.apiClient = apiClient
    }

    internal val client: UserService by lazy {
        apiClient.client.get().create(UserService::class.java)
    }
}