package io.rownd.android.models.domain

import kotlinx.serialization.Serializable

@Serializable
data class GlobalState(
    var auth: AuthState = AuthState(),
    var appConfig: AppConfigState = AppConfigState()
)