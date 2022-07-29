package io.rownd.android.models.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigState(
    var isErrored: Boolean = false,
    var isLoading: Boolean = false,
    var id: String = "",
    var icon: String = "",
    var userVerificationFields: List<String> = listOf(),
    var schema: Map<String, AppSchemaField> = HashMap<String, AppSchemaField>(),
)

@Serializable
data class AppSchemaField(
    var displayName: String?,
    var type: String?,
    var required: Boolean?,
    var ownedBy: String?,
    var encryption: AppSchemaFieldEncryption?
)

@Serializable
data class AppSchemaFieldEncryption(
    var state: AppSchemaEncryptionState?
)

@Serializable
enum class AppSchemaEncryptionState {
    Enabled, Disabled
}