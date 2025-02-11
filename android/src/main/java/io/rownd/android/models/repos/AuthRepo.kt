package io.rownd.android.models.repos

import android.util.Log
import com.auth0.android.jwt.JWT
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInJsOptions
import io.rownd.android.RowndSignInLoginStep
import io.rownd.android.RowndSignInUserType
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.Auth
import io.rownd.android.models.network.AuthApi
import io.rownd.android.models.network.RowndAPIException
import io.rownd.android.models.network.SignOutRequestBody
import io.rownd.android.models.network.SignOutResponse
import io.rownd.android.models.network.TokenRequestBody
import io.rownd.android.models.network.TokenResponse
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndException
import io.rownd.android.util.TokenApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepo @Inject constructor(private val rowndContext: RowndContext) {
    @Inject
    lateinit var authApi: AuthApi

    @Inject
    lateinit var stateRepo: StateRepo

    @Inject
    lateinit var userRepo: UserRepo

    @Inject
    lateinit var signInRepo: SignInRepo

    // TODO: Should be able to use Dagger to inject this
    private val tokenApi: TokenApi by lazy { TokenApi(rowndContext) }

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

        if (isJwtExpiredWithMargin(jwt)) {
            val resp = refreshTokenAsync().await()
            return resp?.asDomainModel()
        }

        return authState
    }

    internal suspend fun getAccessToken(): String? {
        val authState = getLatestAuthState()
        return authState?.accessToken
    }

    internal suspend fun getAccessToken(idToken: String): TokenResponse? {
        return getAccessToken(idToken, intent = null, type = AccessTokenType.default)
    }

    internal suspend fun getAccessToken(idToken: String, intent: RowndSignInIntent?, type: AccessTokenType ): TokenResponse? {
        val appId = stateRepo.getStore().currentState.appConfig.id
        val tokenRequest = TokenRequestBody(
            appId = appId,
            idToken = idToken,
            intent = intent
        )
        return fetchTokenAsync(tokenRequest, intent, type).await()
    }

    fun signOutUser() {
        val appId = stateRepo.getStore().currentState.appConfig.id
        val signOutRequest = SignOutRequestBody(
            signOutAll = true
        )
        signOutUserAsync(appId, signOutRequest)
    }

    @Serializable
    internal enum class AccessTokenType {
        @SerialName("default")
        default,
        @SerialName("google")
        google,
    }

    @Synchronized
    internal fun signOutUserAsync(appId: String, signOutRequest: SignOutRequestBody): Deferred<SignOutResponse?>{
        return CoroutineScope(Dispatchers.IO).async {
            val resp = authApi.client.signOutUser(appId, signOutRequest)
            if (resp.isSuccessful) {
                Rownd.signOut()
                return@async null
            } else {
                val error = RowndAPIException(resp)
                Log.e("Rownd.Auth", "Failed to sign out user from all sessions:", error)
                throw RowndException("Failed to sign out user from all sessions")
            }
        }
    }

    @Synchronized
    internal fun refreshTokenAsync(): Deferred<Auth?> {
        if (refreshTokenJob is Deferred<Auth?>) {
            return refreshTokenJob as Deferred<Auth?>
        }

        refreshTokenJob = CoroutineScope(Dispatchers.IO).async {
            Log.d("Rownd.Auth", "Refreshing tokens: in progress")

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
            } catch (ex: ClientRequestException) {
                if (ex.response.status != HttpStatusCode.BadRequest) {
                    throw RowndException(ex.message)
                }

                Log.e("Rownd.AuthRepo", "Failed to refresh tokens, likely because it has already been consumed:", ex)
                refreshTokenJob = null

                // Sign out on HTTP 400s
                stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState()))
                stateRepo.getStore().dispatch(StateAction.SetUser(User()))

                return@async null
            } catch (ex: Exception) {
                refreshTokenJob = null
                Log.e("Rownd.AuthRepo", "Failed to refresh tokens:", ex)
                throw RowndException(ex.message ?: "An unknown refresh token error occurred.")
            }
        }

        return refreshTokenJob as Deferred<Auth?>
    }

    @Synchronized
    internal fun fetchTokenAsync(tokenRequest: TokenRequestBody, intent: RowndSignInIntent?, type: AccessTokenType): Deferred<TokenResponse?> {
        return CoroutineScope(Dispatchers.IO).async {
            val resp = authApi.client.exchangeToken(tokenRequest)
            if (resp.isSuccessful) {
                val tokenResponse = resp.body() as TokenResponse
                if (type != AccessTokenType.default) {
                    if (tokenResponse.userType === RowndSignInUserType.NewUser && intent === RowndSignInIntent.SignIn) {
                        Rownd.requestSignIn(
                            RowndSignInJsOptions(
                                intent = intent,
                                loginStep = RowndSignInLoginStep.NoAccount,
                                token = tokenRequest.idToken
                            )
                        )
                        return@async null
                    }
                    Rownd.requestSignIn(
                        RowndSignInJsOptions(
                            intent = intent,
                            loginStep = RowndSignInLoginStep.Success,
                            userType = tokenResponse.userType
                        )
                    )
                }

                if (type === AccessTokenType.google) {
                    signInRepo.setLastSignInMethod("google")
                }

                stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState(accessToken = tokenResponse.accessToken, refreshToken = tokenResponse.refreshToken)))
                userRepo?.loadUserAsync()
                tokenResponse
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

    internal fun isJwtExpiredWithMargin(jwt: JWT): Boolean {
        if (jwt.expiresAt == null) {
            return false
        }

        val currentTime = rowndContext.kronosClock?.getCurrentTimeMs() ?: System.currentTimeMillis()
        val currentDateWithMargin = Date(currentTime + (60 * 1000)) //Add 60 secs to current Date

        return currentDateWithMargin.after(jwt.expiresAt)
    }
}