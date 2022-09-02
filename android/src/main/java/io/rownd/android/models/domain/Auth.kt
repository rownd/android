package io.rownd.android.models.domain

import android.util.Base64
import android.util.Log
import androidx.datastore.preferences.protobuf.ByteString
import io.rownd.android.Rownd.store
import io.rownd.android.models.json
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.toBase64
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
data class AuthState(
    val isLoading: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isVerifiedUser: Boolean = false,

    @Transient
    val isAuthenticated: Boolean = accessToken != null
) {
    internal fun toRphInitHash(): String? {
        val userId: String? = UserRepo.get("user_id") as? String

        val rphInit = RphInitObj(
            accessToken,
            refreshToken,
            store.currentState.appConfig.id,
            userId
        )

        val encoded = json.encodeToString(RphInitObj.serializer(), rphInit)
        return encoded.toByteArray().toBase64()
    }
}

@Serializable
data class RphInitObj(
    @SerialName("access_token")
    val accessToken: String?,
    @SerialName("refresh_token")
    val refreshToken: String?,
    @SerialName("app_id")
    val appId: String?,
    @SerialName("app_user_id")
    val appUserId: String?
)