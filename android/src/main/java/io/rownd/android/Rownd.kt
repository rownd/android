@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Application
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import androidx.compose.material.ExperimentalMaterialApi
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.lyft.kronos.AndroidClockFactory
import dagger.Component
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.plugin
import io.rownd.android.authenticators.passkeys.PasskeysCommon
import io.rownd.android.models.AuthenticatorType
import io.rownd.android.models.RowndAuthenticatorRegistrationOptions
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.RowndConnectionAction
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.SignInLinkApi
import io.rownd.android.models.repos.AuthRepo
import io.rownd.android.models.repos.GlobalState
import io.rownd.android.models.repos.SignInRepo
import io.rownd.android.models.repos.StateAction
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.models.repos.dataStore
import io.rownd.android.util.ApiClientModule
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndEvent
import io.rownd.android.util.RowndEventEmitter
import io.rownd.android.util.RowndEventType
import io.rownd.android.util.RowndException
import io.rownd.android.views.HubComposableBottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.RowndWebViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.lang.ref.WeakReference
import java.util.UUID
import javax.inject.Singleton

// The default Rownd instance
val Rownd = RowndClient(DaggerRowndGraph.create())

@Singleton
@Component(modules = [ApiClientModule::class])
interface RowndGraph {
    fun stateRepo(): StateRepo
    fun userRepo(): UserRepo
    fun authRepo(): AuthRepo
    fun connectionAction(): RowndConnectionAction
    fun signInRepo(): SignInRepo
    fun signInLinkApi(): SignInLinkApi
    fun rowndContext(): RowndContext
    fun passkeyAuthenticator(): PasskeysCommon
    fun rowndEventEmitter(): RowndEventEmitter<RowndEvent>
    fun inject(rowndConfig: RowndConfig)
}

class RowndClient constructor(
    graph: RowndGraph,
    val config: RowndConfig = RowndConfig()
) {
    internal var appHandleWrapper: AppLifecycleListener? = null

    internal lateinit var store: Store<GlobalState, StateAction>

    internal var stateRepo: StateRepo = graph.stateRepo()
    internal var userRepo: UserRepo = graph.userRepo()
    internal var authRepo: AuthRepo = graph.authRepo()
    internal var signInRepo: SignInRepo = graph.signInRepo()
    internal var signInLinkApi: SignInLinkApi = graph.signInLinkApi()
    internal var rowndContext = graph.rowndContext()
    internal var passkeyAuthenticator = graph.passkeyAuthenticator()
    internal var connectionAction = graph.connectionAction()
    internal var eventEmitter = graph.rowndEventEmitter()

    var state = stateRepo.state
    var user = userRepo
    private var hasDisplayedOneTap = false
    private var isDisplayingOneTap = false
    private var userRequestedGoogleSignIn = false
    private var rememberedRequestSignIn: (() -> Unit)? = null
    private var googleSignInIntent: RowndSignInIntent? = null

    init {
        graph.inject(config)
        rowndContext.config = config
        rowndContext.client = this
        rowndContext.authRepo = authRepo
        rowndContext.store = stateRepo.getStore()
        rowndContext.eventEmitter = eventEmitter
        stateRepo.userRepo = userRepo
        stateRepo.authRepo = authRepo
    }

    private fun configure(appKey: String) {
        // Init NTP sync ASAP
        rowndContext.kronosClock = AndroidClockFactory.createKronosClock(
            appHandleWrapper?.app?.get()!!.applicationContext,
            ntpHosts = listOf("time.cloudflare.com")
        )
        rowndContext.kronosClock?.syncInBackground()

        config.appKey = appKey

        store = stateRepo.setup(appHandleWrapper?.app?.get()!!.applicationContext.dataStore)

        // Clear webview cache on startup
        Handler(Looper.getMainLooper()).post {
            WebView(appHandleWrapper?.app?.get()!!).clearCache(true)
        }

        // Webview holder in case of activity restarts during auth
        val hubViewModelFactory = RowndWebViewModel.Factory(appHandleWrapper?.app?.get()!!, this)
        appHandleWrapper?.registerActivityListener(
            persistentListOf(
                Lifecycle.State.CREATED
            ), true
        ) {
            if (it !is ViewModelStoreOwner) {
                return@registerActivityListener
            }
            rowndContext.hubViewModel = ViewModelProvider(
                it as ViewModelStoreOwner,
                hubViewModelFactory
            )[RowndWebViewModel::class.java]

            // Re-triggers the sign-in sheet in the event that the activity restarted during sign-in
            if ((rowndContext.hubViewModel)?.webView()?.value != null) {
                displayHub(HubPageSelector.Unknown)
            }
        }

        appHandleWrapper?.registerActivityListener(
            persistentListOf(
                Lifecycle.State.RESUMED
            ), true
        ) {
            signInLinkApi.signInWithLinkIfPresentOnIntentOrClipboard(it)
        }

        // Show the Google One Tap UI if applicable
        CoroutineScope(Dispatchers.IO).launch {
            store.stateAsStateFlow().collect {
                if (
                    !isDisplayingOneTap &&
                    !hasDisplayedOneTap &&
                    !it.auth.isLoading &&
                    !it.auth.isAuthenticated &&
                    it.appConfig.config.hub.auth.signInMethods.google.clientId != "" &&
                    it.appConfig.config.hub.auth.signInMethods.google.oneTap.mobileApp.autoPrompt
                ) {
                    isDisplayingOneTap = true
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            showGoogleOneTap()
                        },
                        it.appConfig.config.hub.auth.signInMethods.google.oneTap.mobileApp.delay.toLong()
                    )
                }
            }
        }
    }

    fun configure(app: Application, appKey: String) {
        _registerActivityLifecycle(app)
        configure(appKey)
    }

    // Used by Flutter and RN SDKs
    fun configure(activity: FragmentActivity, appKey: String) {
        _registerActivityLifecycle(activity)
        configure(appKey)
    }

    // Mainly for use by other Rownd SDKs (like Flutter)
    fun _registerActivityLifecycle(app: Application) {
        if (appHandleWrapper == null) {
            appHandleWrapper = AppLifecycleListener(app)
            _registerActivityLifecycle(null)
        }
    }

    fun _registerActivityLifecycle(activity: FragmentActivity?) {
        if (activity != null && appHandleWrapper == null) {
            appHandleWrapper = AppLifecycleListener(activity)
        }
    }

    private fun runRememberedRequestSignIn() {
        rememberedRequestSignIn?.let {
            it()
            rememberedRequestSignIn = null
        }
    }

    private fun isAppConfigLoadingWithCallback(callback: () -> (Unit)): Boolean {
        val scope = CoroutineScope(Dispatchers.IO)
        val isLoading = state.value.appConfig.isLoading
        if (isLoading) {
            scope.launch {
                store.stateAsStateFlow().collect {
                    // Callback when the appConfig has loaded
                    if (!it.appConfig.isLoading) {
                        callback()
                        scope.cancel()
                        return@collect
                    }
                }
            }
        }
        return isLoading
    }

    private fun isOneTapRequestedAndNotDisplayedYet(): Boolean {
        val google = state.value.appConfig.config.hub.auth.signInMethods.google
        return google.enabled && google.oneTap.mobileApp.autoPrompt && google.oneTap.mobileApp.delay == 0 && !hasDisplayedOneTap
    }

    fun requestSignIn(
        signInOptions: RowndSignInOptions
    ) {
        determineSignInOptions(signInOptions).apply {
            displayHub(HubPageSelector.SignIn, jsFnOptions = this)
        }
    }

    internal fun requestSignIn(signInJsOptions: RowndSignInJsOptions) {
        displayHub(HubPageSelector.SignIn, jsFnOptions = signInJsOptions)
    }

    fun requestSignIn(
        with: RowndSignInHint
    ) {
        requestSignIn(with, signInOptions = RowndSignInOptions())
    }

    fun requestSignIn(with: RowndSignInHint, signInOptions: RowndSignInOptions) {
        val isAppConfigLoading = isAppConfigLoadingWithCallback {
            requestSignIn(with, signInOptions)
        }

        if (isAppConfigLoading) {
            return
        }

        // Prevent Sign-in when Google One Tap is requested
        if (isOneTapRequestedAndNotDisplayedYet()) {
            rememberedRequestSignIn = { requestSignIn(with, signInOptions) }
            return
        }

        val signInOptions = determineSignInOptions(signInOptions)
        when (with) {
            RowndSignInHint.Google -> signInWithGoogle(intent = signInOptions.intent)
            RowndSignInHint.OneTap -> showGoogleOneTap()
            RowndSignInHint.Passkey -> {
                appHandleWrapper?.activity?.get()
                    ?.let { passkeyAuthenticator.authentication.authenticate(it) }
            }

            RowndSignInHint.Guest -> {
                displayHub(
                    HubPageSelector.SignIn,
                    jsFnOptions = RowndSignInJsOptions(signInType = RowndSignInType.Anonymous)
                )
            }
        }
    }

    @Suppress("unused")
    fun requestSignIn() {
        displayHub(HubPageSelector.SignIn)
    }

    inner class auth {
        inner class passkeys {
            fun register() {
                displayHub(
                    targetPage = HubPageSelector.ConnectAuthenticator,
                    jsFnOptions = RowndAuthenticatorRegistrationOptions(type = AuthenticatorType.Passkey)
                )
            }

            fun authenticate() {
                appHandleWrapper?.activity?.get()?.let { passkeyAuthenticator.authentication.authenticate(it) }
            }
        }
    }

    fun signOut() {
        rowndContext.hubViewModel?.webView()?.postValue(null)
        store.dispatch(StateAction.SetAuth(AuthState()))
        store.dispatch(StateAction.SetUser(User()))

        // Remove any cached access/refresh tokens in authenticatedApi client
        userRepo.userApi.client.plugin(Auth).providers.filterIsInstance<BearerAuthProvider>()
            .firstOrNull()?.clearToken()

        val googleSignInMethodConfig =
            state.value.appConfig.config.hub.auth.signInMethods.google
        if (googleSignInMethodConfig.enabled) {
            signOutOfGoogle()
        }
    }

    fun manageAccount() {
        displayHub(HubPageSelector.ManageAccount)
    }

    fun connectAuthenticator(with: RowndConnectAuthenticatorHint) {
        displayHub(
            targetPage = HubPageSelector.ConnectAuthenticator,
            jsFnOptions = RowndAuthenticatorRegistrationOptions()
        )
    }

    inner class Firebase {
        fun getIdToken(): Deferred<String?> {
            return connectionAction.getFirebaseIdToken()
        }
    }

    @Suppress("unused")
    fun addEventListener(observer: (RowndEvent) -> Unit) {
        eventEmitter.addListener(observer)
    }

    @Suppress("unused")
    fun removeEventListener(observer: (RowndEvent) -> Unit) {
        eventEmitter.removeListener(observer)
    }

    private fun determineSignInOptions(signInOptions: RowndSignInOptions): RowndSignInOptions {
        if (signInOptions.intent == RowndSignInIntent.SignUp || signInOptions.intent == RowndSignInIntent.SignIn) {
            if (state.value.appConfig.config.hub.auth.useExplicitSignUpFlow != true) {
                signInOptions.intent = null
                Log.w(
                    "Rownd",
                    "Sign in with intent: SignIn/SignUp is not enabled. Turn it on in the Rownd platform"
                )
            }
        }

        return signInOptions
    }

    private fun signOutOfGoogle() {
        val activity = appHandleWrapper?.activity?.get() ?: return

        CoroutineScope(Dispatchers.Main).launch {
            val credentialManager = CredentialManager.create(activity)
            val request = ClearCredentialStateRequest()
            credentialManager.clearCredentialState(request)
        }
    }

    private fun signInWithGoogle(intent: RowndSignInIntent?) {
        signInWithGoogle(intent, true)
    }

    private fun signInWithGoogle(intent: RowndSignInIntent?, wasUserInitiated: Boolean?) {
        signInWithGoogle(intent, hint = null, wasUserInitiated)
    }

    internal fun getActiveGmailAccounts(): Array<Account> {
        val applicationContext = appHandleWrapper?.activity?.get()?.applicationContext
            ?: return emptyArray()
        val accountManager = AccountManager.get(applicationContext);
        return accountManager.getAccountsByType("com.google")
    }

    internal fun signInWithGoogle(intent: RowndSignInIntent?, hint: String?, wasUserInitiated: Boolean? = true) {
        // We can't attempt this unless the app config is loaded

        googleSignInIntent = intent
        val googleSignInMethodConfig =
            state.value.appConfig.config.hub.auth.signInMethods.google

        if (!googleSignInMethodConfig.enabled) {
            Log.e(
                "Rownd",
                "Google sign-in is not enabled. Turn it on in the Rownd Platform https://app.rownd.io/applications/" + state.value.appConfig.id
            )
        }
        if (googleSignInMethodConfig.clientId == "") {
            Log.e("Rownd", "Cannot sign in with Google. Missing client configuration")
        }

        var nonce = UUID.randomUUID().toString()

        var googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(googleSignInMethodConfig.clientId)
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()

        var request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val activity = appHandleWrapper?.activity?.get() ?: return

        val credentialManager = CredentialManager.create(activity)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity,
                )
                handleSignInWithGoogle(result)
            } catch (e: GetCredentialException) {
                // Bail out if the user canceled the request
                if (e.type == android.credentials.GetCredentialException.TYPE_USER_CANCELED) {
                    return@launch
                }

                // Retry with any Google account
                try {
                    nonce = UUID.randomUUID().toString()
                    googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(googleSignInMethodConfig.clientId)
                        .setAutoSelectEnabled(false)
                        .setNonce(nonce)
                        .build()

                    request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = request,
                        context = activity,
                    )
                    handleSignInWithGoogle(result)
                } catch (e: GetCredentialException) {
                    handleSignInWithGoogleFailure(e, wasUserInitiated)
                } catch (e: GetCredentialProviderConfigurationException) {
                    handleSignInWithGoogleFailure(e, wasUserInitiated)
                }
            }
        }
    }

    private suspend fun handleSignInWithGoogle(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential = GoogleIdTokenCredential
                            .createFrom(credential.data)

                        val tokenResp = authRepo.getAccessToken(
                            googleIdTokenCredential.idToken,
                            intent = googleSignInIntent,
                            type = AuthRepo.AccessTokenType.google
                        )

                        tokenResp?.let {
                            eventEmitter.emit(RowndEvent(
                                event = RowndEventType.SignInCompleted,
                                data = buildJsonObject {
                                    put("method", RowndSignInType.Google.value)
                                    put("user_type", it.userType?.value)
                                }
                            ))
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("Rownd", "Received an invalid google id token response", e)
                    } catch (e: ApiException) {
                        Log.w("Rownd", "Google sign-in failed: code=" + e.statusCode)
                        eventEmitter.emit(RowndEvent(
                            event = RowndEventType.SignInFailed,
                            data = buildJsonObject {
                                put("method", RowndSignInType.Google.value)
                                put("error", e.message)
                            }
                        ))
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e("Rownd", "Unexpected type of credential: '$credential.type'")
                }
            }

            else -> {
                // Catch any unrecognized credential type here.
                Log.e("Rownd", "Unexpected type of credential")
            }
        }
    }

    private fun handleSignInWithGoogleFailure(e: Exception, wasUserInitiated: Boolean? = true) {
        Log.w("Rownd", "Google sign-in failed", e)
        eventEmitter.emit(RowndEvent(
            event = RowndEventType.SignInFailed,
            data = buildJsonObject {
                put("method", RowndSignInType.Google.value)
                put("error", e.message)
            }
        ))

        if (rowndContext.isDisplayingHub() || wasUserInitiated == true) {
            Rownd.requestSignIn(
                RowndSignInJsOptions(
                    intent = googleSignInIntent,
                    loginStep = RowndSignInLoginStep.Error,
                    errorMessage = e.localizedMessage ?: "Unable to complete Google sign-in"
                )
            )
        }
    }

    private fun showGoogleOneTap() {
        isDisplayingOneTap = true
        val googleSignInMethodConfig =
            state.value.appConfig.config.hub.auth.signInMethods.google

        fun cancel() {
            hasDisplayedOneTap = true
            isDisplayingOneTap = false
            userRequestedGoogleSignIn = false
            runRememberedRequestSignIn()
        }

        // Don't show Google one tap when the hub is displayed
        val composableBottomSheet = rowndContext.hubView?.get()
        if (composableBottomSheet != null && composableBottomSheet.isVisible) {
            cancel()
            return
        }

        if (!googleSignInMethodConfig.enabled) {
            throw RowndException("Google sign-in is not enabled. Turn it on in the Rownd Platform https://app.rownd.io/applications/" + state.value.appConfig.id)
        }
        if (googleSignInMethodConfig.clientId == "") {
            throw RowndException("Cannot sign in with Google. Missing client configuration")
        }

        signInWithGoogle(RowndSignInIntent.SignIn, wasUserInitiated = false)
    }

    suspend fun getAccessToken(): String? {
        return authRepo.getAccessToken()
    }

    suspend fun getAccessToken(idToken: String): String? {
        return authRepo.getAccessToken(idToken)?.accessToken
    }

    suspend fun _refreshToken(): String? {
        return try {
            val result = authRepo.refreshTokenAsync().await()
            result?.accessToken
        } catch (ex: RowndException) {
            Log.d("Rownd.testing", "Refresh token flow failed: ${ex.message}")
            null
        }
    }

    fun isEncryptionPossible(): Boolean {
        return userRepo.isEncryptionPossible()
    }

    // Internal stuff
    internal fun displayHub(
        targetPage: HubPageSelector,
        jsFnOptions: RowndSignInOptionsBase? = null
    ) {
        val isAppConfigLoading = isAppConfigLoadingWithCallback {
            displayHub(targetPage, jsFnOptions)
        }

        if (isAppConfigLoading) {
            return
        }

        // Prevent Hub from displaying when Google One Tap is requested
        if (isOneTapRequestedAndNotDisplayedYet()) {
            if (targetPage === HubPageSelector.SignIn) {
                rememberedRequestSignIn = { displayHub(targetPage, jsFnOptions) }
            }
            return
        }

        try {
            val activity = appHandleWrapper?.activity?.get() as? FragmentActivity

            if (activity == null) {
                appHandleWrapper?.registerActivityListener(
                    persistentListOf(Lifecycle.State.CREATED),
                    immediate = true,
                    once = true
                ) {
                    displayHub(targetPage, jsFnOptions)
                }
                return
            }

            if (activity.isFinishing) {
                return
            }

            var jsFnOptionsStr: String? = null
            if (jsFnOptions != null) {
                jsFnOptionsStr = jsFnOptions.toJsonString()
            }

            if (rowndContext.hubView?.get() != null && (rowndContext.hubView?.get() as? HubComposableBottomSheet)?.isDismissing == false) {
                rowndContext.hubView?.get()?.let { it ->
                    val hubSheet = it as? HubComposableBottomSheet
                    hubSheet?.existingWebView?.loadNewPage(targetPage, jsFnOptionsStr)

                    if (hubSheet?.isVisible != true) {
                        hubSheet?.show(activity.supportFragmentManager, HubComposableBottomSheet.TAG)
                    }
                }
            } else {
                val bottomSheet = HubComposableBottomSheet.newInstance(targetPage, jsFnOptionsStr)
                bottomSheet.show(activity.supportFragmentManager, HubComposableBottomSheet.TAG)
                rowndContext.hubView = WeakReference(bottomSheet)
            }
        } catch (ex: Exception) {
            Log.w("Rownd", "Failed to trigger Rownd bottom sheet for target: $targetPage", ex)
        }
    }

    internal fun getDeviceSize(context: Context): DisplayMetrics {
        val metrics = DisplayMetrics()
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowBounds = wm.currentWindowMetrics.bounds
            metrics.widthPixels = windowBounds.width()
            metrics.heightPixels = windowBounds.height()
            metrics.densityDpi = context.resources.configuration.densityDpi
            metrics.density = metrics.densityDpi / 160f
            metrics.scaledDensity = context.resources.configuration.fontScale * metrics.density
        } else {
            wm.defaultDisplay.getMetrics(metrics)
        }

        return metrics
    }
}

@Serializable
abstract class RowndSignInOptionsBase() {
    @Transient
    protected val json = Json { encodeDefaults = true }
    abstract fun toJsonString(): String
}

@Serializable
data class RowndSignInOptions(
    @SerialName("post_login_redirect")
    var postSignInRedirect: String? = Rownd.config.postSignInRedirect,
    var intent: RowndSignInIntent? = null
) : RowndSignInOptionsBase() {
    override fun toJsonString(): String {
        return json.encodeToString(serializer(), this)
    }
}

@Serializable
internal data class RowndSignInJsOptions(
    @SerialName("post_login_redirect")
    var postSignInRedirect: String? = Rownd.config.postSignInRedirect,
    var token: String? = null,
    @SerialName("login_step")
    var loginStep: RowndSignInLoginStep? = null,
    var intent: RowndSignInIntent? = null,
    @SerialName("user_type")
    var userType: RowndSignInUserType? = null,
    @SerialName("sign_in_type")
    var signInType: RowndSignInType? = null,
    @SerialName("request_id")
    var challengeId: String? = null,
    @SerialName("identifier")
    var userIdentifier: String? = null,
    @SerialName("error_message")
    var errorMessage: String? = null
) : RowndSignInOptionsBase() {
    override fun toJsonString(): String {
        return json.encodeToString(serializer(), this)
    }
}

enum class RowndSignInHint {
    Google,
    OneTap,
    Passkey,
    Guest,
}

enum class RowndConnectAuthenticatorHint {
    Passkey,
}

@Serializable
enum class RowndSignInIntent {
    @SerialName("sign_in")
    SignIn,

    @SerialName("sign_up")
    SignUp,
}

@Serializable
enum class RowndSignInUserType(var value: String) {
    @SerialName("new_user")
    NewUser("new_user"),

    @SerialName("existing_user")
    ExistingUser("existing_user"),
}

@Serializable
enum class RowndSignInType(var value: String) {
    @SerialName("passkey")
    Passkey("passkey"),

    @SerialName("anonymous")
    Anonymous("anonymous"),

    @SerialName("google")
    Google("google"),

    @SerialName("apple")
    Apple("apple"),

    @SerialName("sign_in_link")
    SignInLink("sign_in_link"),

    @SerialName("email")
    Email("email"),

    @SerialName("phone")
    Phone("phone"),
}

@Serializable
enum class RowndSignInLoginStep {
    @SerialName("init")
    Init,

    @SerialName("completing")
    Completing,

    @SerialName("success")
    Success,

    @SerialName("no_account")
    NoAccount,

    @SerialName("error")
    Error,
}