package io.rownd.android.authenticators.passkeys

import android.app.Activity
import android.util.Log
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialException
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.rownd.android.models.PasskeyStatus
import io.rownd.android.models.RowndAuthenticatorRegistrationOptions
import io.rownd.android.views.HubPageSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PasskeyRegistration constructor(private val passkeys: PasskeysCommon) {

    private val TAG = "Rownd.PasskeyAuthenticator"

    private val rowndContext = passkeys.rowndContext
    private val authenticatedApi = passkeys.authenticatedApi

    fun register(activity: Activity) {
        val jsFnOptions = RowndAuthenticatorRegistrationOptions(
            status = PasskeyStatus.Loading
        )

        displayRegistrationStatus(jsFnOptions)

        val credentialManager = CredentialManager.create(activity)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
                    // Contains the request in JSON format. Uses the standard WebAuthn
                    // web JSON spec.
                    requestJson = fetchRegistrationOptions(passkeys.computeRpId()),
                    // Defines whether you prefer to use only immediately available credentials,
                    // not hybrid credentials, to fulfill this request. This value is false
                    // by default.
                    preferImmediatelyAvailableCredentials = true,
                )

                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val result = credentialManager.createCredential(
                            request = createPublicKeyCredentialRequest,
                            context = activity,
                        )

                        Log.d("Rownd.passkeys", result.type)
                        handlePasskeyRegistrationResult(result)
                    } catch (e : Exception) {
                        handleFailure(e)
                    }
                }
            } catch (e : CreateCredentialException){
                handleFailure(e)
            } catch (e : Exception) {
                handleFailure(e)
            }
        }
    }

    private suspend fun fetchRegistrationOptions(rpId: String): String {
        return authenticatedApi.client.get("hub/auth/passkeys/registration") {
            headers {
                append("origin", "https://${rpId}")
            }
        }.body()
    }

    private fun fidoRegisterWithServer(result: CreatePublicKeyCredentialResponse) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                authenticatedApi.client.post("hub/auth/passkeys/registration") {
                    headers {
                        append("origin", "https://${passkeys.computeRpId()}")
                    }
                    setBody(result.registrationResponseJson)
                }.body() as String?

                val jsFnOptions = RowndAuthenticatorRegistrationOptions(
                    status = PasskeyStatus.Success
                )

                displayRegistrationStatus(jsFnOptions)
            } catch (e : Exception) {
                handleFailure(e)
            }
        }
    }

    private fun handlePasskeyRegistrationResult(result: CreateCredentialResponse) {
        when(result) {
            is CreatePublicKeyCredentialResponse -> {
                fidoRegisterWithServer(result)
            }
            else -> {
                Log.e(TAG, "Unexpected credentials response: ${result.type}")
            }
        }
    }

    private fun displayRegistrationStatus(options: RowndAuthenticatorRegistrationOptions) {
        CoroutineScope(Dispatchers.Main).launch {
            val hubWebView = rowndContext.hubViewModel?.webView()?.value

            if (hubWebView?.isAttachedToWindow == true) {
                hubWebView.loadNewPage(HubPageSelector.ConnectAuthenticator, options)
            } else {
                rowndContext.client?.displayHub(HubPageSelector.ConnectAuthenticator, options)
            }
        }
    }

    private fun handleFailure(e: Exception) {
        Log.e(TAG, "Something went wrong during passkey registration", e)
        val jsFnOptions = RowndAuthenticatorRegistrationOptions(
            status = PasskeyStatus.Failed,
        )

        displayRegistrationStatus(jsFnOptions)
    }
}