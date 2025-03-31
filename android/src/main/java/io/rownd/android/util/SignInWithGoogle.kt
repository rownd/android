package io.rownd.android.util

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CredentialOption
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.rownd.android.Rownd
import io.rownd.android.RowndSignInIntent
import io.rownd.android.RowndSignInJsOptions
import io.rownd.android.RowndSignInLoginStep
import io.rownd.android.RowndSignInType
import io.rownd.android.models.domain.GoogleSignInMethod
import io.rownd.android.models.network.TokenResponse
import io.rownd.android.models.repos.AuthRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID
import javax.inject.Inject

class SignInWithGoogle @Inject constructor(internal val rowndContext: RowndContext) {
    private var intentLaunchers: MutableMap<String, ActivityResultLauncher<Intent>> = mutableMapOf()
    private var hasDisplayedOneTap = false
    private var isDisplayingOneTap = false
    private var userRequestedGoogleSignIn = false
    private var googleSignInIntent: RowndSignInIntent? = null

    private val SIGN_IN_WITH_GOOGLE_NOT_ENABLED = "Sign-in with Google is not enabled."

    private var currentSpan: Span? = null

    internal var rememberedRequestSignIn: (() -> Unit)? = null

    @Inject
    lateinit var authRepo: AuthRepo

    private fun runRememberedRequestSignIn() {
        rememberedRequestSignIn?.let {
            it()
            rememberedRequestSignIn = null
        }
    }

    internal fun signOut() {
        val activity = rowndContext.client?.appHandleWrapper?.activity?.get() ?: return

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val credentialManager = CredentialManager.create(activity)
                val request = ClearCredentialStateRequest()
                credentialManager.clearCredentialState(request)
            } catch (e: Exception) {
                Log.v("Rownd", "Google sign-out failed", e)
            }
        }
    }

    internal fun signIn(intent: RowndSignInIntent?) {
        signIn(intent, true)
    }

    internal fun signIn(intent: RowndSignInIntent?, wasUserInitiated: Boolean?) {
        signIn(intent, hint = null, wasUserInitiated)
    }

    internal fun getActiveGmailAccounts(): Array<Account> {
        val applicationContext =
            rowndContext.client?.appHandleWrapper?.activity?.get()?.applicationContext
                ?: return emptyArray()
        val accountManager = AccountManager.get(applicationContext);
        return accountManager.getAccountsByType("com.google")
    }

    internal fun signIn(
        intent: RowndSignInIntent?,
        hint: String?,
        wasUserInitiated: Boolean? = true
    ) {
        val tracer = rowndContext.telemetry?.getTracer()
        val span = tracer?.spanBuilder("signInWithGoogle")?.startSpan()
        currentSpan = span
        span?.setAttribute("wasUserInitiated", wasUserInitiated.toString())

        googleSignInIntent = intent
        val googleSignInMethodConfig = getGoogleConfig()

        // We can't attempt this unless the app config is loaded
        if (googleSignInMethodConfig?.enabled != true) {
            Log.e(
                "Rownd",
                "Google sign-in is not enabled. Turn it on in the Rownd Platform https://app.rownd.io/applications/" + rowndContext.store?.currentState?.appConfig?.id
            )

            if (wasUserInitiated == true) {
                showErrorToUser(SIGN_IN_WITH_GOOGLE_NOT_ENABLED)
            }
            span?.setAttribute("error", "not_enabled")
            endSpan()
            return
        }

        val googleClientId = googleSignInMethodConfig.clientId.takeIf {
            it.isNotEmpty()
        } ?: run {
            Log.e(
                "Rownd",
                "Cannot sign in with Google. Missing client_id. Add it to your app in the Rownd dashboard."
            )
            showErrorToUser(SIGN_IN_WITH_GOOGLE_NOT_ENABLED)
            span?.setAttribute("error", "missing_client_id")
            endSpan()
            return
        }

        var nonce = UUID.randomUUID().toString()

        // First try to sign in with pre-authorized accounts
        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(googleClientId)
            .setAutoSelectEnabled(true)
            .setNonce(nonce)
            .build()

        var request: GetCredentialRequest = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        val activity = rowndContext.client?.appHandleWrapper?.activity?.get() ?: return

        val credentialManager = CredentialManager.create(activity)

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = activity,
                )
                handleSignInWithGoogle(result)
            } catch (e: GetCredentialException) {
                // Bail out if the user canceled the request
                if (e.type == android.credentials.GetCredentialException.TYPE_USER_CANCELED) {
                    return@launch
                }

                // Retry with any Google account if the previous request failed
                try {
                    nonce = UUID.randomUUID().toString()

                    var credentialOption: CredentialOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(googleClientId)
                        .setAutoSelectEnabled(false)
                        .setNonce(nonce)
                        .build()

                    // This indicates that the user has disabled Google auto sign-in prompts,
                    // so we need to fall back to an explicit chooser dialog
                    if (e.type == android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL) {
                        credentialOption = GetSignInWithGoogleOption.Builder(googleClientId)
                            .setNonce(nonce)
                            .build()
                    }

                    request = GetCredentialRequest.Builder()
                        .addCredentialOption(credentialOption)
                        .build()

                    val result = credentialManager.getCredential(
                        request = request,
                        context = activity,
                    )
                    handleSignInWithGoogle(result)
                } catch (e: GetCredentialException) {
                    handleSignInWithGoogleFailure(e, wasUserInitiated)
                } catch (e: GetCredentialProviderConfigurationException) {
                    handleSignInWithGoogleFailure(e, wasUserInitiated)
                }
            }
        }
    }

    private suspend fun handleSignInWithGoogle(result: GetCredentialResponse) {
        when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        // Use googleIdTokenCredential and extract id to validate and
                        // authenticate on your server.
                        val googleIdTokenCredential =
                            GoogleIdTokenCredential.createFrom(credential.data)

                        val tokenResp =
                            exchangeGoogleTokenForRowndToken(googleIdTokenCredential.idToken)

                        tokenResp?.let {
                            rowndContext.eventEmitter?.emit(RowndEvent(
                                event = RowndEventType.SignInCompleted,
                                data = buildJsonObject {
                                    put("method", RowndSignInType.Google.value)
                                    put("user_type", it.userType?.value)
                                    put("app_variant_user_type", it.appVariantUserType?.value)
                                }
                            ))

                            currentSpan?.setStatus(StatusCode.OK)
                            endSpan()
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e("Rownd", "Received an invalid google id token response", e)
                        currentSpan?.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                        endSpan()
                    } catch (e: ApiException) {
                        Log.w("Rownd", "Google sign-in failed: code=" + e.statusCode)
                        rowndContext.eventEmitter?.emit(RowndEvent(
                            event = RowndEventType.SignInFailed,
                            data = buildJsonObject {
                                put("method", RowndSignInType.Google.value)
                                put("error", e.message)
                            }
                        ))
                        currentSpan?.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
                        endSpan()
                    }
                } else {
                    // Catch any unrecognized custom credential type here.
                    Log.e("Rownd", "Unexpected type of credential: '${credential.type}'")
                    currentSpan?.setStatus(StatusCode.ERROR, "Unexpected type of credential '${credential.type}'")
                    currentSpan?.setAttribute("was_error_presented_to_user", "false")
                    endSpan()
                }
            }
            else -> {
                // Catch any unrecognized credential type here.
                Log.e("Rownd", "Unexpected type of credential")
                currentSpan?.setStatus(StatusCode.ERROR, "Unexpected type of credential")
                currentSpan?.setAttribute("was_error_presented_to_user", "false")
                endSpan()
            }
        }
    }

    private fun handleSignInWithGoogleFailure(e: Exception, wasUserInitiated: Boolean? = true) {
        Log.w("Rownd", "Google sign-in failed or was canceled", e)

        // If the user explicitly cancels, don't show any errors
        if (e is GetCredentialCancellationException && e.type == android.credentials.GetCredentialException.TYPE_USER_CANCELED) {
            endSpan()
            return
        }

        if (
            e is GetCredentialException &&
            e.type == android.credentials.GetCredentialException.TYPE_NO_CREDENTIAL &&
            e.errorMessage?.toString()?.lowercase()?.contains("28434") == true &&
            wasUserInitiated == true
        ) {
            rowndContext.client?.appHandleWrapper?.activity?.get()?.let { activity ->
                getGoogleConfig()?.let { googleSignInMethodConfig ->
                    val intent = reattemptSignInUsingLegacyGoogleSignIn(
                        activity,
                        googleSignInMethodConfig.clientId
                    )
                    intentLaunchers[activity.toString()]?.launch(intent)
                }
            }
            currentSpan?.addEvent("Fallback to legacy sign-in", Attributes.of(
                AttributeKey.stringKey("reason"), e.errorMessage.toString()
            ))
            endSpan()
            return
        }

        currentSpan?.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
        currentSpan?.recordException(e)

        var errMessage = e.localizedMessage

        when (e) {
            is GetCredentialProviderConfigurationException -> errMessage =
                "Google Play Services is missing or out of date."
        }

        if (errMessage?.contains("failure response from one tap") == true) {
            errMessage = errMessage.substringAfterLast(':')
        }

        rowndContext.eventEmitter?.emit(RowndEvent(
            event = RowndEventType.SignInFailed,
            data = buildJsonObject {
                put("method", RowndSignInType.Google.value)
                put("error", errMessage)
            }
        ))

        // Google Play services may need an update, so let's check that prior to showing an error
        rowndContext.client?.appHandleWrapper?.activity?.get()?.let { activity ->
            val googleApiAvailability = GoogleApiAvailability.getInstance()
            val status = googleApiAvailability.isGooglePlayServicesAvailable(activity)

            if (status != ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(status)) {
                    // Directly show the dialog and let it handle the result
                    currentSpan?.addEvent("Requested that user update or enable Google Play Services")
                    googleApiAvailability.getErrorDialog(activity, status, 0)?.show()
                    endSpan()
                    return
                } else {
                    // Google Play Services is not supported on this device
                    errMessage = "Google Play Services is not supported on this device."
                }
            }
        }

        if (rowndContext.isDisplayingHub() || wasUserInitiated == true) {
            currentSpan?.addEvent("Displayed error to user")
            Rownd.requestSignIn(
                RowndSignInJsOptions(
                    intent = googleSignInIntent,
                    loginStep = RowndSignInLoginStep.Error,
                    errorMessage = errMessage
                )
            )
        }
        endSpan()
    }

    internal fun showGoogleOneTap() {
        isDisplayingOneTap = true
        val googleSignInMethodConfig = getGoogleConfig()

        fun cancel() {
            hasDisplayedOneTap = true
            isDisplayingOneTap = false
            userRequestedGoogleSignIn = false
            runRememberedRequestSignIn()
        }

        // Don't show Google one tap when the hub is displayed
        val composableBottomSheet = rowndContext.hubView?.get()
        if (composableBottomSheet != null && composableBottomSheet.isVisible) {
            cancel()
            return
        }

        if (googleSignInMethodConfig?.enabled != true) {
            throw RowndException("Google sign-in is not enabled. Turn it on in the Rownd Platform https://app.rownd.io/applications/" + rowndContext.store?.currentState?.appConfig?.id)
        }
        if (googleSignInMethodConfig.clientId == "") {
            throw RowndException("Cannot sign in with Google. Missing client configuration.")
        }

        signIn(RowndSignInIntent.SignIn, wasUserInitiated = false)
    }

    internal fun registerIntentLauncher(arc: ActivityResultCaller) {
        intentLaunchers[arc.toString()] =
            arc.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                CoroutineScope(Dispatchers.IO).launch {
                    handleSignInResultFromIntent(result)
                }
            }
    }

    internal fun deRegisterIntentLauncher(name: String) {
        intentLaunchers.remove(name)
    }

    @Deprecated("This function will be removed once androix.credentialmanager reaches feature parity.")
    private fun reattemptSignInUsingLegacyGoogleSignIn(
        context: Activity,
        googleClientId: String
    ): Intent {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(googleClientId)
            .requestEmail()
            .build();

        val mGoogleSignInClient = GoogleSignIn.getClient(context, gso);
        val signInIntent = mGoogleSignInClient.signInIntent

        return signInIntent
    }

    private suspend fun handleSignInResultFromIntent(result: ActivityResult) {
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)

        try {
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)

            account.idToken?.let { idToken ->
                val tokenResp = exchangeGoogleTokenForRowndToken(idToken)

                tokenResp?.let {
                    rowndContext.eventEmitter?.emit(RowndEvent(
                        event = RowndEventType.SignInCompleted,
                        data = buildJsonObject {
                            put("method", RowndSignInType.Google.value)
                            put("user_type", it.userType?.value)
                            put("app_variant_user_type", it.userType?.value)
                        }
                    ))
                    currentSpan?.setStatus(StatusCode.OK)
                }
                endSpan()
                return
            }

            Log.w("Rownd", "Google sign-in failed: missing idToken")
            showErrorToUser("The Google ID token was missing.")
            currentSpan?.setStatus(StatusCode.ERROR, "The Google ID token was missing.")
            endSpan()
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("Rownd", "Google sign-in failed: code=" + e.statusCode)
            rowndContext.eventEmitter?.emit(RowndEvent(
                event = RowndEventType.SignInFailed,
                data = buildJsonObject {
                    put("method", RowndSignInType.Google.value)
                    put("error", e.message)
                }
            ))
            currentSpan?.setStatus(StatusCode.ERROR, e.message ?: "Unknown error")
            endSpan()
        }
    }

    private fun showErrorToUser(message: String) {
        Rownd.requestSignIn(
            RowndSignInJsOptions(
                intent = googleSignInIntent,
                loginStep = RowndSignInLoginStep.Error,
                errorMessage = message
            )
        )
        currentSpan?.setAttribute("was_error_presented_to_user", "true")
    }

    private suspend fun exchangeGoogleTokenForRowndToken(idToken: String): TokenResponse? {
        return authRepo.getAccessToken(
            idToken,
            intent = googleSignInIntent,
            type = AuthRepo.AccessTokenType.google
        )
    }

    private fun getGoogleConfig(): GoogleSignInMethod? {
        return rowndContext.store?.currentState?.appConfig?.config?.hub?.auth?.signInMethods?.google
    }

    internal fun showOneTapIfApplicable() {
        CoroutineScope(Dispatchers.IO).launch {
            rowndContext.store?.stateAsStateFlow()?.collect {
                if (
                    !isDisplayingOneTap &&
                    !hasDisplayedOneTap &&
                    !it.auth.isLoading &&
                    !it.auth.isAuthenticated &&
                    it.appConfig.config.hub.auth.signInMethods.google.clientId != "" &&
                    it.appConfig.config.hub.auth.signInMethods.google.oneTap.mobileApp.autoPrompt
                ) {
                    isDisplayingOneTap = true
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            showGoogleOneTap()
                        },
                        it.appConfig.config.hub.auth.signInMethods.google.oneTap.mobileApp.delay.toLong()
                    )
                }
            }
        }
    }

    internal fun isOneTapRequestedAndNotDisplayedYet(): Boolean {
        val google = getGoogleConfig() ?: return false

        return google.enabled && google.oneTap.mobileApp.autoPrompt && google.oneTap.mobileApp.delay == 0 && !hasDisplayedOneTap
    }

    private fun endSpan() {
        currentSpan?.end()
        currentSpan = null
    }
}