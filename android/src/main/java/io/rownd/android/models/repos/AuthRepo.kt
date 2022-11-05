package io.rownd.android.models.repos

import android.util.Log
import com.auth0.android.jwt.JWT
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.Auth
import io.rownd.android.models.network.AuthApi
import io.rownd.android.models.network.RowndAPIException
import io.rownd.android.models.network.TokenRequestBody
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor() {
    @Inject
    lateinit var authApi: AuthApi

    @Inject
    lateinit var stateRepo: StateRepo

    private var refreshTokenJob: Deferred<Auth?>? = null

    internal suspend fun getAccessToken(): String? {
        if (refreshTokenJob is Deferred<Auth?>) {
            val resp = (refreshTokenJob as Deferred<Auth?>).await()
            return resp?.accessToken
        }

        val accessToken = stateRepo.getStore().currentState.auth.accessToken
            ?: return null

        val jwt = JWT(accessToken)

        if (jwt.isExpired(0)) {
            val resp = refreshTokenAsync().await()
            return resp?.accessToken
        }

        return accessToken
    }

    internal suspend fun getAccessToken(idToken: String): String? {
        val appId = stateRepo.getStore().currentState.appConfig.id
        val tokenRequest = TokenRequestBody(
            appId = appId,
            idToken = idToken
        )
        return fetchTokenAsync(tokenRequest).await()
    }

    @Synchronized
    internal fun refreshTokenAsync(): Deferred<Auth?> {
        if (refreshTokenJob is Deferred<Auth?>) {
            return refreshTokenJob as Deferred<Auth?>
        }

        refreshTokenJob = CoroutineScope(Dispatchers.IO).async {
            Log.d("Rownd.Auth", "Refreshing tokens via ${stateRepo.state.value.auth.refreshToken}")
            val resp = authApi.client.exchangeToken(TokenRequestBody(
                refreshToken = stateRepo.state.value.auth.refreshToken
            ))

            val body = resp.body()

            if (body !is Auth) {
                val err = resp.errorBody()
                Log.w("Rownd.AuthRepo", "Failed to refresh token: ${err?.string()}")
                refreshTokenJob = null

                stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState()))
                stateRepo.getStore().dispatch(StateAction.SetUser(User()))

                return@async null
            }

            val authState = resp.body() as Auth

            stateRepo.getStore().dispatch(StateAction.SetAuth(authState.asDomainModel()))

            Log.d("Rownd.Auth", "Refreshing tokens: complete")
            refreshTokenJob = null
            return@async authState
        }

        return refreshTokenJob as Deferred<Auth?>
    }

    @Synchronized
    internal fun fetchTokenAsync(tokenRequest: TokenRequestBody): Deferred<String?> {
        return CoroutineScope(Dispatchers.IO).async {
            val resp = authApi.client.exchangeToken(tokenRequest)
            if (resp.isSuccessful) {
                val authBody = resp.body() as Auth
                stateRepo.getStore().dispatch(StateAction.SetAuth(authBody.asDomainModel()))
                authBody.accessToken
            } else {
                val error = RowndAPIException(resp)
                Log.e("RowndAuthApi", "Fetching token failed: ${error.message}")
                if (resp.code() == 400) {
                    // The token refresh failed, so we need to sign-out
                    Rownd.signOut()
                }
                null
            }
        }
    }
}