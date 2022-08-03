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
        var activity = appHandleWrapper.activity as AppCompatActivity

        var subView = RowndWebView(appHandleWrapper.app.applicationContext, null)

        var bottomSheet = BottomSheet(subView)
        bottomSheet.show(activity.supportFragmentManager, BottomSheet.TAG)

//        bottomSheet.addView(subView)

        var url = Rownd.config.hubLoaderUrl()
        subView.loadUrl(url)
        subView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    @JvmStatic
    fun signOut() {
//        state.auth._authState.value = AuthState()

        store.dispatch(StateAction.SetAuth(AuthState()))
        // TODO: Trigger hub signout
    }

    @JvmStatic
    suspend fun getAccessToken(): String? {
//        return state.auth.getAccessToken()
        return AuthRepo.getAccessToken()
    }

    // Internal stuff
    internal fun displayHub() {

    }
}