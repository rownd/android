package io.rownd.android.models.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonNames

@Serializable
data class AppConfigState @OptIn(ExperimentalSerializationApi::class) constructor(
    @Transient
    val isErrored: Boolean = false,
    @Transient
    val isLoading: Boolean = false,
    val id: String = "",
    val icon: String = "",
    @SerialName("user_verification_fields")
    @JsonNames("userVerificationFields")
    val userVerificationFields: List<String> = listOf(),
    val schema: Map<String, AppSchemaField> = HashMap(),
    val config: AppConfigConfig = AppConfigConfig(),
)

@Serializable
data class AppSchemaField @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("display_name")
    @JsonNames("displayName")
    val displayName: String?,
    val type: String?,
    val required: Boolean?,
    @SerialName("owned_by")
    @JsonNames("ownedBy")
    val ownedBy: String?,
    val encryption: AppSchemaFieldEncryption?
)

@Serializable
data class AppSchemaFieldEncryption(
    val state: AppSchemaEncryptionState?
)

@Serializable
enum class AppSchemaEncryptionState {
    Enabled, Disabled
}

@Serializable
data class AppConfigConfig constructor(
    val hub: HubConfig = HubConfig(),
    val customizations: CustomizationsConfig = CustomizationsConfig(),
    val subdomain: String? = null
)

@Serializable
data class CustomizationsConfig constructor(
    @SerialName("primary_color")
    val primaryColor: String? = null,
)

@Serializable
data class HubConfig constructor(
    val auth: HubAuthConfig = HubAuthConfig(),
    val customizations: HubCustomizationsConfig? = HubCustomizationsConfig(),
    @SerialName("custom_styles")
    val customStyles: List<HubCustomStylesConfig>? = List(0) { HubCustomStylesConfig() }
)

@Serializable
data class HubCustomizationsConfig constructor(
    @SerialName("font_family")
    val fontFamily: String? = null,
    @SerialName("dark_mode")
    val darkMode: String? = null,
    @SerialName("primary_color")
    val primaryColor: String? = null,
)

@Serializable
data class HubCustomStylesConfig constructor(
    val content: String? = "",
)

@Serializable
data class HubAuthConfig @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("sign_in_methods")
    @JsonNames("signInMethods")
    val signInMethods: SignInMethods = SignInMethods(),
    @SerialName("use_explicit_sign_up_flow")
    val useExplicitSignUpFlow: Boolean? = null
)

@Serializable
data class SignInMethods constructor(
    val google: GoogleSignInMethod = GoogleSignInMethod(),
)

@Serializable
data class GoogleSignInMethod @OptIn(ExperimentalSerializationApi::class) constructor(
    val enabled: Boolean = false,
    @SerialName("client_id")
    @JsonNames("clientId")
    val clientId: String = "",
    @SerialName("one_tap")
    @JsonNames("oneTap")
    val oneTap: GoogleOneTap = GoogleOneTap(),
)

@Serializable
data class GoogleOneTap @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("mobile_app")
    @JsonNames("mobileApp")
    val mobileApp: GoogleOneTapMobileApp = GoogleOneTapMobileApp(),
)

@Serializable
data class GoogleOneTapMobileApp @OptIn(ExperimentalSerializationApi::class) constructor(
    @SerialName("auto_prompt")
    @JsonNames("autoPrompt")
    val autoPrompt: Boolean = false,
    val delay: Int = 0,
)
