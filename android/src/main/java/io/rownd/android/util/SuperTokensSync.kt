package io.rownd.android.util

import android.util.Log
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.rownd.android.models.SuperTokensAppInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuperTokensSync @Inject constructor(
    private val apiClient: KtorApiClient,
) {
    suspend fun syncUser(accessToken: String, appInfo: SuperTokensAppInfo) {
        val migrationUrl = appInfo.migrationUrl()

        try {
            apiClient.client.post(migrationUrl) {
                header("Authorization", "Bearer $accessToken")
            }
        } catch (e: Exception) {
            Log.e("Rownd.ST", "[Rownd->ST] migrate failed (non-fatal): ${e.message}")
        }
    }
}
