package io.rownd.android.util
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer


class AuthenticatedApi constructor(rowndContext: RowndContext): KtorApiClient(rowndContext) {
    init {
        this.client = this.client.config {
            install(Auth) {
                bearer {
                    loadTokens {
                        val accessToken = rowndContext.authRepo?.getAccessToken()
                        accessToken?.let { BearerTokens(it, "") }
                    }
                    refreshTokens {
                        val accessToken = rowndContext.authRepo?.getAccessToken()
                        accessToken?.let { BearerTokens(it, "") }
                    }
                    sendWithoutRequest { true }
                }
            }
        }
    }
}
