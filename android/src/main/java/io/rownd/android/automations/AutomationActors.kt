package io.rownd.android.automations

import android.util.Log
import io.rownd.android.Rownd
import io.rownd.android.RowndConnectAuthenticatorHint
import io.rownd.android.RowndSignInHint
import io.rownd.android.models.RowndAuthenticatorRegistrationOptions
import io.rownd.android.models.domain.AutomationActionType
import io.rownd.android.util.AnyValueSerializer
import io.rownd.android.views.HubPageSelector
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.json.JSONObject

internal fun automationActorPasskey(args: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>?) {
    Log.i("Rownd","Trigger Automation: Rownd.connectAuthenticator(with: RowndConnectSignInHint.passkey)")

    val passkeyOptions = RowndAuthenticatorRegistrationOptions()
    val passkeyOptionsJsonString = passkeyOptions.toJsonString()
    val passkeyOptionsMap: Map<String, String?> = Json.decodeFromString(passkeyOptionsJsonString)

    val newArgs = args?.toMutableMap()
    passkeyOptionsMap.forEach { (key, value) ->
        newArgs?.set(key, value)
    }

    val jsFnOptionsStr = newArgs?.let { JSONObject(it).toString() }

    Rownd.connectAuthenticator(with = RowndConnectAuthenticatorHint.Passkey, jsFnOptionsStr = jsFnOptionsStr)
}

internal fun automationActorRequireAuthentication(args: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>?) {
    Log.i("Rownd","Trigger Automation: rownd.requestSignIn()")

    val jsFnOptionsStr = args?.let { JSONObject(it).toString() }

    val method = args?.get("method") ?: run {
        Rownd.displayHub(targetPage = HubPageSelector.SignIn, jsFnOptionsStr = jsFnOptionsStr)
        return
    }

    when (method) {
        "google" -> Rownd.requestSignIn(with = RowndSignInHint.Google)
        "passkey" -> Rownd.requestSignIn(with = RowndSignInHint.Passkey)
        else -> {
            return Rownd.displayHub(targetPage = HubPageSelector.SignIn, jsFnOptionsStr = jsFnOptionsStr)
        }
    }
}

internal val AutomationActors: List<Pair<AutomationActionType, (Map<String, @Serializable(with = AnyValueSerializer::class) Any?>?) -> Unit>> = listOf(
    Pair(AutomationActionType.PromptForPasskey) { x -> automationActorPasskey(x) },
    Pair(AutomationActionType.RequireAuthentication) { x -> automationActorRequireAuthentication(x) },
)