package io.rownd.android.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.webkit.*
import androidx.annotation.RequiresApi
import androidx.fragment.app.DialogFragment
import io.rownd.android.Rownd
import io.rownd.android.models.AuthenticationMessage
import io.rownd.android.models.MessageType
import io.rownd.android.models.RowndHubInteropMessage
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.repos.StateAction
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.Constants
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


val json = Json { ignoreUnknownKeys = true }

@Serializable
enum class HubPageSelector {
    SignIn,
    SignOut,
    Unknown
}

@SuppressLint("SetJavaScriptEnabled")
class RowndWebView(context: Context, attrs: AttributeSet?) : WebView(context, attrs), DialogChild {

    override lateinit var dialog: DialogFragment
    internal var targetPage: HubPageSelector = HubPageSelector.Unknown

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = Constants.DEFAULT_WEB_USER_AGENT

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val nightModeFlags = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
                settings.forceDark = WebSettings.FORCE_DARK_ON
            }
        }

        this.addJavascriptInterface(RowndJavascriptInterface(this), "rowndAndroidSDK")
        this.webViewClient = RowndWebViewClient(this)

        val appFlags = Rownd.appHandleWrapper.app.get()?.applicationInfo?.flags ?: 0
        if (0 != appFlags.and(ApplicationInfo.FLAG_DEBUGGABLE)) {
            setWebContentsDebuggingEnabled(true)
        }

    }
}

class RowndWebViewClient(webView: RowndWebView) : WebViewClient() {
    private val webView: RowndWebView

    init {
        this.webView = webView
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        Log.d("Rownd.hub", "Started loading $url")
        // TODO: Need to display a loading indicator
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        when((view as RowndWebView).targetPage) {
            HubPageSelector.SignIn, HubPageSelector.Unknown -> view.evaluateJavascript("rownd.requestSignIn()") { handleScriptReturn(it) }
            HubPageSelector.SignOut -> view.evaluateJavascript("rownd.signOut()") { handleScriptReturn(it) }
        }

    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        Log.e("Rownd.hub", error?.description.toString())
    }

    private fun handleScriptReturn(value: String) {
        Log.d("Rownd.hub", value)
    }
}

class RowndJavascriptInterface(private val parentWebView: RowndWebView) {
    @JavascriptInterface
    fun postMessage(message: String) {
        Log.i("Rownd.hub", "postMessage: $message")

        val interopMessage = json.decodeFromString(RowndHubInteropMessage.serializer(), message)
        Log.d("Rownd.hub", interopMessage.toString())

        when(interopMessage.type) {
            MessageType.authentication ->  {
                if (Rownd.store.currentState.auth.isAuthenticated) {
                    // The Hub is open for something else, so just chill...
                    return
                }

                Rownd.store.dispatch(StateAction.SetAuth(AuthState(
                    accessToken = (interopMessage as AuthenticationMessage).payload.accessToken,
                    refreshToken = (interopMessage as AuthenticationMessage).payload.refreshToken
                )))
                UserRepo.loadUserAsync()

                parentWebView.dialog.dismiss()
            }

            MessageType.signOut -> {
                Rownd.store.dispatch(StateAction.SetAuth(AuthState()))
                Rownd.store.dispatch(StateAction.SetUser(User()))
                parentWebView.dialog.dismiss()
            }
            else -> {
                Log.w("RowndHub", "An unknown message was received")
            }
        }
    }
}