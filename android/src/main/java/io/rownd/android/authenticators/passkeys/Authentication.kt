package io.rownd.android.authenticators.passkeys

import android.app.Activity
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInJsOptions
import io.rownd.android.RowndSignInLoginStep
import io.rownd.android.RowndSignInType
import io.rownd.android.RowndSignInUserType
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.network.TokenResponse
import io.rownd.android.models.repos.StateAction
import io.rownd.android.util.RowndEvent
import io.rownd.android.util.RowndEventType
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

                passkeys.rowndContext.eventEmitter?.emit(
                    RowndEvent(
                        event = RowndEventType.SignInCompleted,
                        data = mapOf(
                            "method" to RowndSignInType.Passkey.value,
                            "user_type" to fidoAuthResp.userType?.value,
                            "app_variant_user_type" to fidoAuthResp.appVariantUserType?.value,
                        )
                    )
                )

                val hubWebView = rowndContext.hubViewModel?.webView()?.value ?: return@launch

                val jsFnOptions = RowndSignInJsOptions(
                    loginStep = RowndSignInLoginStep.Success,
                    signInType = RowndSignInType.Passkey,
                    intent = RowndSignInIntent.SignIn,
                    userType = fidoAuthResp.userType,
                    appVariantUserType = fidoAuthResp.appVariantUserType,
                )

                hubWebView.loadNewPage(targetPage = HubPageSelector.SignIn, jsFnOptions = jsFnOptions)
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
                )

                val getCredRequest = GetCredentialRequest(
                    listOf(getPublicKeyCredentialOption)
                )

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = credentialManager.getCredential(
                            request = getCredRequest,
                            context = activity,
                        )
                        handleSignIn(result)
                    } catch (e : Exception) {
                        handleFailure(e)
                    }
                }
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
                handleFailure(Exception("Unexpected type of credential: ${credential.type}"))
            }
        }
    }

    private fun handleFailure(reason: Exception?) {
        Log.d(TAG, "Passkey authentication failure", reason)

        var errMessage = reason?.message
        when(reason) {
            is androidx.credentials.exceptions.GetCredentialProviderConfigurationException -> errMessage = "Google Play Services is missing or out of date."
        }

        val jsFnOptions = RowndSignInJsOptions(
            loginStep = RowndSignInLoginStep.Error,
            userType = RowndSignInUserType.ExistingUser,
            errorMessage = errMessage
        )

        Rownd.requestSignIn(jsFnOptions)

        passkeys.rowndContext.eventEmitter?.emit(
            RowndEvent(
                event = RowndEventType.SignInFailed,
                data = mapOf(
                    "method" to RowndSignInType.Passkey.toString(),
                    "user_type" to reason?.message
                )
            )
        )
    }
}