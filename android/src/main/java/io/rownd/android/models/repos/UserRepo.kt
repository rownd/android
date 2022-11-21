package io.rownd.android.models.repos

import android.util.Log
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.UserApi
import io.rownd.android.util.Encryption
import io.rownd.android.util.EncryptionException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepo @Inject constructor(val stateRepo: StateRepo) {

    @Inject
    lateinit var userApi: UserApi

    internal fun loadUserAsync(): Deferred<User?> {
        return CoroutineScope(Dispatchers.IO).async {
            val result = userApi.client.fetchUser(stateRepo.state.value.appConfig.id)
                .onSuccess {
                    Log.i("RowndUsersApi", "Successfully loaded user data: $it")
                    stateRepo.getStore().dispatch(StateAction.SetUser(it.asDomainModel(stateRepo, this@UserRepo)))
                }
                .onFailure {
                    Log.e("RowndUsersApi", "Failed to fetch the user: ${it.message}")
                }

            return@async result.getOrNull()?.asDomainModel(stateRepo, this@UserRepo)
        }
    }

    internal fun saveUserAsync(user: User): Deferred<User?> {
        // Create network user based on domain user
        val networkUser = user.asNetworkModel(stateRepo, this)
        return CoroutineScope(Dispatchers.IO).async {
            val result = userApi.client.saveUser(stateRepo.state.value.appConfig.id, networkUser)
                .onSuccess {
                    Log.i("RowndUsersApi", "Successfully saved user data: $it")
                    stateRepo.getStore().dispatch(StateAction.SetUser(it.asDomainModel(stateRepo, this@UserRepo)))
                }
                .onFailure {
                    Log.e("RowndUsersApi", "Failed to save the user: ${it.message}")
                }

            return@async result.getOrNull()?.asDomainModel(stateRepo, this@UserRepo)
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

    fun set(data: Map<String, Any>) {
        val updatedUser = User(
            data = data
        )
        stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))
        saveUserAsync(updatedUser)
    }

    fun set(field: String, data: Any) {
        val existingUser = stateRepo.state.value.user
        val userData = existingUser.data.toMutableMap()
        userData[field] = data
        val updatedUser = existingUser.copy(
            data = userData
        )
        stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))
        saveUserAsync(updatedUser)
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