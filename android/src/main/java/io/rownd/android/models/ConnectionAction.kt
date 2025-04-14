package io.rownd.android.models

import android.util.Log
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.util.AuthenticatedApiClient
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
data class ConnectionActionPayload constructor(
    @SerialName("action_type")
    val actionType: String,
)

class RowndConnectionAction @Inject constructor() {

    @Inject
    lateinit var rowndContext: RowndContext

    @Inject
    lateinit var stateRepo: StateRepo

    @Inject
    lateinit var authenticatedApiClient: AuthenticatedApiClient

    internal fun getFirebaseIdToken(): Deferred<String?> {
        if (!stateRepo.state.value.auth.isAuthenticated) {
            throw RowndException("User needs to be authenticated to generate a firebase *ID token*")
        }

        return CoroutineScope(Dispatchers.IO).async {
            try {
                val response: FirebaseGetIdTokenResponse = authenticatedApiClient.client.post("/hub/connection_action") {
                    setBody(
                        ConnectionActionPayload(
                            actionType = "firebase-auth.get-firebase-token"
                        )
                    )
                }.body()
                Log.i("RowndConnectionAction", "Successfully generated firebase *ID token*: $response")
                return@async response.data?.token
            } catch (ex: Exception) {
                throw RowndException("Failed to generate the firebase *ID token*: ${ex.message}")
            }
        }
    }
}


@Serializable
data class FirebaseGetIdTokenResponse constructor(
    @SerialName("result")
    val result: String? = null,
    @SerialName("data")
    val data: FirebaseGetIdTokenResponseData? = null,
)

@Serializable
data class FirebaseGetIdTokenResponseData constructor(
    @SerialName("token")
    val token: String? = null,
)

