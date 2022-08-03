package io.rownd.android.models.repos

import android.util.Log
import com.auth0.android.jwt.JWT
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.network.AuthApi
import io.rownd.android.models.network.TokenRequestBody
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthRepo {
//    internal val _authState = MutableStateFlow(AuthState())
//    val state: StateFlow<AuthState> get() = _authState

    companion object {
        internal suspend fun getAccessToken(): String? {
            var accessToken = StateRepo.getStore().currentState.auth.accessToken
                ?: // TODO: Throw error because there is no access token?
                return null

            val jwt = JWT(accessToken)

            val refreshToken = StateRepo.getStore().currentState.auth.refreshToken
            if (jwt.isExpired(0) && refreshToken != null) {
                return fetchTokenAsync(TokenRequestBody(refreshToken)).await()
            }

            return accessToken
        }

        @Synchronized
        private fun fetchTokenAsync(tokenRequest: TokenRequestBody): Deferred<String?> {
            return CoroutineScope(Dispatchers.IO).async {
                val result = AuthApi.client.exchangeToken(tokenRequest)
                    .onSuccess {
                        StateRepo.getStore().dispatch(StateAction.SetAuth(it.asDomainModel()))
                    }
                    .onFailure {
                        Log.e("RowndApi", "Oh no! Request failed! ${it.message}")
                    }

                if (result.isSuccess) {
                    val authState = result.getOrNull()
                    return@async authState?.accessToken
                } else {
                    return@async null
                }
            }
        }
    }
}