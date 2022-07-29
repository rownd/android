package io.rownd.android.models

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

object RowndHubInteropMessageSerializer : JsonContentPolymorphicSerializer<RowndHubInteropMessage>(RowndHubInteropMessage::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out RowndHubInteropMessage> {
        return when (element.jsonObject["type"]?.jsonPrimitive?.content) {
            "authentication" -> AuthenticationMessage.serializer()
//            "sign_out" -> OrgModule.serializer()
            else -> throw Exception("Unknown Module: key 'type' not found or does not matches any module type")
        }
    }
}