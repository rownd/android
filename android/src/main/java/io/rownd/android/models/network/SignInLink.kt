package io.rownd.android.models.network

import android.app.Activity
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import android.view.View
import androidx.core.view.doOnLayout
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.ApiClient
import io.rownd.android.util.Encryption
import io.rownd.android.util.RequireAccessToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

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

interface SignInLinkService {
    @POST("me/auth/magic")
    @RequireAccessToken
    suspend fun createSignInLink() : Response<SignInLink>

    @GET
    suspend fun authenticateWithSignInLink(@Url url: String) : Response<SignInAuthenticationResponse>
}

object SignInLinkApi {
    internal val client: SignInLinkService = ApiClient.getInstance().create(SignInLinkService::class.java)

    internal suspend fun signInWithLink(url: String) {
        var signInUrl = url
        val urlObj = Uri.parse(url)
        var encKey: String? = null

        if (urlObj.fragment != null) {
            encKey = urlObj.fragment
            signInUrl = signInUrl.replace("#${urlObj.fragment}", "")
        }

        try {
            val authResp = client.authenticateWithSignInLink(signInUrl)
            if (!authResp.isSuccessful) {
                Log.e("Rownd.SignInLink", "Auto sign-in failed for ${urlObj.path}")
                throw RowndAPIException(authResp)
            }

            val authBody: SignInAuthenticationResponse =
                authResp.body() ?: throw RowndAPIException(authResp)

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
            UserRepo.loadUserAsync().await()
        } catch (err: Exception) {
            Log.e("Rownd.SignInLink", "Exception thrown during auto sign-in attempt:", err)
        }
    }

    internal fun signInWithLinkIfPresentOnIntentOrClipboard(ctx: Activity) {
        if (Rownd.store.currentState.auth.isAuthenticated) {
            return
        }

        val action: String? = ctx.intent?.action

        if (action == ACTION_VIEW && isRowndSignInLink(ctx.intent?.data)) {
            dispatchSignInWithLink(ctx.intent?.data)
        } else if (ctx.hasWindowFocus()) {
            // Look on the clipboard
            signInWithLinkFromClipboardIfPresent(ctx)
        } else {
            val rootView = ctx.findViewById<View>(android.R.id.content)
            rootView.doOnLayout {
                signInWithLinkFromClipboardIfPresent(ctx)
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
//        clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
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