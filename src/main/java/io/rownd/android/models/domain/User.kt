package io.rownd.android.models.domain

import android.util.Log
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.models.network.User as NetworkUser
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.util.Encryption
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val data: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = HashMap<String, Any?>(),
    val redacted: PersistentList<String> = persistentListOf()
) {
    fun asNetworkModel(): NetworkUser {
        return NetworkUser(
            data = dataAsEncrypted(),
            redacted = redacted
        )
    }

    internal fun dataAsEncrypted(): Map<String, Any?> {
        val encKeyId = UserRepo.ensureEncryptionKey(this) ?: return data

        val data = data.toMutableMap()

        // Encrypt user fields
        for (entry in data.entries) {
            val (key, _) = entry
            if (StateRepo.state.value.appConfig.schema[key]?.encryption?.state == AppSchemaEncryptionState.Enabled && entry.value is String) {
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