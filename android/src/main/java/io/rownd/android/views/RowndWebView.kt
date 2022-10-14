package io.rownd.android.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.fragment.app.DialogFragment
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInHint
import io.rownd.android.models.AuthenticationMessage
import io.rownd.android.models.MessageType
import io.rownd.android.models.RowndHubInteropMessage
import io.rownd.android.models.UserDataUpdateMessage
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.repos.StateAction
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.Constants
import kotlinx.collections.immutable.persistentListOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val json = Json { ignoreUnknownKeys = true }

@Serializable
enum class HubPageSelector {
    SignIn,
    SignOut,
    QrCode,
    ManageAccount,
    Unknown
}

private const val HUB_CLOSE_AFTER_SECS: Long = 1

@SuppressLint("SetJavaScriptEnabled")
class RowndWebView(context: Context, attrs: AttributeSet?) : WebView(context, attrs), DialogChild {
    override var dialog: DialogFragment? = null
    internal var targetPage: HubPageSelector = HubPageSelector.Unknown
    internal var jsFunctionArgsAsJson: String = "{}"
    internal var progressBar: ProgressBar? = null
    internal var setIsLoading: ((isLoading: Boolean) -> Unit)? = null

    init {
        this.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        this.setBackgroundColor(0x00000000)
        this.isHorizontalScrollBarEnabled = false
        this.isVerticalScrollBarEnabled = false
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

    private fun evaluateJavascript(code: String) {
        val wrappedJs = """
            if (typeof rownd !== 'undefined') {
                $code
            } else {
                _rphConfig.push(['onLoaded', () => {
                    $code
                }]);
            }
        """

        Log.d("Rownd.hub", "Evaluating script: $code")

        webView.evaluateJavascript(wrappedJs) {
            Log.d("Rownd.hub", "Hub js evaluation response: $it")
        }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url
        return if (shouldOpenInSeparateActivity(url)) {
            view?.context?.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url.toString()))
            )
            true
        } else {
            false
        }
    }

    private fun shouldOpenInSeparateActivity(url: Uri?): Boolean {
        if (url == null || !URLUtil.isValidUrl(url.toString())) {
            return false
        }

        // The following urls should always open in the hub web view
        val urlStrings: List<String> = persistentListOf(
            "https://appleid.apple.com/auth/authorize"
        )
        val match = urlStrings.find {
            url.toString().startsWith(it)
        }
        if (match != null && match != "") {
            return false
        }

        return true
    }

    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(webView, url, favicon)
        Log.d("Rownd.hub", "Started loading $url")
        if (webView.setIsLoading == null) {
            webView.progressBar?.visibility = View.VISIBLE
        }

        webView.setIsLoading?.invoke(true)
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        view?.setBackgroundColor(0x00000000)

        when ((view as RowndWebView).targetPage) {
            HubPageSelector.SignIn, HubPageSelector.Unknown -> evaluateJavascript("rownd.requestSignIn(${webView.jsFunctionArgsAsJson})")
            HubPageSelector.SignOut -> evaluateJavascript("rownd.signOut({\"show_success\":true})")
            HubPageSelector.QrCode -> evaluateJavascript("rownd.generateQrCode(${webView.jsFunctionArgsAsJson})")
            HubPageSelector.ManageAccount -> evaluateJavascript("rownd.user.manageAccount()")
        }

        if (view.progress == 100) {
            if (webView.setIsLoading == null) {
                webView.progressBar?.visibility = View.INVISIBLE
            }
            webView.setIsLoading?.invoke(false)
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
        Log.d("Rownd.hub", "postMessage: $message")

        val interopMessage = json.decodeFromString(RowndHubInteropMessage.serializer(), message)
        Log.d("Rownd.hub", interopMessage.toString())

        when (interopMessage.type) {
            MessageType.authentication -> {
                if (parentWebView.targetPage != HubPageSelector.SignIn) {
                    return
                }

                Rownd.store.dispatch(
                    StateAction.SetAuth(
                        AuthState(
                            accessToken = (interopMessage as AuthenticationMessage).payload.accessToken,
                            refreshToken = interopMessage.payload.refreshToken
                        )
                    )
                )
                UserRepo.loadUserAsync()

                Executors.newSingleThreadScheduledExecutor().schedule({
                    parentWebView.dialog?.dismiss()
                }, HUB_CLOSE_AFTER_SECS, TimeUnit.SECONDS)
            }

            MessageType.signOut -> {
                if (parentWebView.targetPage != HubPageSelector.SignOut) {
                    return
                }

                Executors.newSingleThreadScheduledExecutor().schedule({
                    parentWebView.dialog?.dismiss()
                }, HUB_CLOSE_AFTER_SECS, TimeUnit.SECONDS)

                Rownd.store.dispatch(StateAction.SetAuth(AuthState()))
                Rownd.store.dispatch(StateAction.SetUser(User()))
            }

            MessageType.triggerSignInWithGoogle -> {
                Rownd.requestSignIn(RowndSignInHint.Google)
                parentWebView.dialog?.dismiss()
            }

            MessageType.UserDataUpdate -> {
                Rownd.store.dispatch(
                    StateAction.SetUser(
                        (interopMessage as UserDataUpdateMessage).payload.asDomainModel()
                    )
                )
                UserRepo.loadUserAsync()
            }

            MessageType.CloseHubView -> {
                parentWebView.dialog?.dismiss()
            }
            else -> {
                Log.w("RowndHub", "An unknown message was received")
            }
        }
    }
}