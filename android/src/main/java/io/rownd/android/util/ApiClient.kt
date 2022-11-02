package io.rownd.android.util

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.rownd.android.Rownd
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Invocation
import retrofit2.Retrofit


val json = Json { ignoreUnknownKeys = true }

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
internal annotation class RequireAccessToken

internal class DefaultHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = request.newBuilder()
        val method = chain.request().tag(Invocation::class.java)!!.method()

        newRequest.addHeader("user-agent", Constants.DEFAULT_API_USER_AGENT)

        val appKey = Rownd.config.appKey
        if (appKey != null) {
            newRequest.addHeader("x-rownd-app-key", appKey)
        }

        // This will block the thread, but this should be ok since Retrofit is running this
        // on a separate thread anyway (i.e., not main/ui)
        if(method.isAnnotationPresent(RequireAccessToken::class.java)) {
            runBlocking {
                val accessToken = Rownd.getAccessToken()
                if (accessToken != null) {
                    newRequest.addHeader("Authorization", "Bearer $accessToken")
                }
            }
        }

        return chain.proceed(newRequest.build())
    }
}

internal var reqLogging = HttpLoggingInterceptor().apply {
    this.setLevel(HttpLoggingInterceptor.Level.BODY)
}

object ApiClient {
    private val okHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(DefaultHeadersInterceptor())
        .addInterceptor(reqLogging)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    fun getInstance(): Retrofit {

        return Retrofit.Builder().baseUrl(Rownd.config.apiUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
    }
}