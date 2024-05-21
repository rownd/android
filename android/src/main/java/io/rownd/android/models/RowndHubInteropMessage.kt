package io.rownd.android.models

import io.rownd.android.RowndSignInIntent
import io.rownd.android.models.network.User
import io.rownd.android.util.RowndEvent
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

//val json = Json { encodeDefaults = true }

@Serializable(with = RowndHubInteropMessageSerializer::class)
sealed class RowndHubInteropMessage {
    abstract var type: MessageType
}

@Serializable
enum class MessageType {
    @SerialName("authentication")
    authentication,

    @SerialName("sign_out")
    signOut,

    @SerialName("try_again")
    tryAgain,

    @SerialName("trigger_sign_in_with_google")
    triggerSignInWithGoogle,

    @SerialName("user_data_update")
    UserDataUpdate,

    @SerialName("close_hub_view_controller")
    CloseHubView,

    @SerialName("trigger_sign_up_with_passkey")
    CreatePasskey,

    @SerialName("trigger_sign_in_with_passkey")
    AuthenticateWithPasskey,

    @SerialName("hub_loaded")
    HubLoaded,

    @SerialName("hub_resize")
    HubResize,

    @SerialName("can_touch_background_to_dismiss")
    CanTouchBackgroundToDismiss,

    @SerialName("event")
    Event,

    @SerialName("auth_challenge_initiated")
    AuthChallengeInitiated,

    @SerialName("auth_challenge_cleared")
    AuthChallengeCleared,

    @SerialName("open_email_app")
    OpenEmailApp,

    Unknown
}

@Serializable
data class AuthenticationMessage(
    override var type: MessageType = MessageType.authentication,
    var payload: AuthenticationPayload
) : RowndHubInteropMessage()

@Serializable
data class AuthenticationPayload(
    @SerialName("access_token")
    var accessToken: String,

    @SerialName("refresh_token")
    var refreshToken: String,
)

@Serializable
data class SignOutMessage(
    override var type: MessageType = MessageType.signOut
) : RowndHubInteropMessage()

@Serializable
data class TryAgainMessage(
    override var type: MessageType = MessageType.tryAgain
) : RowndHubInteropMessage()


@Serializable
data class TriggerSignInWithGoogleMessage(
    override var type: MessageType = MessageType.triggerSignInWithGoogle,
    var payload: TriggerSignInWithGooglePayload? = null
) : RowndHubInteropMessage()

@Serializable
data class TriggerSignInWithGooglePayload(
    @SerialName("intent")
    var intent: RowndSignInIntent? = null,
    var hint: String? = null,
)

@Serializable
data class UserDataUpdateMessage(
    override var type: MessageType = MessageType.UserDataUpdate,
    var payload: User
) : RowndHubInteropMessage()

@Serializable
data class CloseHubViewMessage(
    override var type: MessageType = MessageType.CloseHubView
) : RowndHubInteropMessage()

@Serializable
data class SignUpWithPasskeyMessage(
    override var type: MessageType = MessageType.CreatePasskey
) : RowndHubInteropMessage()

@Serializable
data class SignInWithPasskeyMessage(
    override var type: MessageType = MessageType.AuthenticateWithPasskey
) : RowndHubInteropMessage()

@Serializable
data class HubResizePayload(
    @SerialName("height")
    var height: String? = null,
)
@Serializable
data class HubLoaded(
    override var type: MessageType = MessageType.HubLoaded
) : RowndHubInteropMessage()

@Serializable
data class HubResizeMessage(
    override var type: MessageType = MessageType.HubResize,
    var payload: HubResizePayload
) : RowndHubInteropMessage()

@Serializable
data class CanTouchBackgroundToDismissMessage(
    override var type: MessageType = MessageType.CanTouchBackgroundToDismiss,
    var payload: CanTouchBackgroundToDismissPayload
) : RowndHubInteropMessage()

@Serializable
data class CanTouchBackgroundToDismissPayload(
    @SerialName("enable")
    var enable: String
)

@Serializable
data class EventMessage(
    override var type: MessageType = MessageType.Event,

    @SerialName("payload")
    var payload: RowndEvent
) : RowndHubInteropMessage()

@Serializable
data class OpenEmailAppMessage(
    override var type: MessageType = MessageType.OpenEmailApp
) : RowndHubInteropMessage()


object RowndHubInteropMessageSerializer : JsonContentPolymorphicSerializer<RowndHubInteropMessage>(RowndHubInteropMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out RowndHubInteropMessage> {
        return when (val messageType = element.jsonObject["type"]?.jsonPrimitive?.content) {
            "authentication" -> AuthenticationMessage.serializer()
            "sign_out" -> SignOutMessage.serializer()
            "try_again" -> TryAgainMessage.serializer()
            "trigger_sign_in_with_google" -> TriggerSignInWithGoogleMessage.serializer()
            "user_data_update" -> UserDataUpdateMessage.serializer()
            "close_hub_view_controller" -> CloseHubViewMessage.serializer()
            "trigger_sign_up_with_passkey" -> SignUpWithPasskeyMessage.serializer()
            "trigger_sign_in_with_passkey" -> SignInWithPasskeyMessage.serializer()
            "hub_loaded" -> HubLoaded.serializer()
            "hub_resize" -> HubResizeMessage.serializer()
            "can_touch_background_to_dismiss" -> CanTouchBackgroundToDismissMessage.serializer()
            "event" -> EventMessage.serializer()
            "open_email_app" -> OpenEmailAppMessage.serializer()
            else -> throw Error("Key '$messageType' did not match a known serializer.")
        }
    }
}