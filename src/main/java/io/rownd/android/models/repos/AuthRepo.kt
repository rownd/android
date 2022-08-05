package io.rownd.android.models.repos

import android.util.Log
import com.auth0.android.jwt.JWT
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.network.Auth
import io.rownd.android.models.network.AuthApi
import io.rownd.android.models.network.RowndAPIException
import io.rownd.android.models.network.TokenRequestBody
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AuthRepo {
    companion object {
        internal suspend fun getAccessToken(): String? {
            val accessToken = StateRepo.getStore().currentState.auth.accessToken
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
                val resp = AuthApi.client.exchangeToken(tokenRequest)
                if (resp.isSuccessful) {
                    val authBody = resp.body() as Auth
                    StateRepo.getStore().dispatch(StateAction.SetAuth(authBody.asDomainModel()))
                    authBody.accessToken
                } else {
                    val error = RowndAPIException(resp)
                    Log.e("RowndAuthApi", "Fetching token failed: ${error.message}")
                    null
                }
            }
        }
    }
}