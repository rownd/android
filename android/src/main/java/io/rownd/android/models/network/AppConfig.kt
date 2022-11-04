package io.rownd.android.models.network

import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.util.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.http.GET
import javax.inject.Inject
import io.rownd.android.models.domain.AppConfigConfig as DomainAppConfigConfig
import io.rownd.android.models.domain.AppSchemaEncryptionState as DomainAppSchemaEncryptionState
import io.rownd.android.models.domain.AppSchemaField as DomainAppSchemaField
import io.rownd.android.models.domain.AppSchemaFieldEncryption as DomainAppSchemaFieldEncryption
import io.rownd.android.models.domain.GoogleSignInMethod as DomainGoogleSignInMethod
import io.rownd.android.models.domain.HubAuthConfig as DomainHubAuthConfig
import io.rownd.android.models.domain.HubConfig as DomainHubConfig
import io.rownd.android.models.domain.SignInMethods as DomainSignInMethods

@Serializable
data class AppConfig(
    var id: String,
    var icon: String = "",
    @SerialName("user_verification_fields")
    var userVerificationFields: List<String>,
    var schema: Map<String, AppSchemaField>,
    var config: AppConfigConfig,
) {
    fun asDomainModel(): AppConfigState {
        return AppConfigState(
            isLoading = false,
            id = id,
            icon = icon,
            userVerificationFields = userVerificationFields,
            schema = schema.mapValues {
                it.value.asDomainModel()
            },
            config = config.asDomainModel()
        )
    }
}

@Serializable
data class AppSchemaField(
    @SerialName("display_name")
    var displayName: String?,
    var type: String?,
    var required: Boolean?,
    @SerialName("owned_by")
    var ownedBy: String?,
    var encryption: AppSchemaFieldEncryption? = null
) {
    fun asDomainModel(): DomainAppSchemaField {
        return DomainAppSchemaField(
            displayName,
            type,
            required,
            ownedBy,
            encryption?.asDomainModel()
        )
    }
}

@Serializable
data class AppSchemaFieldEncryption(
    var state: AppSchemaEncryptionState?
) {
    fun asDomainModel(): DomainAppSchemaFieldEncryption {
        return DomainAppSchemaFieldEncryption(
            state?.asDomainModel() ?: DomainAppSchemaEncryptionState.Disabled
        )
    }
}

@Serializable
enum class AppSchemaEncryptionState {
    @SerialName("enabled")
    Enabled,

    @SerialName("disabled")
    Disabled;

    fun asDomainModel(): DomainAppSchemaEncryptionState {
        return when(this) {
            Enabled -> DomainAppSchemaEncryptionState.Enabled
            Disabled -> DomainAppSchemaEncryptionState.Disabled
        }
    }
}

@Serializable
data class AppConfigConfig(
    var hub: HubConfig = HubConfig()
) {
    fun asDomainModel(): DomainAppConfigConfig {
        return DomainAppConfigConfig(
            hub.asDomainModel()
        )
    }
}

@Serializable
data class HubConfig(
    var auth: HubAuthConfig = HubAuthConfig()
) {
    fun asDomainModel(): DomainHubConfig {
        return DomainHubConfig(
            auth.asDomainModel()
        )
    }
}

@Serializable
data class HubAuthConfig(
    @SerialName("sign_in_methods")
    var signInMethods: SignInMethods = SignInMethods()
) {
    fun asDomainModel(): DomainHubAuthConfig {
        return DomainHubAuthConfig(
            signInMethods.asDomainModel()
        )
    }
}

@Serializable
data class SignInMethods(
    var google: GoogleSignInMethod = GoogleSignInMethod()
) {
    fun asDomainModel(): DomainSignInMethods {
        return DomainSignInMethods(
            google.asDomainModel()
        )
    }
}

@Serializable
data class GoogleSignInMethod(
    val enabled: Boolean = false,
    @SerialName("client_id")
    val clientId: String = ""
) {
    fun asDomainModel(): DomainGoogleSignInMethod {
        return DomainGoogleSignInMethod(
            enabled,
            clientId,
        )
    }
}

@Serializable
data class AppConfigResponse(
    var app: AppConfig
)

interface AppConfigService {
    @GET("hub/app-config")
    suspend fun getAppConfig() : Result<AppConfigResponse>
}

class AppConfigApi @Inject constructor(apiClient: ApiClient) {

    var apiClient: ApiClient

    init {
        this.apiClient = apiClient
    }

    val client: AppConfigService by lazy {
        apiClient.client.get().create(AppConfigService::class.java)
    }
}