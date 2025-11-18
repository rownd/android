package io.rownd.android.models.domain

import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.AuthLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.rownd.android.models.network.User as NetworkUser

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = HashMap(),
    val redacted: MutableList<String> = mutableListOf(),
    val state: String? = "enabled",
    @SerialName("auth_level")
    val authLevel: AuthLevel? = AuthLevel.Unverified,
    var isLoading: Boolean = false
) {
    fun asNetworkModel(stateRepo: StateRepo, userRepo: UserRepo): NetworkUser {
        return NetworkUser(
            data = data,
            redacted = redacted,
            state = state,
            authLevel = authLevel,
        )
    }
}