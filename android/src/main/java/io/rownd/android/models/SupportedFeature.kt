package io.rownd.android.models

import kotlinx.serialization.Serializable

@Serializable
data class SupportedFeature(
    var name: String,
    var enabled: String
)