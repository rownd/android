package io.rownd.android.models

import android.net.Uri
import android.util.Base64
import android.util.Log
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.repos.AuthRepo
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import javax.inject.Inject

val json = Json { encodeDefaults = true }

@Serializable
data class RowndConfig(
    var appKey: String? = null,
    var baseUrl: String = "https://hub.rownd.io",
    var apiUrl: String = "https://api.rownd.io",
    var postSignInRedirect: String? = null,
    var appleIdCallbackUrl: String? = "https://api.rownd.io/hub/auth/apple/callback",
    var customizations: RowndCustomizations = RowndCustomizations(),
    var defaultRequestTimeout: Long = 15000L,
    var defaultNumApiRetries: Int = 5
) {

    @Inject
    @Transient
    lateinit var userRepo: UserRepo

    @Inject
    @Transient
    lateinit var stateRepo: StateRepo

    @Inject
    @Transient
    lateinit var authRepo: AuthRepo

    suspend fun hubLoaderUrl(): String {
        val jsonConfig = json.encodeToString(serializer(), this)
        val base64Config = Base64.encodeToString(jsonConfig.encodeToByteArray(), Base64.NO_WRAP)

        val uriBuilder = Uri.parse("$baseUrl/mobile_app").buildUpon()
        uriBuilder.appendQueryParameter("config", base64Config)

        try {
            val authState = authRepo.getLatestAuthState() ?: AuthState()
            val rphInitStr = authState.toRphInitHash(userRepo)
            uriBuilder.encodedFragment("rph_init=$rphInitStr")
        } catch (error: Exception) {
            Log.d("Rownd.config", "Couldn't compute requested init hash: ${error.message}")
        }

        return uriBuilder.build().toString()
    }
}

