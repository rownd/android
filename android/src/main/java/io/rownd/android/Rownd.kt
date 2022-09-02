@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.app.Application
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.FragmentTransaction
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.repos.*
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.util.Encryption
import io.rownd.android.util.RowndException
import io.rownd.android.views.BottomSheet
import io.rownd.android.views.HubBottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.RowndWebView
import io.rownd.android.views.key_transfer.KeyTransferBottomSheet
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.lang.ref.WeakReference

object Rownd {
    private val json = Json { encodeDefaults = true }
    internal lateinit var appHandleWrapper: AppLifecycleListener

    lateinit var config: RowndConfig
    internal lateinit var store: Store<GlobalState, StateAction>
    var state = StateRepo.state

    @JvmStatic
    fun configure(app: Application, appKey: String) {
        appHandleWrapper = AppLifecycleListener(app)
        config = RowndConfig(appKey)

        store = StateRepo.setup(app.applicationContext.dataStore)
    }

    @JvmStatic
    fun requestSignIn(
        signInOptions: RowndSignInOptions
    ) {
        displayHub(HubPageSelector.SignIn, jsFnOptions = signInOptions ?: RowndSignInOptions())
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
    fun transferEncryptionKey() {
        val activity = appHandleWrapper.activity.get() as AppCompatActivity

        val bottomSheet = KeyTransferBottomSheet.newInstance()
        bottomSheet.show(activity.supportFragmentManager, BottomSheet.TAG)
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
        val activity = appHandleWrapper.activity.get() as AppCompatActivity

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