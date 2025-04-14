package io.rownd.android.authenticators.passkeys

import io.rownd.android.util.AuthenticatedApiClient
import io.rownd.android.util.KtorApiClient
import io.rownd.android.util.RowndContext
import javax.inject.Inject

class PasskeysCommon @Inject constructor() {
    @Inject
    lateinit var rowndContext: RowndContext

    @Inject
    lateinit var api: KtorApiClient

    @Inject
    lateinit var authenticatedApiClient: AuthenticatedApiClient

    val registration: PasskeyRegistration by lazy { PasskeyRegistration(this) }
    val authentication: PasskeyAuthentication by lazy { PasskeyAuthentication(this) }

    internal fun computeRpId(): String {
        return "${rowndContext.store?.currentState?.appConfig?.config?.subdomain}${rowndContext.config.subdomainExtension}"
    }



}