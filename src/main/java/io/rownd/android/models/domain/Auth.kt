package io.rownd.android.models.domain

import kotlinx.serialization.Serializable

@Serializable
data class AuthState(
    val isLoading: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isVerifiedUser: Boolean = false,

    @Transient
    val isAuthenticated: Boolean = accessToken != null
)