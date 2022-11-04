package io.rownd.android.util

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import io.rownd.android.BuildConfig
import io.rownd.android.Rownd
import kotlinx.serialization.ExperimentalSerializationApi
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Named
import javax.inject.Singleton

internal var reqLogging = HttpLoggingInterceptor().apply {
    this.setLevel(HttpLoggingInterceptor.Level.BODY)
}

class MockInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        if (BuildConfig.DEBUG) {
            val uri = chain.request().url.toUri().toString()
            val responseString = when {
                uri.endsWith("starred") -> getListOfReposBeingStarredJson
                else -> ""
            }

            return chain.proceed(chain.request())
                .newBuilder()
                .code(400)
                .protocol(Protocol.HTTP_2)
                .message(responseString)
                .body(
                    responseString.toByteArray()
                        .toResponseBody("application/json".toMediaTypeOrNull())
                )
                .addHeader("content-type", "application/json")
                .build()
        } else {
            //just to be on safe side.
            throw IllegalAccessError("MockInterceptor is only meant for Testing Purposes and " +
                    "bound to be used only with DEBUG mode")
        }
    }

}

const val getListOfReposBeingStarredJson = """
[{
	"id": 1296269,
	"node_id": "MDEwOlJlcG9zaXRvcnkxMjk2MjY5",
	"name": "Hello-World",
	"full_name": "octocat/Hello-World",
	"private": false,
	"html_url": "https://github.com/octocat/Hello-World",
	"description": "This your first repo!",
	"fork": false,
	"languages_url": "http://api.github.com/repos/octocat/Hello-World/languages",
	"stargazers_count": 80,
	"watchers_count": 80,
	"pushed_at": "2011-01-26T19:06:43Z",
	"created_at": "2011-01-26T19:01:12Z",
	"updated_at": "2011-01-26T19:14:43Z",
	"subscribers_count": 42
}]
"""

@Module
open class TestApiClientModule {
    @Provides
    @Singleton
    @Named("OkHttpClient")
    open fun provideOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(DefaultHeadersInterceptor())
        .addInterceptor(reqLogging)
        .addInterceptor(MockInterceptor())
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    @Named("Retrofit")
    open fun provideRetrofit(@Named("OkHttpClient") client: OkHttpClient): Retrofit = Retrofit
        .Builder()
        .client(client)
        .baseUrl(Rownd.config.apiUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(ResultCallAdapterFactory())
        .build()
}