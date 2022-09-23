@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.app.Application
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.FragmentActivity
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.Store
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.repos.*
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.views.BottomSheet
import io.rownd.android.views.HubBottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.key_transfer.KeyTransferBottomSheet
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object Rownd {
    private val json = Json { encodeDefaults = true }
    internal lateinit var appHandleWrapper: AppLifecycleListener

    val config = RowndConfig()
    internal lateinit var store: Store<GlobalState, StateAction>
    var state = StateRepo.state

    private fun configure(appKey: String) {
        config.appKey = appKey
        store = StateRepo.setup(appHandleWrapper.app.get()!!.applicationContext.dataStore)
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
        val activity = appHandleWrapper.activity.get() as FragmentActivity

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