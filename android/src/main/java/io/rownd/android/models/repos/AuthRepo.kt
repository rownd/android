package io.rownd.android.models.repos

import android.util.Log
import com.auth0.android.jwt.JWT
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.opentelemetry.api.trace.StatusCode
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInJsOptions
import io.rownd.android.RowndSignInLoginStep
import io.rownd.android.RowndSignInUserType
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.Auth
import io.rownd.android.models.network.SignOutRequestBody
import io.rownd.android.models.network.SignOutResponse
import io.rownd.android.models.network.TokenRequestBody
import io.rownd.android.models.network.TokenResponse
import io.rownd.android.util.AuthLevel
import io.rownd.android.util.AuthenticatedApiClient
import io.rownd.android.util.InvalidRefreshTokenException
import io.rownd.android.util.NetworkConnectionFailureException
import io.rownd.android.util.NoRefreshTokenPresentException
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndException
import io.rownd.android.util.ServerException
import io.rownd.android.util.TokenApiClient
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
class AuthRepo @Inject constructor() {
    @Inject
    lateinit var rowndContext: RowndContext

    @Inject
    lateinit var stateRepo: StateRepo

    @Inject
    lateinit var userRepo: UserRepo

    @Inject
    lateinit var signInRepo: SignInRepo

    @Inject
    lateinit var authenticatedApiClient: AuthenticatedApiClient

    @Inject
    lateinit var tokenApiClient: TokenApiClient

    private var refreshTokenJob: Deferred<Auth?>? = null

    internal suspend fun getLatestAuthState(): AuthState? {
        if (refreshTokenJob is Deferred<Auth?>) {
            val resp = (refreshTokenJob as Deferred<Auth?>).await()
            return resp?.asDomainModel()
        }

        val authState = stateRepo.getStore().currentState.auth
        val accessToken = authState.accessToken ?: return null

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

    internal suspend fun getAccessToken(idToken: String, intent: RowndSignInIntent?, type: AccessTokenType): TokenResponse? {
        val appId = stateRepo.getStore().currentState.appConfig.id
        val tokenRequest = TokenRequestBody(
            appId = appId,
            idToken = idToken,
            intent = intent
        )

        if (stateRepo.state.value.user.authLevel == AuthLevel.Instant) {
            tokenRequest.instantUserId = stateRepo.state.value.user.data["user_id"]?.toString()
        }

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
    @Throws(RowndException::class)
    internal fun signOutUserAsync(appId: String, signOutRequest: SignOutRequestBody): Deferred<SignOutResponse?>{
        return CoroutineScope(Dispatchers.IO).async {
            try {
                signOutUser(appId, signOutRequest)
                Rownd.signOut()
                return@async null
            } catch(ex: Exception) {
                Log.e("Rownd.Auth", "Failed to sign out user from all sessions:", ex)
                throw RowndException("Failed to sign out user from all sessions: ${ex.message}")
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
            val span = rowndContext.telemetry?.startSpan("refreshTokenAsync")
            val refreshToken = stateRepo.state.value.auth.refreshToken ?: throw NoRefreshTokenPresentException("No refresh token was available")
            val jwt = JWT(refreshToken)

            try {
                val authState: Auth = tokenApiClient.client.post("/hub/auth/token") {
                    setBody(
                        TokenRequestBody(
                            refreshToken = refreshToken
                        )
                    )
                }.body()

                stateRepo.getStore().dispatch(StateAction.SetAuth(authState.asDomainModel()))

                Log.d("Rownd.Auth", "Refreshing tokens: complete")
                span?.setStatus(StatusCode.OK)
                refreshTokenJob = null
                return@async authState
            } catch (ex: ClientRequestException) {
                span?.recordException(ex)
                span?.setStatus(StatusCode.ERROR, ex.message)
                span?.setAttribute("http_status_code", ex.response.status.value.toString())
                jwt.id?.let { span?.setAttribute("jti", it) }
                jwt.subject?.let { span?.setAttribute("subject", it) }

                if (ex.response.status != HttpStatusCode.BadRequest) {
                    throw ServerException(ex.message)
                }

                Log.e(
                    "Rownd.AuthRepo",
                    "Failed to refresh tokens, likely because it has already been consumed:",
                    ex
                )
                refreshTokenJob = null

                // Sign out on HTTP 400s
                stateRepo.getStore().dispatch(StateAction.SetAuth(AuthState()))
                stateRepo.getStore().dispatch(StateAction.SetUser(User()))

                span?.addEvent("Permanent: Refresh token failure. User was signed out.")
                span?.setAttribute("error", "invalid_refresh_token")
                throw InvalidRefreshTokenException(ex.message)
            } catch (ex: ServerResponseException) {
                span?.recordException(ex)
                span?.setStatus(StatusCode.ERROR, ex.message)
                span?.setAttribute("http_status_code", ex.response.status.value.toString())
                jwt.id?.let { span?.setAttribute("jti", it) }
                jwt.subject?.let { span?.setAttribute("subject", it) }

                throw ServerException(ex.message)
            } catch (ex: Exception) {
                refreshTokenJob = null
                Log.e("Rownd.AuthRepo", "Failed to refresh tokens:", ex)
                span?.recordException(ex)
                span?.setStatus(StatusCode.ERROR, ex.message ?: "An unknown refresh token error occurred.")
                jwt.id?.let { span?.setAttribute("jti", it) }
                jwt.subject?.let { span?.setAttribute("subject", it) }

                throw NetworkConnectionFailureException(ex.message ?: "An unknown refresh token error occurred.")
            } finally {
                span?.end()
            }
        }

        return refreshTokenJob as Deferred<Auth?>
    }

    @Synchronized
    internal fun fetchTokenAsync(tokenRequest: TokenRequestBody, intent: RowndSignInIntent?, type: AccessTokenType): Deferred<TokenResponse?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                val tokenResponse = exchangeToken(tokenRequest)

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
                            userType = tokenResponse.userType,
                            appVariantUserType = tokenResponse.appVariantUserType
                        )
                    )
                }

                if (type === AccessTokenType.google) {
                    signInRepo.setLastSignInMethod("google")
                }

                stateRepo.getStore().dispatch(
                    StateAction.SetAuth(
                        AuthState(
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken
                        )
                    )
                )
                userRepo?.loadUserAsync()
                return@async tokenResponse
            } catch (ex: ClientRequestException) {
                Log.e("RowndAuthApi", "Fetching token failed: ${ex.message}")
                if (ex.response.status == HttpStatusCode.BadRequest) {
                    // The token refresh failed, so we need to sign-out
                    Rownd.signOut()
                    throw InvalidRefreshTokenException(ex.message)
                }

                throw ServerException(ex.message)
                return@async null
            } catch (ex: ServerResponseException) {
                throw ServerException(ex.message)
                return@async null
            } catch (ex: Exception) {
                return@async null
            }
        }
    }

    internal fun isJwtExpiredWithMargin(jwt: JWT): Boolean {
        if (jwt.expiresAt == null) {
            return false
        }

        val currentTime = rowndContext.kronosClock?.getCurrentTimeMs() ?: System.currentTimeMillis()
        val currentDateWithMargin = Date(currentTime + (60 * 1000)) // Add 60 sec margin to current Date

        return currentDateWithMargin.after(jwt.expiresAt)
    }

    suspend fun exchangeToken(requestBody: TokenRequestBody) : TokenResponse {
        return tokenApiClient.client.post("hub/auth/token") {
            setBody(requestBody)
        }.body()
    }

    suspend fun signOutUser(appId: String, requestBody: SignOutRequestBody) : SignOutResponse {
        return authenticatedApiClient.client.post("me/applications/$appId/signout") {
            setBody(requestBody)
        }.body()
    }
}