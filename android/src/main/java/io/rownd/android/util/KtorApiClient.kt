package io.rownd.android.util

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.resources.Resources
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject

private object CustomAndroidHttpLogger : Logger {
    private const val logTag = "Rownd.ApiClient"

    override fun log(message: String) {
        Log.d(logTag, redactSensitiveKeys(message))
    }
}

open class KtorApiClient @Inject constructor(engine: HttpClientEngine, rowndContext: RowndContext)  {
    private val base = HttpClient(engine) {
        install(Logging) {
            level = LogLevel.ALL
            logger = CustomAndroidHttpLogger
        }
        install(UserAgent) {
            agent = Constants.DEFAULT_API_USER_AGENT
        }
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true

            })
        }
        install(ContentEncoding)
        install(HttpRequestRetry) {
            retryOnExceptionOrServerErrors(maxRetries = rowndContext.config.defaultNumApiRetries)
            exponentialDelay(maxDelayMs = 3000L)
        }
        install(Resources)

        // Handle request timeouts
        install(HttpTimeout) {
            requestTimeoutMillis = rowndContext.config.defaultRequestTimeout
        }

        expectSuccess = true
//        HttpResponseValidator {
//            handleResponseExceptionWithRequest { exception, request ->
//                val clientException = exception as? ClientRequestException ?: return@handleResponseExceptionWithRequest
//                val exceptionResponse = clientException.response
//
//                try {
//                    val apiError =
//                        json.decodeFromString(APIError.serializer(), exceptionResponse.bodyAsText())
//                    throw apiError
//                } catch (ex: Exception) {
//                    throw exception
//                }
//            }
//        }

        defaultRequest {
            url(rowndContext.config.apiUrl)
            contentType(ContentType.Application.Json)
            headers {
                rowndContext.config.appKey?.let { this.append("x-rownd-app-key", it) }
            }
        }
    }

    open var client = base
}