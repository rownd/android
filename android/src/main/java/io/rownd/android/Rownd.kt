@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.webkit.WebView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.SignInCredential
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.lyft.kronos.AndroidClockFactory
import dagger.Component
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.rownd.android.authenticators.passkeys.PasskeysCommon
import io.rownd.android.models.RowndAuthenticatorRegistrationOptions
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.SignInState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.SignInLinkApi
import io.rownd.android.models.repos.*
import io.rownd.android.util.*
import io.rownd.android.views.HubComposableBottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.RowndWebViewModel
import io.rownd.android.views.key_transfer.KeyTransferBottomSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference
import javax.inject.Singleton

// The default Rownd instance
val Rownd = RowndClient(DaggerRowndGraph.create())

@Singleton
@Component(modules = [ApiClientModule::class])
interface RowndGraph {
    fun stateRepo(): StateRepo
    fun userRepo(): UserRepo
    fun authRepo(): AuthRepo
    fun signInRepo(): SignInRepo
    fun signInLinkApi(): SignInLinkApi
    fun rowndContext(): RowndContext
    fun passkeyAuthenticator(): PasskeysCommon
    fun inject(rowndConfig: RowndConfig)
}

class RowndClient constructor(
    graph: RowndGraph,
    val config: RowndConfig = RowndConfig()
) {
    private val json = Json { encodeDefaults = true }
    internal var appHandleWrapper: AppLifecycleListener? = null

    internal lateinit var store: Store<GlobalState, StateAction>

    var stateRepo: StateRepo = graph.stateRepo()
    var userRepo: UserRepo = graph.userRepo()
    var authRepo: AuthRepo = graph.authRepo()
    var signInRepo: SignInRepo = graph.signInRepo()
    var signInLinkApi: SignInLinkApi = graph.signInLinkApi()
    var rowndContext = graph.rowndContext()
    var passkeyAuthenticator = graph.passkeyAuthenticator()

    var state = stateRepo.state
    private var intentLaunchers: MutableMap<String, ActivityResultLauncher<Intent>> = mutableMapOf()
    private var intentSenderRequestLaunchers: MutableMap<String, ActivityResultLauncher<IntentSenderRequest>> = mutableMapOf()
    private var hasDisplayedOneTap = false
    private var googleSignInIntent: RowndSignInIntent? = null

    init {
        graph.inject(config)
        rowndContext.config = config
        rowndContext.client = this
        rowndContext.authRepo = authRepo
        rowndContext.store = stateRepo.getStore()
        stateRepo.userRepo = userRepo
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
                    !hasDisplayedOneTap &&
                    !it.auth.isLoading &&
                    !it.auth.isAuthenticated &&
                    it.appConfig.config.hub.auth.signInMethods.google.clientId != "" &&
                    it.appConfig.config.hub.auth.signInMethods.google.oneTap.mobileApp.autoPrompt
                ) {
                    Handler(Looper.getMainLooper()).postDelayed({
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

        // Add an activity result callback for Google sign in
        appHandleWrapper?.registerActivityListener(
            persistentListOf(Lifecycle.State.CREATED),
            immediate = false,
            immediateIfBefore = Lifecycle.State.STARTED
        ) {
            if (it is ActivityResultCaller) {
                intentLaunchers[it.toString()] =
                    it.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        CoroutineScope(Dispatchers.IO).launch {
                            handleSignInWithGoogleCallback(result)
                        }
                    }
            }
        }

        // Remove Google sign-in callbacks if activity is destroyed
        appHandleWrapper?.registerActivityListener(
            persistentListOf(Lifecycle.State.DESTROYED),
            false
        ) {
            intentLaunchers.remove(it.localClassName)
        }

        // Add an activity result callback for Google One Tap sign in
        appHandleWrapper?.registerActivityListener(
            persistentListOf(Lifecycle.State.CREATED),
            immediate = false,
            immediateIfBefore = Lifecycle.State.STARTED
        ) {
            if (it is ActivityResultCaller) {
                intentSenderRequestLaunchers[it.toString()] =
                    it.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
                        CoroutineScope(Dispatchers.IO).launch {
                            handleGoogleOneTapCallback(result)
                        }
                    }
            }
        }

        // Remove Google One Tap sign-in callback if activity is destroyed
        appHandleWrapper?.registerActivityListener(
            persistentListOf(Lifecycle.State.DESTROYED),
            false
        ) {
            intentSenderRequestLaunchers.remove(it.toString())
        }
    }

    fun requestSignIn(
        signInOptions: RowndSignInOptions
    ) {
        val signInOptions = determineSignInOptions(signInOptions)
        displayHub(HubPageSelector.SignIn, jsFnOptions = signInOptions)
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
        val signInOptions = determineSignInOptions(signInOptions)
        when (with) {
            RowndSignInHint.Google -> signInWithGoogle(intent = signInOptions.intent)
            RowndSignInHint.OneTap -> showGoogleOneTap()
            RowndSignInHint.Passkey -> {
                appHandleWrapper?.activity?.get()?.let { passkeyAuthenticator.authentication.authenticate(it) }
            }
        }
    }

    fun requestSignIn() {
        displayHub(HubPageSelector.SignIn)
    }

    fun signOut() {
        rowndContext.hubViewModel?.webView()?.postValue(null)
        store.dispatch(StateAction.SetAuth(AuthState()))
        store.dispatch(StateAction.SetUser(User()))

        // Remove any cached access/refresh tokens in authenticatedApi client
        userRepo.userApi.client.plugin(Auth).providers.filterIsInstance<BearerAuthProvider>().firstOrNull()?.clearToken()

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

    fun transferEncryptionKey() {
        val activity = appHandleWrapper?.activity?.get() as AppCompatActivity

        val bottomSheet = KeyTransferBottomSheet.newInstance()
        bottomSheet.show(activity.supportFragmentManager, KeyTransferBottomSheet.TAG)
    }

    private fun determineSignInOptions(signInOptions: RowndSignInOptions) :RowndSignInOptions {
        if (signInOptions.intent == RowndSignInIntent.SignUp || signInOptions.intent == RowndSignInIntent.SignIn) {
            if (state.value.appConfig.config.hub.auth.useExplicitSignUpFlow != true) {
                signInOptions.intent = null
                Log.w("Rownd", "Sign in with intent: SignIn/SignUp is not enabled. Turn it on in the Rownd platform")
            }
        }

        return signInOptions
    }

    private fun signOutOfGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val activity = appHandleWrapper?.activity?.get() ?: return
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)

        googleSignInClient.signOut().addOnCompleteListener(activity) {
            if (!it.isSuccessful) {
                Log.w("Rownd", "Failed to sign out of Google")
            }
        }
    }

    private fun signInWithGoogle(intent: RowndSignInIntent?) {
        googleSignInIntent = intent
        val googleSignInMethodConfig =
            state.value.appConfig.config.hub.auth.signInMethods.google
        if (!googleSignInMethodConfig.enabled) {
            throw RowndException("Google sign-in is not enabled. Turn it on in the Rownd Platform https://app.rownd.io/applications/" + state.value.appConfig.id)
        }
        if (googleSignInMethodConfig.clientId == "") {
            throw RowndException("Cannot sign in with Google. Missing client configuration")
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(googleSignInMethodConfig.clientId)
            .build()

        val activity = appHandleWrapper?.activity?.get() ?: return

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent: Intent = googleSignInClient.signInIntent
        intentLaunchers[activity.toString()]?.launch(signInIntent)
    }

    private suspend fun handleSignInWithGoogleCallback(result: ActivityResult) {
        val task: Task<GoogleSignInAccount> =
            GoogleSignIn.getSignedInAccountFromIntent(
                result.data
            )
        try {
            val account: GoogleSignInAccount =
                task.getResult(ApiException::class.java)

            if (account.idToken == "") {
                Log.w("Rownd", "Google sign-in failed: missing idToken")
            } else {
                account.idToken?.let { idToken -> authRepo.getAccessToken(idToken, intent = googleSignInIntent, type = AuthRepo.AccessTokenType.google ) }
            }
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Rownd", "Google sign-in failed: code=" + e.statusCode)
        }
    }

    private suspend fun handleGoogleOneTapCallback(result: ActivityResult) {
        val activity = appHandleWrapper?.activity?.get() ?: return

        val credential: SignInCredential?
        try {
            val oneTapClient = Identity.getSignInClient(activity)
            credential = oneTapClient.getSignInCredentialFromIntent(result.data)
            if (credential.googleIdToken == "") {
                Log.e("Rownd.OneTap", "Google One Tap sign-in failed: missing idToken")
            } else {
                credential.googleIdToken?.let { idToken -> authRepo.getAccessToken(idToken, intent = null, type = AuthRepo.AccessTokenType.google) }
            }
        } catch (e: ApiException) {
            if (e.statusCode == CommonStatusCodes.CANCELED) {
                hasDisplayedOneTap = true
                Log.d("Rownd.OneTap", "Google One Tap UI was closed by user")
            } else {
                Log.e("Rownd.OneTap", "Couldn't get credential from result." + " (${e.localizedMessage})", e)
            }
        }
    }

    private fun showGoogleOneTap() {
        val googleSignInMethodConfig =
            state.value.appConfig.config.hub.auth.signInMethods.google
        if (!googleSignInMethodConfig.enabled) {
            throw RowndException("Google sign-in is not enabled. Turn it on in the Rownd Platform https://app.rownd.io/applications/" + state.value.appConfig.id)
        }
        if (googleSignInMethodConfig.clientId == "") {
            throw RowndException("Cannot sign in with Google. Missing client configuration")
        }

        val activity = appHandleWrapper?.activity?.get() ?: return

        val oneTapClient = Identity.getSignInClient(activity)
        val signUpRequest = BeginSignInRequest.builder()
            .setGoogleIdTokenRequestOptions(
                BeginSignInRequest.GoogleIdTokenRequestOptions.builder()
                    .setSupported(true)
                    .setServerClientId(googleSignInMethodConfig.clientId)
                    .setFilterByAuthorizedAccounts(false)
                    .build()
            )
            .build()

        oneTapClient.beginSignIn(signUpRequest)
            .addOnSuccessListener(activity) { result ->
                try {
                    Log.d("Rownd.OneTap", "Launching Google One Tap UI")
                    intentSenderRequestLaunchers[activity.toString()]?.launch(
                        IntentSenderRequest.Builder(result.pendingIntent.intentSender).build()
                    )
                    hasDisplayedOneTap = true
                } catch (e: IntentSender.SendIntentException) {
                    Log.e("Rownd.OneTap", "Couldn't start One Tap UI: ${e.localizedMessage}", e)
                }
            }
            .addOnFailureListener(activity) { e ->
                // No Google Accounts found. Just continue presenting the signed-out UI.
                Log.e("Rownd.OneTap", e.localizedMessage, e)
            }
    }

    suspend fun getAccessToken(): String? {
        return authRepo.getAccessToken()
    }

    suspend fun getAccessToken(idToken: String): String? {
        return authRepo.getAccessToken(idToken)
    }

    suspend fun _refreshToken(): String? {
        return try {
            val result = authRepo.refreshTokenAsync().await()
            result?.accessToken
        } catch (ex: RowndException) {
            Log.d("Rownd.testing","Refresh token flow failed: ${ex.message}")
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
        try {
            val activity = appHandleWrapper?.activity?.get() as FragmentActivity

            if (activity.isFinishing) {
                return
            }

            var jsFnOptionsStr: String? = null
            if (jsFnOptions != null) {
                jsFnOptionsStr = jsFnOptions.toJsonString()
            }

            val bottomSheet = HubComposableBottomSheet.newInstance(targetPage, jsFnOptionsStr)
            bottomSheet.show(activity.supportFragmentManager, HubComposableBottomSheet.TAG)
            rowndContext.hubView = WeakReference(bottomSheet)
        } catch (exception: Exception) {
            Log.w("Rownd", "Failed to trigger Rownd bottom sheet for target: $targetPage")
        }
    }

    internal fun getDeviceSize(context: Context): DisplayMetrics {
        val metrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val display = context.display
            display?.getMetrics(metrics)
        } else {
            @Suppress("DEPRECATION")
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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
): RowndSignInOptionsBase() {
    override fun toJsonString(): String {
        return json.encodeToString(serializer(), this)
    }
}

@Serializable
internal data class RowndSignInJsOptions (
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
    @SerialName("error_message")
    var errorMessage: String? = null
): RowndSignInOptionsBase() {
    override fun toJsonString(): String {
        return json.encodeToString(serializer(), this)
    }
}

enum class RowndSignInHint {
    Google,
    OneTap,
    Passkey,
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
enum class RowndSignInUserType {
    @SerialName("new_user")
    NewUser,
    @SerialName("existing_user")
    ExistingUser,
}

@Serializable
enum class RowndSignInType {
    @SerialName("passkey")
    Passkey
}

@Serializable
enum class RowndSignInLoginStep {
    @SerialName("init")
    Init,
    @SerialName("success")
    Success,
    @SerialName("no_account")
    NoAccount,
    @SerialName("error")
    Error,
}