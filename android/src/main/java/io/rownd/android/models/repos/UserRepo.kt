package io.rownd.android.models.repos

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.rownd.android.models.domain.User
import io.rownd.android.util.AuthenticatedApi
import io.rownd.android.util.Encryption
import io.rownd.android.util.EncryptionException
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.set
import io.rownd.android.models.network.User as NetworkUser

@Singleton
class UserRepo @Inject constructor(val stateRepo: StateRepo, private val rowndContext: RowndContext) {

//    @Inject
//    lateinit var userApi: UserApi

    internal val userApi: AuthenticatedApi by lazy { AuthenticatedApi(rowndContext) }

    internal fun loadUserAsync(): Deferred<User?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                val user: NetworkUser = userApi.client.get("me/applications/${stateRepo.state.value.appConfig.id}/data").body()
                Log.i("RowndUsersApi", "Successfully loaded user data: $user")
                stateRepo.getStore().dispatch(StateAction.SetUser(user.asDomainModel(stateRepo, this@UserRepo)))
                return@async user.asDomainModel(stateRepo, this@UserRepo)
            } catch (ex: ClientRequestException) {
                Log.e("RowndUsersApi", "Failed to fetch the user: ${ex.message}")

                if (ex.response.status == HttpStatusCode.NotFound) {
                    // This user doesn't exist
                    rowndContext.client?.signOut()
                }

                return@async null
            } catch (ex: Exception) {
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
                val savedUser: NetworkUser =
                    userApi.client.put("me/applications/${stateRepo.state.value.appConfig.id}/data") {
                        setBody(
                            networkUser
                        )
                    }.body()
                Log.i("RowndUsersApi", "Successfully saved user data: $user")
                stateRepo.getStore().dispatch(StateAction.SetUser(savedUser.asDomainModel(stateRepo, this@UserRepo)))
                return@async savedUser.asDomainModel(stateRepo, this@UserRepo)
            } catch (ex: Exception) {
                Log.e("RowndUsersApi", "Failed to save the user: ${ex.message}")
                throw RowndException("Failed to save the user: ${ex.message}")
            }
        }
    }

    @Serializable
    data class UserMetaDataResponse constructor(
        val id: String = "",
        val meta: Map<String, @Serializable(with = AnyValueSerializer::class) Any?> = HashMap<String, Any?>()
    )

    internal fun saveMetaUserAsync(user: User): Deferred<User?> {
        // Create network user based on domain user
        val networkUser = user.asNetworkModel(stateRepo, this)
        return CoroutineScope(Dispatchers.IO).async {
            try {
                val savedMetaData: UserMetaDataResponse =
                    userApi.client.put("me/meta") {
                        setBody(
                            networkUser
                        )
                    }.body()
                Log.i("RowndUsersApi", "Successfully saved user meta data: $savedMetaData")
                val existingUser = stateRepo.state.value.user
                val updatedUser = existingUser.copy(
                    data = existingUser.data,
                    meta = savedMetaData.meta
                )
                stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))
                return@async updatedUser
            } catch (ex: Exception) {
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

    fun set(data: Map<String, Any>) {
        val updatedUser = User(
            data = data,
            meta = stateRepo.state.value.user.meta
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

    fun setMetaData(meta: Map<String, Any>) {
        val updatedUser = User(
            data = stateRepo.state.value.user.data,
            meta = meta
        )
        stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))

        saveMetaUserAsync(updatedUser)
    }

    fun setMetaData(field: String, data: Any) {
        val existingUser = stateRepo.state.value.user
        val userMetaData = existingUser.meta.toMutableMap()
        userMetaData[field] = data
        val updatedUser = existingUser.copy(
            meta = userMetaData
        )
        stateRepo.getStore().dispatch(StateAction.SetUser(updatedUser))

        saveMetaUserAsync(updatedUser)
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