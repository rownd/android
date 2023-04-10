package io.rownd.android.models

import io.rownd.android.RowndSignInIntent
import io.rownd.android.models.network.User
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
    unknown
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
            else -> throw Error("Key '$messageType' did not match a known serializer.")
        }
    }
}