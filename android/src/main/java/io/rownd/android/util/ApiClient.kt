package io.rownd.android.util

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Lazy
import dagger.Module
import dagger.Provides
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
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton


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
                try {
                    val accessToken = Rownd.getAccessToken()
                    if (accessToken != null) {
                        newRequest.addHeader("Authorization", "Bearer $accessToken")
                    }
                    null
                } catch (e: Exception) {
                    Log.d("Rownd.ApiClient", "Failed to get access token during request. Error: ${e.message}")
                }
            }
        }

        return chain.proceed(newRequest.build())
    }
}

internal var reqLogging = HttpLoggingInterceptor().apply {
    this.setLevel(HttpLoggingInterceptor.Level.BODY)
}

//@Component(modules = [ApiClientModule::class])
//interface ApiClientComponent {
//    fun apiClientModule(): ApiClientModule
//}

@Module
class ApiClientModule {
    @Provides
    @Singleton
    @Named("OkHttpClient")
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient
        .Builder()
        .addInterceptor(DefaultHeadersInterceptor())
        .addInterceptor(reqLogging)
        .build()

    @OptIn(ExperimentalSerializationApi::class)
    @Provides
    @Singleton
    @Named("Retrofit")
    fun provideRetrofit(@Named("OkHttpClient") client: OkHttpClient): Retrofit = Retrofit
        .Builder()
        .client(client)
        .baseUrl(Rownd.config.apiUrl)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .addCallAdapterFactory(ResultCallAdapterFactory())
        .build()
}

class ApiClient @Inject constructor() {
    @Inject
    @Named("OkHttpClient")
    lateinit var okHttpClient: OkHttpClient

    /**
     * This leverages Dagger's lazy init so that we give developers time
     * to adjust `Rownd.config` before we initialize the API client.
     * Without this mechanism, the API client is initialized immediately
     * and it's then impossible to change the API base URL and such.
     *
     * However, it's not enough to lazily init this. Any consumers of the
     * ApiClient need to use Kotlin's `by lazy` delegate to prevent Dagger's
     * lazy init from executing immediately.
     *
     * Example:
     * ```kotlin
     * var client: ApiClient by lazy {
     *   apiClient.client.get().create(TokenService::class.java)
     * }
     * ```
     */
    @Inject
    @Named("Retrofit")
    lateinit var client: Lazy<Retrofit>
}