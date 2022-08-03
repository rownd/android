package io.rownd.android.models.domain

import kotlinx.serialization.Serializable

@Serializable
data class AppConfigState(
    val isErrored: Boolean = false,
    val isLoading: Boolean = false,
    val id: String = "",
    val icon: String = "",
    val userVerificationFields: List<String> = listOf(),
    val schema: Map<String, AppSchemaField> = HashMap<String, AppSchemaField>(),
)

@Serializable
data class AppSchemaField(
    val displayName: String?,
    val type: String?,
    val required: Boolean?,
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