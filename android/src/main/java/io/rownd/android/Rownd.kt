@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
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
import io.rownd.android.views.BottomSheet
import io.rownd.android.views.HubBottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.key_transfer.KeyTransferBottomSheet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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

    private fun configure(appKey: String) {
        config.appKey = appKey
        store = StateRepo.setup(appHandleWrapper.app.get()!!.applicationContext.dataStore)

        appHandleWrapper.registerActivityListener(
            persistentListOf(
                Lifecycle.State.INITIALIZED,
                Lifecycle.State.RESUMED
            ), true) {
            SignInLinkApi.signInWithLinkIfPresentOnIntentOrClipboard(it)
        }

        // Add an activity result callback for Google sign in
        appHandleWrapper.registerActivityListener(persistentListOf(Lifecycle.State.CREATED), false) {
            if (launcher == null) {
                launcher =
                    (it as AppCompatActivity).registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                        @Suppress("DeferredResultUnused")
                        CoroutineScope(Dispatchers.IO).async {
                            handleSignInWithGoogleCallback(result)
                        }
                    }
            }
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
        displayHub(HubPageSelector.SignOut)
        store.dispatch(StateAction.SetAuth(AuthState()))
        store.dispatch(StateAction.SetUser(User()))
    }

    @JvmStatic
    fun manageAccount() {
        displayHub(HubPageSelector.ManageAccount)
    }

    @JvmStatic
    fun transferEncryptionKey() {
        val activity = appHandleWrapper.activity?.get() as AppCompatActivity

        val bottomSheet = KeyTransferBottomSheet.newInstance()
        bottomSheet.show(activity.supportFragmentManager, BottomSheet.TAG)
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

        val activity = appHandleWrapper.activity?.get() as AppCompatActivity
        val googleSignInClient = GoogleSignIn.getClient(activity, gso)
        val signInIntent: Intent = googleSignInClient.signInIntent
        launcher?.launch(signInIntent)
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
    fun isEncryptionPossible() : Boolean {
        return UserRepo.isEncryptionPossible()
    }

    // Internal stuff
    private fun displayHub(targetPage: HubPageSelector, jsFnOptions: RowndSignInOptions? = null) {
        val activity = appHandleWrapper.activity?.get() as FragmentActivity

        var jsFnOptionsStr: String? = null
        if (jsFnOptions != null) {
            jsFnOptionsStr = json.encodeToString(RowndSignInOptions.serializer(), jsFnOptions)
        }

        val bottomSheet = HubBottomSheet.newInstance(targetPage, jsFnOptionsStr)
        bottomSheet.show(activity.supportFragmentManager, BottomSheet.TAG)
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
