package io.rownd.android.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.webkit.*
import android.widget.ProgressBar
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.fragment.app.DialogFragment
import androidx.webkit.*
import io.rownd.android.Rownd
import io.rownd.android.RowndClient
import io.rownd.android.RowndSignInHint
import io.rownd.android.models.AuthenticationMessage
import io.rownd.android.models.MessageType
import io.rownd.android.models.RowndHubInteropMessage
import io.rownd.android.models.UserDataUpdateMessage
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.domain.User
import io.rownd.android.models.repos.StateAction
import io.rownd.android.util.Constants
import io.rownd.android.views.html.noInternetHTML
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    internal var dismiss: (() -> Unit)? = null
    internal var targetPage: HubPageSelector = HubPageSelector.Unknown
    internal var jsFunctionArgsAsJson: String = "{}"
    internal var progressBar: ProgressBar? = null
    internal var setIsLoading: ((isLoading: Boolean) -> Unit)? = null

    @OptIn(ExperimentalMaterialApi::class)
    internal var animateBottomSheet: ((to: ModalBottomSheetValue) -> Unit)? = null

    internal lateinit var rowndClient: RowndClient

    init {
        this.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
        this.setBackgroundColor(0x00000000)
        this.isHorizontalScrollBarEnabled = false
        this.isVerticalScrollBarEnabled = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = Constants.DEFAULT_WEB_USER_AGENT

        this.addJavascriptInterface(RowndJavascriptInterface(this, context), "rowndAndroidSDK")
        this.webViewClient = RowndWebViewClient(this, context)

        val appFlags = Rownd.appHandleWrapper.app.get()?.applicationInfo?.flags ?: 0
        if (0 != appFlags.and(ApplicationInfo.FLAG_DEBUGGABLE)) {
            setWebContentsDebuggingEnabled(true)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (this.canGoBack()) {
                        this.goBack()
                    } else {
                        dismiss?.invoke()
                    }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}

class RowndWebViewClient(webView: RowndWebView, context: Context) : WebViewClient() {
    private val webView: RowndWebView
    private val context: Context
    private var timeout: Boolean = true

    init {
        this.webView = webView
        this.context = context

        CoroutineScope(Dispatchers.IO).launch {
            delay(10000)
            if (timeout) {
                loadNoInternetHTML()
            }
        }
    }

    private fun setIsLoading(isLoading: Boolean) {
        if (isLoading) {
            if (webView.setIsLoading == null) {
                webView.progressBar?.visibility = View.VISIBLE
            }

            webView.setIsLoading?.invoke(true)
        } else {
            if (webView.setIsLoading == null) {
                webView.progressBar?.visibility = View.INVISIBLE
            }

            webView.setIsLoading?.invoke(false)
        }
    }

    private fun loadNoInternetHTML() {
        webView.post {
            setIsLoading(false)
            webView.loadDataWithBaseURL(null, noInternetHTML(context), "text/html", "utf-8", null)
        }
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
            "https://appleid.apple.com/auth/authorize",
            Rownd.config.baseUrl
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
        timeout = false
        super.onPageStarted(webView, url, favicon)
        Log.d("Rownd.hub", "Started loading $url")
        setIsLoading(true)

        if (url?.startsWith(Rownd.config.baseUrl) == true) {
            view?.setBackgroundColor(0x00000000)
        } else {
            view?.setBackgroundColor(Color.WHITE)
        }
    }

    @OptIn(ExperimentalMaterialApi::class)
    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        view?.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        when ((view as RowndWebView).targetPage) {
            HubPageSelector.SignIn, HubPageSelector.Unknown -> evaluateJavascript("rownd.requestSignIn(${webView.jsFunctionArgsAsJson})")
            HubPageSelector.SignOut -> evaluateJavascript("rownd.signOut({\"show_success\":true})")
            HubPageSelector.QrCode -> evaluateJavascript("rownd.generateQrCode(${webView.jsFunctionArgsAsJson})")
            HubPageSelector.ManageAccount -> evaluateJavascript("rownd.user.manageAccount()")
        }

        if (view.progress == 100) {
            setIsLoading(false)
        }

        if (url?.startsWith(Rownd.config.baseUrl) == false && url != "about:blank") {
            webView.animateBottomSheet?.invoke(ModalBottomSheetValue.Expanded)
        }
    }

    override fun onReceivedError(
        view: WebView?,
        request: WebResourceRequest?,
        error: WebResourceError?
    ) {
        super.onReceivedError(view, request, error)
        Log.e("Rownd.hub", error?.description.toString())
        loadNoInternetHTML()
    }

    private fun handleScriptReturn(value: String) {
        Log.d("Rownd.hub", value)
    }
}

class RowndJavascriptInterface(
    private val parentWebView: RowndWebView,
    context: Context) {

    private val context: Context

    init {
        this.context = context
    }

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
                parentWebView.rowndClient.userRepo.loadUserAsync()

                Executors.newSingleThreadScheduledExecutor().schedule({
                    parentWebView.dismiss?.invoke()
                }, HUB_CLOSE_AFTER_SECS, TimeUnit.SECONDS)
            }

            MessageType.signOut -> {
                if (parentWebView.targetPage != HubPageSelector.SignOut) {
                    return
                }

                Executors.newSingleThreadScheduledExecutor().schedule({
                    parentWebView.dismiss?.invoke()
                }, HUB_CLOSE_AFTER_SECS, TimeUnit.SECONDS)

                Rownd.store.dispatch(StateAction.SetAuth(AuthState()))
                Rownd.store.dispatch(StateAction.SetUser(User()))
            }

            MessageType.triggerSignInWithGoogle -> {
                Rownd.requestSignIn(RowndSignInHint.Google)
                parentWebView.dismiss?.invoke()
            }

            MessageType.UserDataUpdate -> {
                Rownd.store.dispatch(
                    StateAction.SetUser(
                        (interopMessage as UserDataUpdateMessage).payload.asDomainModel(parentWebView.rowndClient.stateRepo, parentWebView.rowndClient.userRepo)
                    )
                )
                parentWebView.rowndClient.userRepo.loadUserAsync()
            }

            MessageType.CloseHubView -> {
                parentWebView.dismiss?.invoke()
            }

            MessageType.tryAgain -> {
                CoroutineScope(Dispatchers.Main).launch {
                    parentWebView.loadUrl(Rownd.config.hubLoaderUrl())
                }
            }
            else -> {
                Log.w("RowndHub", "An unknown message was received")
            }
        }
    }
}