package io.rownd.android.authenticators.passkeys

import io.rownd.android.util.AuthenticatedApi
import io.rownd.android.util.KtorApiClient
import io.rownd.android.util.RowndContext
import javax.inject.Inject

class PasskeysCommon @Inject constructor(internal val rowndContext: RowndContext) {

    internal val api: KtorApiClient by lazy { KtorApiClient(rowndContext) }
    internal val authenticatedApi: AuthenticatedApi by lazy { AuthenticatedApi(rowndContext) }

    val registration: PasskeyRegistration by lazy { PasskeyRegistration(this) }
    val authentication: PasskeyAuthentication by lazy { PasskeyAuthentication(this) }

    internal fun computeRpId(): String {
        return "${rowndContext.store?.currentState?.appConfig?.config?.subdomain}${rowndContext.config.subdomainExtension}"
    }



}