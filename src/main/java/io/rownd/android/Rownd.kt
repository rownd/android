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
import io.rownd.android.models.repos.*
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.views.BottomSheet
import io.rownd.android.views.HubPageSelector
import io.rownd.android.views.RowndWebView
import kotlinx.coroutines.flow.StateFlow
import java.lang.ref.WeakReference

object Rownd {
    internal lateinit var appHandleWrapper: AppLifecycleListener

    lateinit var config: RowndConfig
    internal lateinit var store: Store<GlobalState, StateAction>
//    lateinit var state: StateFlow<GlobalState>
    var state = StateRepo.state

    @JvmStatic
    fun configure(app: Application, appKey: String) {
        appHandleWrapper = AppLifecycleListener(app)
        config = RowndConfig(appKey)

        store = StateRepo.setup(app.applicationContext.dataStore)

//        state.start()
        System.out.println("Configuring Rownd!")
    }

    @JvmStatic
    fun requestSignIn() {
        displayHub(HubPageSelector.SignIn)
    }

    @JvmStatic
    fun signOut() {
        displayHub(HubPageSelector.SignOut)
//        store.dispatch(StateAction.SetAuth(AuthState()))

    }

    @JvmStatic
    suspend fun getAccessToken(): String? {
//        return state.auth.getAccessToken()
        return AuthRepo.getAccessToken()
    }

    // Internal stuff
    internal fun displayHub(targetPage: HubPageSelector): RowndWebView {
        val activity = appHandleWrapper.activity as AppCompatActivity

        val hubView = RowndWebView(appHandleWrapper.app.applicationContext, null)
        hubView.targetPage = targetPage

        var bottomSheet = BottomSheet(hubView)
        bottomSheet.show(activity.supportFragmentManager, BottomSheet.TAG)

        var url = Rownd.config.hubLoaderUrl()
        hubView.loadUrl(url)
        hubView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        return hubView
    }
}