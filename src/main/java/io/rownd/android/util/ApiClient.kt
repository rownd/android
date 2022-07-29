package io.rownd.android.util

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import io.rownd.android.Rownd
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit

val json = Json { ignoreUnknownKeys = true }

internal class DefaultHeadersInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var newRequest = request.newBuilder()

        newRequest.addHeader("user-agent", Constants.DEFAULT_API_USER_AGENT)

        if (Rownd.config.appKey != null) {
            newRequest.addHeader("x-rownd-app-key", Rownd.config.appKey)
        }

        // TODO: Add authorization header
//        if (someState.auth.accessToken) {
//            newRequest.addHeader("Authorization", someState.auth.accessToken)
//        }

        return chain.proceed(newRequest.build())
    }
}

object ApiClient {
    private val okHttpClient = OkHttpClient.Builder().addInterceptor(DefaultHeadersInterceptor()).build()

    fun getInstance(): Retrofit {

        return Retrofit.Builder().baseUrl(Rownd.config.apiUrl)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(MediaType.get("appliation/json")))
            .addCallAdapterFactory(ResultCallAdapterFactory())
            .build()
    }
}