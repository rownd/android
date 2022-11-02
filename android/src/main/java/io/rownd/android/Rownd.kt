@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.network.SignInLinkApi
import io.rownd.android.models.repos.*
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.util.RowndException
import io.rownd.android.views.HubComposableBottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.RowndWebViewModel
import io.rownd.android.views.key_transfer.KeyTransferBottomSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


object Rownd {
    private val json = Json { encodeDefaults = true }
    internal lateinit var appHandleWrapper: AppLifecycleListener

    val config = RowndConfig()
    internal lateinit var store: Store<GlobalState, StateAction>
    var state = StateRepo.state
    private var launcher: ActivityResultLauncher<Intent>? = null
    private var launchers: MutableMap<String, ActivityResultLauncher<Intent>> = mutableMapOf()
    private lateinit var hubViewModel: RowndWebViewModel

    private fun configure(appKey: String) {
        config.appKey = appKey

        store = StateRepo.setup(appHandleWrapper.app.get()!!.applicationContext.dataStore)

        // Webview holder in case of activity restarts during auth
        val hubViewModelFactory = RowndWebViewModel.Factory(appHandleWrapper.app.get()!!)
        appHandleWrapper.registerActivityListener(
            persistentListOf(
                Lifecycle.State.CREATED
            ), true
        ) {
            if (it !is ViewModelStoreOwner) {
                return@registerActivityListener
            }
            hubViewModel = ViewModelProvider(it as ViewModelStoreOwner, hubViewModelFactory)[RowndWebViewModel::class.java]
            // Re-triggers the sign-in sheet in the event that the activity restarted during sign-in
            if (hubViewModel.webView().value != null) {
                displayHub(HubPageSelector.Unknown)
            }
        }

        appHandleWrapper.registerActivityListener(
            persistentListOf(
                Lifecycle.State.RESUMED
            ), true) {
            SignInLinkApi.signInWithLinkIfPresentOnIntentOrClipboard(it)
        }

        // Add an activity result callback for Google sign in
        appHandleWrapper.registerActivityListener(persistentListOf(Lifecycle.State.CREATED), false) {
            if (it is ActivityResultCaller) {
                launchers[it.toString()] =
                    it.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        CoroutineScope(Dispatchers.IO).launch {
                            handleSignInWithGoogleCallback(result)
                        }
                    }
            }
        }

        // Remove Google sign-in callbacks if activity is destroyed
        appHandleWrapper.registerActivityListener(persistentListOf(Lifecycle.State.DESTROYED), false) {
            launchers.remove(it.localClassName)
        }
    }

    @JvmStatic
    fun configure(app: Application, appKey: String) {
        appHandleWrapper = AppLifecycleListener(app)
        configure(appKey)
    }

    @JvmStatic
    fun configure(activity: FragmentActivity, appKey: String) {
        appHandleWrapper = AppLifecycleListener(activity)
        configure(appKey)
    }

    @JvmStatic
    fun requestSignIn(
        signInOptions: RowndSignInOptions
    ) {
        displayHub(HubPageSelector.SignIn, jsFnOptions = signInOptions)
    }

    @JvmStatic
    fun requestSignIn(
        with: RowndSignInHint
    ) {
        when (with) {
            RowndSignInHint.Google -> signInWithGoogle()
        }
    }

    @JvmStatic
    fun requestSignIn() {
        displayHub(HubPageSelector.SignIn)
    }

    @JvmStatic
    fun signOut() {
        hubViewModel.webView().postValue(null)
        store.dispatch(StateAction.SetAuth(AuthState()))
        store.dispatch(StateAction.SetUser(User()))

        val googleSignInMethodConfig = state.value.appConfig.config.hub.auth.signInMethods.google
        if (googleSignInMethodConfig.enabled) {
            signOutOfGoogle()
        }
    }

    @JvmStatic
    fun manageAccount() {
        displayHub(HubPageSelector.ManageAccount)
    }

    @JvmStatic
    fun transferEncryptionKey() {
        val activity = appHandleWrapper.activity?.get() as AppCompatActivity

        val bottomSheet = KeyTransferBottomSheet.newInstance()
        bottomSheet.show(activity.supportFragmentManager, KeyTransferBottomSheet.TAG)
    }

    private fun signOutOfGoogle() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val activity = appHandleWrapper.activity?.get() ?: return
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)

        googleSignInClient.signOut().addOnCompleteListener(activity) {
            if (!it.isSuccessful) {
                Log.w("Rownd", "Failed to sign out of Google")
            }
        }
    }

    private fun signInWithGoogle() {
        val googleSignInMethodConfig = state.value.appConfig.config.hub.auth.signInMethods.google
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

        val activity = appHandleWrapper.activity?.get() ?: return

        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent: Intent = googleSignInClient.signInIntent
        launchers[activity.toString()]?.launch(signInIntent)
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
                account.idToken?.let { idToken -> AuthRepo.getAccessToken(idToken) }
            }
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Rownd", "Google sign-in failed: code=" + e.statusCode)
        }
    }

    @JvmStatic
    suspend fun getAccessToken(): String? {
        return AuthRepo.getAccessToken()
    }

    @JvmStatic
    suspend fun _refreshToken(): String? {
        val result = AuthRepo.refreshTokenAsync().await()
        return result.accessToken
    }

    @JvmStatic
    fun isEncryptionPossible() : Boolean {
        return UserRepo.isEncryptionPossible()
    }

    // Internal stuff
    private fun displayHub(targetPage: HubPageSelector, jsFnOptions: RowndSignInOptions? = null) {
        try {
            val activity = appHandleWrapper.activity?.get() as FragmentActivity

            if (activity.isFinishing) {
                return
            }

            var jsFnOptionsStr: String? = null
            if (jsFnOptions != null) {
                jsFnOptionsStr = json.encodeToString(RowndSignInOptions.serializer(), jsFnOptions)
            }

            val bottomSheet = HubComposableBottomSheet.newInstance(targetPage, jsFnOptionsStr)
            bottomSheet.show(activity.supportFragmentManager, HubComposableBottomSheet.TAG)
        } catch(exception: Exception) {
            Log.w("Rownd", "Failed to trigger Rownd bottom sheet for target: $targetPage")
        }
    }
}

@Serializable
data class RowndSignInOptions(
    @SerialName("post_login_redirect")
    var postSignInRedirect: String? = Rownd.config.postSignInRedirect
)

enum class RowndSignInHint {
    Google,
}
