package io.rownd.android.authenticators.passkeys

import android.app.Activity
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.GetCredentialException
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.rownd.android.*
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.network.TokenResponse
import io.rownd.android.models.repos.StateAction
import io.rownd.android.views.HubPageSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PasskeyAuthentication @Inject constructor(private val passkeys: PasskeysCommon) {

    private val TAG = "Rownd.PasskeyAuthenticator"

    private val rowndContext = passkeys.rowndContext
    private val api = passkeys.api

    private suspend fun fetchAuthenticatorOptions(rpId: String): String {
        return api.client.get("hub/auth/passkeys/authentication") {
            headers {
                append("origin", "https://${rpId}")
            }
        }.body()
    }

    private fun fidoAuthenticateWithServer(authJsonStr: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fidoAuthResp: TokenResponse =
                    api.client.post("hub/auth/passkeys/authentication") {
                        headers {
                            append(
                                "origin",
                                "https://${passkeys.computeRpId()}"
                            )
                        }
                        setBody(authJsonStr)
                    }.body()
                        ?: return@launch handleFailure(Exception("Authentication init response was empty"))

                rowndContext.store?.dispatch(
                    StateAction.SetAuth(
                        AuthState(
                            accessToken = fidoAuthResp.accessToken,
                            refreshToken = fidoAuthResp.refreshToken,
                            isLoading = false
                        )
                    )
                )
            } catch (e : Exception) {
                handleFailure(e)
            }
        }
    }

    fun authenticate(activity: Activity) {
        val credentialManager = CredentialManager.create(activity)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
                    requestJson = fetchAuthenticatorOptions(passkeys.computeRpId()),
                    preferImmediatelyAvailableCredentials = true
                )

                val getCredRequest = GetCredentialRequest(
                    listOf(getPublicKeyCredentialOption)
                )

                val result = credentialManager.getCredential(
                    request = getCredRequest,
                    activity = activity,
                )
                handleSignIn(result)
            } catch (e : GetCredentialException) {
                handleFailure(e)
            } catch (e : Exception) {
                handleFailure(e)
            }
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        // Handle the successfully returned credential.
        when (val credential = result.credential) {
            is PublicKeyCredential -> {
                val responseJson = credential.authenticationResponseJson
                fidoAuthenticateWithServer(responseJson)
            }
            else -> {
                // Catch any unrecognized credential type here.
                Log.e(TAG, "Unexpected type of credential")
            }
        }
    }

    private fun handleFailure(reason: Exception?) {
        val hubWebView = rowndContext.hubViewModel?.webView()?.value ?: return

        val jsFnOptions = RowndSignInJsOptions(
            loginStep = RowndSignInLoginStep.Error,
            signInType = RowndSignInType.Passkey,
            intent = RowndSignInIntent.SignIn,
            userType = RowndSignInUserType.ExistingUser,
            errorMessage = reason?.message
        )

        hubWebView.loadNewPage(targetPage = HubPageSelector.SignIn, jsFnOptions = jsFnOptions)
    }
}