package io.rownd.android.models.repos

import android.util.Log
import io.rownd.android.Rownd
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.AuthApi
import io.rownd.android.models.network.TokenRequestBody
import io.rownd.android.models.network.UserApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async

class UserRepo {
    companion object {
        internal fun loadUserAsync(): Deferred<User?> {
            return CoroutineScope(Dispatchers.IO).async {
                val result = UserApi.client.fetchUser(Rownd.store.currentState.appConfig.id)
                    .onSuccess {
                        Log.i("RowndUsersApi", "Successfully loaded user data: $it")
                        StateRepo.getStore().dispatch(StateAction.SetUser(it.asDomainModel()))
                    }
                    .onFailure {
                        Log.e("RowndUsersApi", "Failed to fetch the user: ${it.message}")
                    }

                    return@async result.getOrNull()?.asDomainModel()
            }
        }
    }
}