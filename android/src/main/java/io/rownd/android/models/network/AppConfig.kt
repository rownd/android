package io.rownd.android.models.network

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.util.AuthenticatedApiClient
import io.rownd.android.util.RowndContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import io.rownd.android.models.domain.AnonymousSignInMethod as DomainAnonymousSignInMethod
import io.rownd.android.models.domain.AppConfigConfig as DomainAppConfigConfig
import io.rownd.android.models.domain.AppSchemaEncryptionState as DomainAppSchemaEncryptionState
import io.rownd.android.models.domain.AppSchemaField as DomainAppSchemaField
import io.rownd.android.models.domain.AppSchemaFieldEncryption as DomainAppSchemaFieldEncryption
import io.rownd.android.models.domain.CustomizationsConfig as DomainCustomizationsConfig
import io.rownd.android.models.domain.GoogleOneTap as DomainGoogleOneTap
import io.rownd.android.models.domain.GoogleOneTapMobileApp as DomainGoogleOneTapMobileApp
import io.rownd.android.models.domain.GoogleSignInMethod as DomainGoogleSignInMethod
import io.rownd.android.models.domain.HubAuthConfig as DomainHubAuthConfig
import io.rownd.android.models.domain.HubConfig as DomainHubConfig
import io.rownd.android.models.domain.HubCustomStylesConfig as DomainHubCustomStylesConfig
import io.rownd.android.models.domain.HubCustomizationsConfig as DomainHubCustomizationsConfig
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
    var hub: HubConfig = HubConfig(),
    var customizations: CustomizationsConfig = CustomizationsConfig(),
    var subdomain: String? = null
) {
    fun asDomainModel(): DomainAppConfigConfig {
        return DomainAppConfigConfig(
            hub.asDomainModel(),
            customizations.asDomainModel(),
            subdomain,
        )
    }
}

@Serializable
data class CustomizationsConfig(
    @SerialName("primary_color")
    var primaryColor: String? = ""
) {
    fun asDomainModel(): DomainCustomizationsConfig {
        return DomainCustomizationsConfig(
            primaryColor
        )
    }
}

@Serializable
data class HubConfig(
    var auth: HubAuthConfig = HubAuthConfig(),
    var customizations: HubCustomizationsConfig? = HubCustomizationsConfig(),
    @SerialName("custom_styles")
    var customStyles: List<DomainHubCustomStylesConfig>? = List(0) { DomainHubCustomStylesConfig() },
) {
    fun asDomainModel(): DomainHubConfig {
        return DomainHubConfig(
            auth.asDomainModel(),
            customizations?.asDomainModel(),
            customStyles
        )
    }
}

@Serializable
data class HubCustomizationsConfig(
    @SerialName("font_family")
    var fontFamily: String? = "",
    @SerialName("dark_mode")
    val darkMode: String? = null,
    @SerialName("primary_color")
    val primaryColor: String? = null,
) {
    fun asDomainModel(): DomainHubCustomizationsConfig {
        return DomainHubCustomizationsConfig(fontFamily, darkMode, primaryColor)
    }
}

@Serializable
data class HubAuthConfig(
    @SerialName("sign_in_methods")
    var signInMethods: SignInMethods = SignInMethods(),
    @SerialName("use_explicit_sign_up_flow")
    var useExplicitSignUpFlow: Boolean? = null
) {
    fun asDomainModel(): DomainHubAuthConfig {
        return DomainHubAuthConfig(
            signInMethods.asDomainModel(),
            useExplicitSignUpFlow
        )
    }
}

@Serializable
data class SignInMethods(
    var google: GoogleSignInMethod = GoogleSignInMethod(),
    var anonymous: AnonymousSignInMethod = AnonymousSignInMethod()
) {
    fun asDomainModel(): DomainSignInMethods {
        return DomainSignInMethods(
            google.asDomainModel(),
            anonymous.asDomainModel()
        )
    }
}

@Serializable
data class AnonymousSignInMethod(
    val enabled: Boolean = false,
) {
    fun asDomainModel(): DomainAnonymousSignInMethod {
        return DomainAnonymousSignInMethod(
            enabled,
        )
    }
}

@Serializable
data class GoogleSignInMethod(
    val enabled: Boolean = false,
    @SerialName("client_id")
    val clientId: String = "",
    @SerialName("one_tap")
    val oneTap: GoogleOneTap = GoogleOneTap()
) {
    fun asDomainModel(): DomainGoogleSignInMethod {
        return DomainGoogleSignInMethod(
            enabled,
            clientId,
            oneTap.asDomainModel(),
        )
    }
}

@Serializable
data class GoogleOneTap(
    @SerialName("mobile_app")
    val mobileApp: GoogleOneTapMobileApp = GoogleOneTapMobileApp()
) {
    fun asDomainModel(): DomainGoogleOneTap {
        return DomainGoogleOneTap(
            mobileApp.asDomainModel()
        )
    }
}

@Serializable
data class GoogleOneTapMobileApp(
    @SerialName("auto_prompt")
    val autoPrompt: Boolean = false,
    val delay: Int = 0,
) {
    fun asDomainModel(): DomainGoogleOneTapMobileApp {
        return DomainGoogleOneTapMobileApp(
            autoPrompt,
            delay,
        )
    }
}

@Serializable
data class AppVariant(
    var id: String,
    var name: String
)

@Serializable
data class AppConfigResponse(
    var app: AppConfig,
    var variant: AppVariant? = null
) {
    fun asDomainModel(): AppConfigState {
        val app = app.asDomainModel()

        variant?.let {
            return app.copy(
                variantId = it.id
            )
        }

        return app
    }
}

class AppConfigApi @Inject constructor() {
    @Inject
    lateinit var rowndContext: RowndContext

    @Inject
    lateinit var authenticatedApiClient: AuthenticatedApiClient

    suspend fun getAppConfig(): AppConfigResponse {
        val appConfig: AppConfigResponse = authenticatedApiClient.client.get("hub/app-config").body()
        return appConfig
    }

}