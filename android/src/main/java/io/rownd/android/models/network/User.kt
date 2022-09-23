package io.rownd.android.models.network

import android.util.Log
import io.rownd.android.models.domain.AppSchemaEncryptionState
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.ApiClient
import io.rownd.android.util.Encryption
import io.rownd.android.util.RequireAccessToken
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import io.rownd.android.models.domain.User as DomainUser

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>,
    val redacted: List<String> = listOf()
) {
    fun asDomainModel(): DomainUser {
        return DomainUser(
            data = dataAsDecrypted(),
            redacted = redacted.toMutableList()
        )
    }

    internal fun dataAsDecrypted(): Map<String, Any?> {
        val encKeyId = UserRepo.ensureEncryptionKey(DomainUser(data = data)) ?: return data

        val data = data.toMutableMap()

        // Decrypt user fields
        for (entry in data.entries) {
            val (key, _) = entry
            if (StateRepo.state.value.appConfig.schema[key]?.encryption?.state == AppSchemaEncryptionState.Enabled && entry.value is String) {
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

object UserApi {
    internal val client: UserService = ApiClient.getInstance().create(UserService::class.java)
}