package io.rownd.android.models.repos

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.rownd.android.models.domain.User
import io.rownd.android.util.AuthenticatedApiClient
import io.rownd.android.util.Encryption
import io.rownd.android.util.EncryptionException
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set
import io.rownd.android.models.network.User as NetworkUser

@Singleton
class UserRepo @Inject constructor() {
    @Inject
    lateinit var rowndContext: RowndContext

    @Inject
    lateinit var stateRepo: StateRepo

    @Inject
    lateinit var authenticatedApiClient: AuthenticatedApiClient

    internal fun setIsLoading(value: Boolean) {
        stateRepo.getStore().dispatch(StateAction.SetUserIsLoading(value))
    }

    internal fun loadUserAsync(): Deferred<User?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                setIsLoading(value = true)
                val user: NetworkUser = authenticatedApiClient.client.get("me/applications/${stateRepo.state.value.appConfig.id}/data").body()
                Log.i("RowndUsersApi", "Successfully loaded user data: $user")
                stateRepo.getStore().dispatch(StateAction.SetUser(user.asDomainModel(stateRepo, this@UserRepo)))
                setIsLoading(value = false)
                return@async user.asDomainModel(stateRepo, this@UserRepo)
            } catch (ex: ClientRequestException) {
                setIsLoading(value = false)
                Log.e("RowndUsersApi", "Failed to fetch the user: ${ex.message}")

                if (ex.response.status == HttpStatusCode.NotFound) {
                    // This user doesn't exist
                    rowndContext.client?.signOut()
                }

                return@async null
            } catch (ex: Exception) {
                setIsLoading(value = false)
                Log.e("RowndUsersApi", "Failed to fetch the user: ${ex.message}")
                return@async null
            }
        }
    }

    internal fun saveUserAsync(user: User): Deferred<User?> {
        // Create network user based on domain user
        val networkUser = user.asNetworkModel(stateRepo, this)
        return CoroutineScope(Dispatchers.IO).async {
            try {
                setIsLoading(value = true)
                val savedUser: NetworkUser =
                    authenticatedApiClient.client.put("me/applications/${stateRepo.state.value.appConfig.id}/data") {
                        setBody(
                            networkUser
                        )
                    }.body()
                Log.i("RowndUsersApi", "Successfully saved user data: $user")
                stateRepo.getStore().dispatch(StateAction.SetUser(savedUser.asDomainModel(stateRepo, this@UserRepo)))
                setIsLoading(value = false)
                return@async savedUser.asDomainModel(stateRepo, this@UserRepo)
            } catch (ex: Exception) {
                setIsLoading(value = false)
                Log.e("RowndUsersApi", "Failed to save the user: ${ex.message}")
                throw RowndException("Failed to save the user: ${ex.message}")
            }
        }
    }

    fun get(): User {
        return stateRepo.state.value.user
    }

    fun <T> get(field: String): T? {
        val value = stateRepo.state.value.user.data[field] ?: return null

        return try {
            value as T
        } catch (error: Exception) {
            null
        }
    }

    fun set(data: Map<String, Any>): Deferred<User?> {
        val updatedUser = User(
            data = data
        )
        stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))

        return saveUserAsync(updatedUser)
    }

    fun set(field: String, data: Any): Deferred<User?> {
        val existingUser = stateRepo.state.value.user
        val userData = existingUser.data.toMutableMap()
        userData[field] = data
        val updatedUser = existingUser.copy(
            data = userData
        )
        stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))
        return saveUserAsync(updatedUser)
    }

    fun getKeyId(user: User): String {
        return get("user_id")
            ?: throw EncryptionException("An encryption key was requested, but the user has not been loaded yet. Are you signed in?")
    }

    internal fun ensureEncryptionKey(user: User): String? {
        try {
            val keyId = getKeyId(user)

            var key = Encryption.loadKey(keyId)

            if (key == null) {
                key = Encryption.generateKey()
                Encryption.storeKey(key, keyId)
                return keyId
            }

            return keyId
        } catch (error: Exception) {
            Log.e(
                "RowndUser",
                "Failed to ensure that an encryption key exists: ${error.message}"
            )
            return null
        }
    }

    fun isEncryptionPossible(): Boolean {
        try {
            val key = Encryption.loadKey(getKeyId(stateRepo.state.value.user)) ?: return false

            return true
        } catch (error: Exception) {
            return false
        }
    }

}