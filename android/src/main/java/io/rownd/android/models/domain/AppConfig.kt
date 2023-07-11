package io.rownd.android.models.domain

import io.rownd.android.util.AnyValueSerializer
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
    var automations: List<Automation>? = null,
    val subdomain: String? = null
)

@Serializable
data class Automation constructor(
    var id: String? = null,
    var name: String? = null,
    var template: String? = null,
    var state: AutomationState? = null,
    var actions: List<AutomationsAction>? = null,
    var rules: List<AutomationRule>? = null,
    var triggers: List<AutomationTrigger>? = null,
)

@Serializable
enum class AutomationState {
    enabled,
    disabled,
}

@Serializable
data class AutomationsAction constructor(
    var type: AutomationActionType? = null,
    var args: Map<String, @Serializable(with = AnyValueSerializer::class) Any?>? = null
)

@Serializable
enum class AutomationActionType {
    @SerialName("REQUIRE_AUTHENTICATION")
    RequireAuthentication,
    @SerialName("PROMPT_FOR_PASSKEY")
    PromptForPasskey,
    @SerialName("SIGN_OUT")
    SignOut,
    @SerialName("REQUIRE_VERIFICATION")
    RequireVerification,
    @SerialName("REDIRECT")
    Redirect,
    @SerialName("PROMPT_FOR_INPUT")
    PromptForInput,
    @SerialName("NONE")
    None
}

@Serializable
data class AutomationTrigger constructor(
    var target: String? = null,
    var value: String? = null,
    var type: AutomationTriggerType? = null
)

@Serializable
enum class AutomationTriggerType {
    @SerialName("TIME")
    Time,
    @SerialName("URL")
    Url,
    @SerialName("EVENT")
    Event,
    @SerialName("HTML_SELECTOR")
    HtmlSelector,
    @SerialName("HTML_SELECTOR_VISIBLE")
    HtmlSelectorVisible
}

@Serializable
data class AutomationRule constructor(
    var attribute: String,
    @SerialName("entity_type")
    var entityType: AutomationRuleEntityType? = null,
    var condition: AutomationRuleCondition? = null,
    var value: @Serializable(with = AnyValueSerializer::class) Any?
)

@Serializable
enum class AutomationRuleCondition {
    @SerialName("EQUALS")
    Equals,
    @SerialName("NOT_EQUALS")
    NotEquals,
    @SerialName("CONTAINS")
    Contains,
    @SerialName("NOT_CONTAINS")
    NotContains,
    @SerialName("IN")
    In,
    @SerialName("NOT_IN")
    NotIn,
    @SerialName("EXISTS")
    Exists,
    @SerialName("NOT_EXISTS")
    NotExists,
    @SerialName("GREATER_THAN")
    GreaterThan,
    @SerialName("GREATER_THAN_EQUAL")
    GreaterThanEqual,
    @SerialName("LESS_THAN")
    LessThan,
    @SerialName("LESS_THAN_EQUAL")
    LessThanEqual,
}

@Serializable
enum class AutomationRuleEntityType {
    @SerialName("metadata")
    MetaData,
    @SerialName("user_data")
    UserData
}

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
    val anonymous: AnonymousSignInMethod = AnonymousSignInMethod(),
)

@Serializable
data class AnonymousSignInMethod constructor(
    val enabled: Boolean = false,
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
