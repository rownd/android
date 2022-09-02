package io.rownd.android.models.network

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

val json = Json { ignoreUnknownKeys = true }

@Serializable
internal data class APIError(
    var error: String,
    var message: String,
    var messages: List<String>? = null
)

class RowndAPIException(response: Response<*>) : HttpException(response) {

    private var apiError: APIError? = null
    override lateinit var message: String

    init {
        try {
            val body = response.errorBody()
            apiError = body?.let { json.decodeFromString(APIError.serializer(), it.string()) }

            message = message()
        } catch(e: Exception) {
            message = response.message()
            Log.w("RowndApi", "Failed to decode error: ${e.message}")
        }
    }

    override fun message() : String {
        return if (apiError?.messages != null && apiError?.messages!!.isNotEmpty()) {
            message + "\n" + apiError?.messages!!.joinToString("\n")
        } else {
            apiError?.message ?: message ?: "message unavailable"
        }
    }
}