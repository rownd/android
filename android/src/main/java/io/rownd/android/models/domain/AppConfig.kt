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
    val schema: Map<String, AppSchemaField> = HashMap<String, AppSchemaField>(),
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