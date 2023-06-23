package io.rownd.android.views

//import android.webkit.*
//import android.webkit.WebView.setWebContentsDebuggingEnabled
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import androidx.webkit.*
import io.rownd.android.*
import io.rownd.android.models.*
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
import kotlin.properties.Delegates

val json = Json { ignoreUnknownKeys = true }

@Serializable
enum class HubPageSelector {
    SignIn,
    SignOut,
    QrCode,
    ManageAccount,
    ConnectAuthenticator,
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
    internal var animateBottomSheet: ((to: Float) -> Unit)? = null

    internal lateinit var rowndClient: RowndClient

    init {
        this.setLayerType(LAYER_TYPE_HARDWARE, null)
        this.setBackgroundColor(0x00000000)
        this.isHorizontalScrollBarEnabled = false
        this.isVerticalScrollBarEnabled = false
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = Constants.DEFAULT_WEB_USER_AGENT

        fun dynamicBottomSheet(height: String) {
            val deviceMetrics = Rownd.getDeviceSize(context)
            val viewportPixelHeight = deviceMetrics.heightPixels / deviceMetrics.density
            val deviceHeight = deviceMetrics.heightPixels
            height.toIntOrNull()?.let { it
                val ratio = it / viewportPixelHeight
                val targetOffset = deviceHeight.toFloat() - deviceHeight.toFloat() * ratio - 100F
                animateBottomSheet?.let { it(targetOffset) }
            }

        }

        val richard = RowndJavascriptInterface(this, ::dynamicBottomSheet)

        this.addJavascriptInterface(richard, "rowndAndroidSDK")
        this.webViewClient = RowndWebViewClient(this, context)

        val appFlags = Rownd.appHandleWrapper?.app?.get()?.applicationInfo?.flags ?: 0
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

    internal fun loadNewPage(targetPage: HubPageSelector = HubPageSelector.SignIn, jsFnOptions: RowndSignInOptionsBase) {
        jsFunctionArgsAsJson = jsFnOptions.toJsonString()
        this.targetPage = targetPage

        val parentScope = this
        CoroutineScope(Dispatchers.Main).launch {
            parentScope.loadUrl(Rownd.config.hubLoaderUrl())
        }
    }
}

class RowndWebViewClient(webView: RowndWebView, context: Context) : WebViewClientCompat() {
    private val webView: RowndWebView
    private val context: Context
    private var timeout: Boolean = true

    init {
        this.webView = webView
        this.context = context

        CoroutineScope(Dispatchers.IO).launch {
            delay(20000)
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

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url
        return if (shouldOpenInSeparateActivity(url)) {
            view.context?.startActivity(
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

    @OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        view.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)

        if (WebViewFeature.isFeatureSupported(WebViewFeature.VISUAL_STATE_CALLBACK)) {
            WebViewCompat.postVisualStateCallback(view, 1) {
                displayTargetPage(view)
            }
        } else {
            // If VISUAL_STATE_CALLBACK isn't supported on this platform, try to display the
            // appropriate content.
            displayTargetPage(view)
        }

        if (!url.startsWith(Rownd.config.baseUrl) && url != "about:blank") {
            webView.animateBottomSheet?.invoke(100F)
        }
    }

    private fun displayTargetPage(view: WebView) {
        when ((view as RowndWebView).targetPage) {
            HubPageSelector.SignIn, HubPageSelector.Unknown -> evaluateJavascript("rownd.requestSignIn(${webView.jsFunctionArgsAsJson})")
            HubPageSelector.SignOut -> evaluateJavascript("rownd.signOut({\"show_success\":true})")
            HubPageSelector.QrCode -> evaluateJavascript("rownd.generateQrCode(${webView.jsFunctionArgsAsJson})")
            HubPageSelector.ManageAccount -> evaluateJavascript("rownd.user.manageAccount()")
            HubPageSelector.ConnectAuthenticator -> evaluateJavascript("rownd.connectAuthenticator(${webView.jsFunctionArgsAsJson})")
        }

        setIsLoading(false)
    }

    private fun handleScriptReturn(value: String) {
        Log.d("Rownd.hub", value)
    }
}

class RowndJavascriptInterface constructor(
    private val parentWebView: RowndWebView,
    dynamicBottomSheet: (to: String) -> Unit
    ) {

    private val dynamicBottomSheet: (to: String) -> Unit

    init {
        this.dynamicBottomSheet = dynamicBottomSheet
    }

    @JavascriptInterface
    fun postMessage(message: String) {
        Log.d("Rownd.hub", "postMessage: $message")

        try {
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

                    parentWebView.rowndClient.signInRepo.reset()
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
                    Rownd.requestSignIn(
                        with = RowndSignInHint.Google,
                        RowndSignInOptions(intent = (interopMessage as TriggerSignInWithGoogleMessage).payload?.intent)
                    )
                    parentWebView.dismiss?.invoke()
                }

                MessageType.UserDataUpdate -> {
                    Rownd.store.dispatch(
                        StateAction.SetUser(
                            (interopMessage as UserDataUpdateMessage).payload.asDomainModel(
                                parentWebView.rowndClient.stateRepo,
                                parentWebView.rowndClient.userRepo
                            )
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

                MessageType.CreatePasskey -> {
                    parentWebView.rowndClient.appHandleWrapper?.activity?.get()?.let {
                        parentWebView.rowndClient.passkeyAuthenticator.registration.register(it)
                    }
                }

                MessageType.AuthenticateWithPasskey -> {
                    parentWebView.rowndClient.requestSignIn(with = RowndSignInHint.Passkey)
                }

                MessageType.HubResize -> {
                    val height = (interopMessage as HubResizeMessage).payload.height
                    if (height != null) {
                        dynamicBottomSheet(height)
                    }
                }

                else -> {
                    Log.w("RowndHub", "An unknown message was received")
                }
            }
        } catch (e : Exception) {
            Log.w("Rownd.hub", "Unparseable message", e)
        } catch (e : Error) {
            Log.w("Rownd.hub", "Unparseable message", e)
        }
    }
}