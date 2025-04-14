package io.rownd.android.util
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthenticatedApiClient @Inject constructor(
    engine: HttpClientEngine,
    private val rowndContext: RowndContext
): KtorApiClient(engine, rowndContext) {
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
