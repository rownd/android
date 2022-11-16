package io.rownd.android.util

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

open class KtorApiClient constructor(apiUrl: String)  {
    val client = HttpClient(Android) {
        install(Logging) {
            this.level = LogLevel.ALL
        }
        install(UserAgent) {
            agent = Constants.DEFAULT_API_USER_AGENT
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        install(ContentEncoding)
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = 5)
            exponentialDelay()
        }
        install(Resources)

        install(HttpTimeout) {
            requestTimeoutMillis = 15000L
        }

        defaultRequest {
            url(apiUrl)
            contentType(ContentType.parse("application/json"))

        }
    }
}