package io.rownd.android.models.repos

import android.util.Log
import com.auth0.android.jwt.JWT
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.Auth
import io.rownd.android.models.network.AuthApi
import io.rownd.android.models.network.RowndAPIException
import io.rownd.android.models.network.TokenRequestBody
import io.rownd.android.util.RowndContext
import io.rownd.android.util.TokenApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor(rowndContext: RowndContext) {
    @Inject
    lateinit var authApi: AuthApi

    @Inject
    lateinit var stateRepo: StateRepo

    // TODO: Should be able to use Dagger to inject this
    private val tokenApi: TokenApi by lazy { TokenApi(rowndContext.config.apiUrl) }

    private var refreshTokenJob: Deferred<Auth?>? = null

    internal suspend fun getLatestAuthState(): AuthState? {
        if (refreshTokenJob is Deferred<Auth?>) {
            val resp = (refreshTokenJob as Deferred<Auth?>).await()
            return resp?.asDomainModel()
        }

        val authState = stateRepo.getStore().currentState.auth
        val accessToken = authState.accessToken
            ?: return null

        val jwt = JWT(accessToken)

        if (jwt.isExpired(0)) {
            val resp = refreshTokenAsync().await()
            return resp?.asDomainModel()
        }

        return authState
    }

    internal suspend fun getAccessToken(): String? {
        val authState = getLatestAuthState()
        return authState?.accessToken
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

            try {
                val authState: Auth = tokenApi.client.post("/hub/auth/token") {
                    setBody(
                        TokenRequestBody(
                            refreshToken = stateRepo.state.value.auth.refreshToken
                        )
                    )
                }.body()

                stateRepo.getStore().dispatch(StateAction.SetAuth(authState.asDomainModel()))

                Log.d("Rownd.Auth", "Refreshing tokens: complete")
                refreshTokenJob = null
                return@async authState
            } catch (ex: Exception) {
                Log.e("Rownd.AuthRepo", "Failed to refresh tokens:", ex)
//                Log.w("Rownd.AuthRepo", "Failed to refresh token: ${err?.string()}")
                refreshTokenJob = null

                stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState()))
                stateRepo.getStore().dispatch(StateAction.SetUser(User()))

                return@async null
            }
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