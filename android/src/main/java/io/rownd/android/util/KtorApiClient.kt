package io.rownd.android.util

import android.util.Log
import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.compression.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

private object CustomAndroidHttpLogger : Logger {
    private const val logTag = "Rownd.ApiClient"

    override fun log(message: String) {
        Log.d(logTag, redactSensitiveKeys(message))
    }
}

open class KtorApiClient constructor(rowndContext: RowndContext)  {
    private val base = HttpClient(Android) {
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

        // Uncomment after upgrading to ktor 2.2, since the HttpRequestRetry
        // plugin should catch timeouts at that point. It does not as of ktor 2.1.
//        install(HttpTimeout) {
//            requestTimeoutMillis = rowndContext.config.defaultRequestTimeout
//        }

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

    init {
        // Remove after upgrading to ktor 2.2+, since it should
        // be able to catch request timeouts directly vs. this workaround.
        base.plugin(HttpSend).intercept { request ->
            val executionContext = request.executionContext
            val killer = client.launch(Dispatchers.Default) {
                delay(rowndContext.config.defaultRequestTimeout)
                val cause = HttpRequestTimeoutException(request)
                executionContext.cancel(cause.message!!, cause)
            }
            executionContext.invokeOnCompletion {
                killer.cancel()
            }
            execute(request)
        }
    }
}