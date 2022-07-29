@file:OptIn(ExperimentalMaterialApi::class)

package io.rownd.android

import android.app.Application
import android.content.Context
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material.ExperimentalMaterialApi
import androidx.fragment.app.FragmentTransaction
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.repos.AppConfigRepo
import io.rownd.android.util.AppLifecycleListener
import io.rownd.android.views.BottomSheet
import io.rownd.android.views.RowndWebView
import java.lang.ref.WeakReference

object Rownd {
    internal lateinit var appHandleWrapper: AppLifecycleListener

    lateinit var config: RowndConfig
    lateinit var appConfigRepo: AppConfigRepo

    @JvmStatic
    fun configure(app: Application, appKey: String) {
        appHandleWrapper = AppLifecycleListener(app)
        config = RowndConfig(appKey)

        appConfigRepo = AppConfigRepo()
        System.out.println("Configuring Rownd!")
    }

    @JvmStatic
    fun requestSignIn() {
        var activity = appHandleWrapper.activity as AppCompatActivity

        var subView = RowndWebView(appHandleWrapper.app.applicationContext, null)
        subView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        var url = Rownd.config.hubLoaderUrl()
        subView.loadUrl(url)
        var bottomSheet = BottomSheet(subView)
        bottomSheet.show(activity.supportFragmentManager, BottomSheet.TAG)
    }

    @JvmStatic
    fun signOut() {

    }

    // Internal stuff
    internal fun displayHub() {

    }
}