package io.rownd.android.models.network

import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.domain.AppSchemaField as DomainAppSchemaField
import io.rownd.android.models.domain.AppSchemaFieldEncryption as DomainAppSchemaFieldEncryption
import io.rownd.android.models.domain.AppSchemaEncryptionState as DomainAppSchemaEncryptionState
import io.rownd.android.util.ApiClient
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.GET

@Serializable
data class AppConfig(
    var id: String,
    var icon: String = "",
    @SerialName("user_verification_fields")
    var userVerificationFields: List<String>,
    var schema: Map<String, AppSchemaField>,
) {
    fun asDomainModel(): AppConfigState {
        return AppConfigState(
            isLoading = false,
            id = id,
            icon = icon,
            userVerificationFields = userVerificationFields,
            schema = schema.mapValues {
                it.value.asDomainModel()
            }
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
data class AppConfigResponse(
    var app: AppConfig
)

interface AppConfigService {
    @GET("hub/app-config")
    suspend fun getAppConfig() : Result<AppConfigResponse>
}

object AppConfigApi {
    val client: AppConfigService = ApiClient.getInstance().create(AppConfigService::class.java)
}