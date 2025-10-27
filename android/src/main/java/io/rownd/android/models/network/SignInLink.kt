package io.rownd.android.models.network

import android.app.Activity
import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.net.toUri
import androidx.core.view.doOnLayout
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInType
import io.rownd.android.RowndSignInUserType
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AuthenticatedApi
import io.rownd.android.util.Encryption
import io.rownd.android.util.KtorApiClient
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndEvent
import io.rownd.android.util.RowndEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class SignInLink(
    val link: String,
    @SerialName("app_user_id")
    val appUserId: String
)

@Serializable
data class SignInAuthenticationResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("refresh_token")
    val refreshToken: String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("app_user_id")
    val appUserId: String
)

class SignInLinkApi @Inject constructor(var rowndContext: RowndContext) {
    @Inject lateinit var userRepo: UserRepo

    private val authApiClient: AuthenticatedApi by lazy { AuthenticatedApi(rowndContext) }
    private val apiClient: KtorApiClient by lazy { KtorApiClient(rowndContext) }

    @Inject
    lateinit var config: io.rownd.android.models.RowndConfig

    suspend fun createSignInLink() : SignInLink {
        return authApiClient.client.post("me/auth/magic").body()
    }

    suspend fun authenticateWithSignInLink(url: String) : SignInAuthenticationResponse {
        return apiClient.client.get(url).body()
    }

    internal suspend fun signInWithLink(url: String) {
        var signInUrl = url
        val urlObj = url.toUri()
        var encKey: String? = null

        if (urlObj.fragment != null) {
            encKey = urlObj.fragment
            signInUrl = signInUrl.replace("#${urlObj.fragment}", "")
        }

        // Rewrite links to https, since we sometimes send links via SMS
        // without a protocol attached
        signInUrl = signInUrl.replace("http://", "https://")

        try {
            val authBody = authenticateWithSignInLink(signInUrl)

            if (encKey != null) {
                Encryption.deleteKey(authBody.appUserId)
                Encryption.storeKey(encKey, authBody.appUserId)
            }

            Rownd.store.dispatch(
                StateAction.SetAuth(
                    AuthState(
                        accessToken = authBody.accessToken,
                        refreshToken = authBody.refreshToken
                    )
                )
            )

            rowndContext.eventEmitter?.emit(
                RowndEvent(
                    event = RowndEventType.SignInCompleted,
                    data = mapOf(
                        "method" to RowndSignInType.SignInLink.value,
                        "user_type" to RowndSignInUserType.ExistingUser.value,
                    )
                )
            )

            userRepo.loadUserAsync().await()
        } catch (err: Exception) {
            Log.e("Rownd.SignInLink", "Exception thrown during auto sign-in attempt (url: ${urlObj.path}):", err)
        }
    }

    internal fun signInWithLinkIfPresentOnIntentOrClipboard(ctx: Activity) {
        if (Rownd.store.currentState.auth.isAuthenticated) {
            return
        }

        val action: String? = ctx.intent?.action

        if (action == ACTION_VIEW && isRowndSignInLink(ctx.intent?.data)) {
            dispatchSignInWithLink(ctx.intent?.data)
        } else if (config.enableSmartLinkPasteBehavior) {
            if (ctx.hasWindowFocus()) {
                // Look on the clipboard
                signInWithLinkFromClipboardIfPresent(ctx)
            } else {
                val rootView = ctx.findViewById<View>(android.R.id.content)
                rootView.doOnLayout {
                    signInWithLinkFromClipboardIfPresent(ctx)
                }
            }
        }
    }

    private fun signInWithLinkFromClipboardIfPresent(ctx: Activity) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        if (clipboard.primaryClipDescription?.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) != true) {
            return
        }

        var clipboardText = clipboard.primaryClip?.getItemAt(0)?.text.toString() ?: return

        if (!clipboardText.contains("rownd.link")) {
            return
        }

        if (!clipboardText.startsWith("http")) {
            clipboardText = "https://${clipboardText}"
        }

        val potentialSignInLink = Uri.parse(clipboardText) ?: return

        dispatchSignInWithLink(potentialSignInLink)
        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
    }

    private fun dispatchSignInWithLink(uri: Uri?) {
        if (isRowndSignInLink(uri)) {
            CoroutineScope(Dispatchers.IO).launch {
                signInWithLink(uri.toString())
            }
        }
    }

    private fun isRowndSignInLink(uri: Uri?) : Boolean {
        return uri?.host?.endsWith("rownd.link") == true
    }
}