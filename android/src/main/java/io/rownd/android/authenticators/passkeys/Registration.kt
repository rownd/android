package io.rownd.android.authenticators.passkeys

import android.app.Activity
import android.os.Build
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
import io.ktor.utils.io.core.toByteArray
import io.rownd.android.models.PasskeyStatus
import io.rownd.android.models.RowndAuthenticatorRegistrationOptions
import io.rownd.android.models.json
import io.rownd.android.util.RowndException
import io.rownd.android.util.toBase64
import io.rownd.android.views.HubPageSelector
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class PasskeyRegistration constructor(private val passkeys: PasskeysCommon) {

    private val TAG = "Rownd.PasskeyAuthenticator"

    private val rowndContext = passkeys.rowndContext
    private val authenticatedApi = passkeys.authenticatedApiClient

    private fun modifyRequestJson(requestJsonString: String): String {
        val requestJsonElement = json.parseToJsonElement(requestJsonString)

        var requestJsonObject = requestJsonElement.jsonObject
        val userId = requestJsonObject["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: throw Exception("Missing user id")

        // Modifies user.id to use base-64 encoded value
        requestJsonObject = JsonObject(requestJsonObject.toMutableMap().apply {
            put("user", JsonObject(requestJsonObject["user"]?.jsonObject?.toMutableMap()?.apply {
                put("id", JsonPrimitive(userId.toByteArray().toBase64()))
            } ?: mutableMapOf()))
        })

        return requestJsonObject.toString().trim()
    }

    fun register(activity: Activity) {
        val jsFnOptions = RowndAuthenticatorRegistrationOptions(
            status = PasskeyStatus.Loading
        )

        if (rowndContext.store?.currentState?.auth?.isAuthenticated != true) {
            Log.e(TAG, "Need to be authenticated to register a passkey")
            return
        }

        displayRegistrationStatus(jsFnOptions)

        val credentialManager = CredentialManager.create(activity)

        CoroutineScope(Dispatchers.IO).launch IO@ {
            try {
                if (Build.VERSION.SDK_INT < 28) {
                    throw RowndException("This device is incompatible with passkeys")
                }

                val requestJson: String = modifyRequestJson(fetchRegistrationOptions(passkeys.computeRpId()))
                val createPublicKeyCredentialRequest = CreatePublicKeyCredentialRequest(
                    // Contains the request in JSON format. Uses the standard WebAuthn
                    // web JSON spec.
                    requestJson = requestJson,
                    // Defines whether you prefer to use only immediately available credentials,
                    // not hybrid credentials, to fulfill this request. This value is false
                    // by default.
                    preferImmediatelyAvailableCredentials = true,
                )

                CoroutineScope(Dispatchers.Main).launch Main@ {
                    try {
                        val result = credentialManager.createCredential(
                            request = createPublicKeyCredentialRequest,
                            context = activity,
                        )

                        Log.d("Rownd.passkeys", result.type)
                        handlePasskeyRegistrationResult(result)
                    }
                    catch (e : CreateCredentialException){
                        val errorMessage = e.errorMessage
                        // Handle cancellation from user
                        if (errorMessage?.contains("Unable to get sync account") == true) {
                            handleCancellation(e)
                            return@Main
                        }
                        handleFailure(e)
                    }
                    catch (e : Exception) {
                        handleFailure(e)
                    }
                }
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
    private fun handleCancellation(e: CreateCredentialException) {
        // Hide bottom sheet
        CoroutineScope(Dispatchers.Main).launch {
            val hubWebView = rowndContext.hubViewModel?.webView()?.value
            if (hubWebView?.isAttachedToWindow == true) {
                hubWebView.dismiss?.invoke()
            }
        }
    }


    private fun handleFailure(e: Exception) {
        Log.e(TAG, "Something went wrong during passkey registration", e)

        var errMessage = e.localizedMessage
        when(e) {
            is androidx.credentials.exceptions.CreateCredentialProviderConfigurationException -> errMessage = "Google Play Services is missing or out of date."
        }

        val jsFnOptions = RowndAuthenticatorRegistrationOptions(
            status = PasskeyStatus.Failed,
            error = errMessage
        )

        displayRegistrationStatus(jsFnOptions)
    }
}