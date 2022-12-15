package io.rownd.android.util
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*


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
