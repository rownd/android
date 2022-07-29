package io.rownd.android.models.domain

data class AuthState(
    val isLoading: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val isVerifiedUser: Boolean = false
)