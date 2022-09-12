package io.rownd.android.models.network

import android.util.Log
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.repos.StateAction
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.ApiClient
import io.rownd.android.util.Encryption
import io.rownd.android.util.RequireAccessToken
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url
import java.net.URI
import java.net.URL

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
        val urlObj = URI(url)
        var encKey: String? = null

        if (urlObj.fragment != null) {
            encKey = urlObj.fragment
            signInUrl = signInUrl.replace("#${urlObj.fragment}", "")
        }

        val authResp = client.authenticateWithSignInLink(signInUrl)
        if (!authResp.isSuccessful) {
            Log.e("Rownd.SignInLink", "Auto sign-in failed for ${urlObj.path}")
            throw RowndAPIException(authResp)
        }

        val authBody: SignInAuthenticationResponse = authResp.body() ?: throw RowndAPIException(authResp)

        if (encKey != null) {
            Encryption.deleteKey(authBody.appUserId)
            Encryption.storeKey(encKey, authBody.appUserId)
        }

        Rownd.store.dispatch(StateAction.SetAuth(AuthState(
            accessToken = authBody.accessToken,
            refreshToken = authBody.refreshToken
        )))
        UserRepo.loadUserAsync().await()
    }
}