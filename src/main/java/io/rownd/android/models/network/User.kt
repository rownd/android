package io.rownd.android.models.network

import io.rownd.android.models.domain.User as DomainUser
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.ApiClient
import io.rownd.android.util.RequireAccessToken
import kotlinx.collections.immutable.toPersistentList
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>,
    val redacted: List<String>
) {
    fun asDomainModel() : DomainUser {
        return DomainUser(
            data = data,
            redacted = redacted.toPersistentList()
        )
    }
}

//@Serializable
//data class UserRequestBody(
//    val data: String
//)

interface UserService {
    @RequireAccessToken
    @GET("me/applications/{app}/data")
    suspend fun fetchUser(@Path("app") appId: String) : Result<User>

    @RequireAccessToken
    @POST("me/applications/{app}/data")
    suspend fun saveUser(@Path("app") appId: String, @Body requestBody: User) : Result<User>
}

object UserApi {
    internal val client: UserService = ApiClient.getInstance().create(UserService::class.java)
}