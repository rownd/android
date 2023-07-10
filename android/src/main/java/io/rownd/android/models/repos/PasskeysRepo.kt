package io.rownd.android.models.repos

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import android.util.Log
import io.rownd.android.models.domain.PasskeysState
import io.rownd.android.models.network.PasskeyRegistrationResponse
import io.rownd.android.models.network.PasskeysApi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasskeysRepo @Inject constructor() {
    @Inject
    lateinit var passkeysApi: PasskeysApi

    @Inject
    lateinit var stateRepo: StateRepo

    internal fun get(): PasskeysState {
        return stateRepo.getStore().currentState.passkeys
    }

    internal fun fetchPasskeyRegistration(): Deferred<PasskeyRegistrationResponse?> {
        return CoroutineScope(Dispatchers.IO).async {
            val result = passkeysApi.client.fetchRegistration()
                .onSuccess {
                    val domainModalList = it.passkeys?.map { x -> x.asDomainModel() }
                    stateRepo.getStore().dispatch(StateAction.SetPasskeys(PasskeysState(isInitialized = true, registrations = domainModalList ?: emptyList())))
                }
                .onFailure {
                    Log.e("Rownd", "Passkey registration fetch failed! ${it.message}")
                }

            return@async result.getOrNull()
        }
    }

}