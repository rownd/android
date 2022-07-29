package io.rownd.android.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.Bitmap
import android.util.AttributeSet
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import io.rownd.android.Rownd
import io.rownd.android.models.AuthenticationMessage
import io.rownd.android.models.MessageType
import io.rownd.android.models.RowndHubInteropMessage
import io.rownd.android.models.domain.AuthState
import io.rownd.android.util.Constants
import kotlinx.serialization.json.Json

val json = Json { ignoreUnknownKeys = true }


@SuppressLint("SetJavaScriptEnabled")
class RowndWebView(context: Context, attrs: AttributeSet?) : WebView(context, attrs), DialogChild {

    override lateinit var dialog: DialogFragment

    init {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.userAgentString = Constants.DEFAULT_WEB_USER_AGENT
        this.addJavascriptInterface(RowndJavascriptInterface(this), "rowndAndroidSDK")
        this.webViewClient = RowndWebViewClient()

        if (0 != (Rownd.appHandleWrapper.app?.applicationInfo?.flags?.and(ApplicationInfo.FLAG_DEBUGGABLE)
                ?: false)
        ) {
            setWebContentsDebuggingEnabled(true)
        }

    }
}

class RowndWebViewClient : WebViewClient() {
    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)

        Log.d("Rownd.hub", "Started loading $url")
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)

        view?.evaluateJavascript("rownd.requestSignIn()") { value ->
            Log.d("Rownd.hub", value)
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
}

class RowndJavascriptInterface(private val parentWebView: RowndWebView) {
    @JavascriptInterface
    fun postMessage(message: String) {
        Log.i("Rownd.hub", "postMessage: $message")

        val interopMessage = json.decodeFromString(RowndHubInteropMessage.serializer(), message)
        Log.d("Rownd.hub", interopMessage.toString())

        when(interopMessage.type) {
            MessageType.authentication -> Rownd.state.auth._authState.value = AuthState(
                accessToken = (interopMessage as AuthenticationMessage).payload.accessToken,
                refreshToken = (interopMessage as AuthenticationMessage).payload.refreshToken
            )
            MessageType.signOut -> {
                Rownd.state.auth._authState.value = AuthState()
            }
            else -> {
                Log.w("RowndHub", "An unknown message was received")
            }
        }
        parentWebView.dialog.dismiss()
    }
}