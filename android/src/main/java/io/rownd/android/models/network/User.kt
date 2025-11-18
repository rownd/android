package io.rownd.android.models.network

import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.AuthLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.rownd.android.models.domain.User as DomainUser

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>,
    val redacted: List<String> = listOf(),
    val state: String? = "enabled",
    @SerialName("auth_level")
    val authLevel: AuthLevel? = null
) {
    fun asDomainModel(stateRepo: StateRepo, userRepo: UserRepo): DomainUser {
        return DomainUser(
            data = data,
            redacted = redacted.toMutableList(),
            state = state,
            authLevel = authLevel
        )
    }
}
