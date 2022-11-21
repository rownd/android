package io.rownd.android.models.domain

import android.util.Log
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.Encryption
import kotlinx.serialization.Serializable
import io.rownd.android.models.network.User as NetworkUser

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = HashMap<String, Any?>(),
    val redacted: MutableList<String> = mutableListOf()
) {
    fun asNetworkModel(stateRepo: StateRepo, userRepo: UserRepo): NetworkUser {
        return NetworkUser(
            data = dataAsEncrypted(stateRepo, userRepo),
            redacted = redacted
        )
    }

    internal fun dataAsEncrypted(stateRepo: StateRepo, userRepo: UserRepo): Map<String, Any?> {
        val encKeyId = userRepo.ensureEncryptionKey(this) ?: return data

        val data = data.toMutableMap()

        // Encrypt user fields
        for (entry in data.entries) {
            val (key, _) = entry
            if (stateRepo.state.value.appConfig.schema[key]?.encryption?.state == AppSchemaEncryptionState.Enabled && entry.value is String) {
                val value = entry.value as String
                try {
                    val encrypted: String = Encryption.encrypt(value, encKeyId)
                    data[key] = encrypted
                } catch (error: Exception) {
                    Log.d(
                        "RowndUserNetwork",
                        "Failed to encrypt user data value. Error: ${error.message}"
                    )
                }
            }
        }
        return data
    }
}