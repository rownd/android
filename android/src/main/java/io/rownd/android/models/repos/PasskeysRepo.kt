package io.rownd.android.models.repos

import android.util.Log
import io.rownd.android.models.domain.PasskeysState
import io.rownd.android.models.network.PasskeyRegistrationResponse
import io.rownd.android.models.network.PasskeysApi
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PasskeysRepo @Inject constructor() {
    @Inject
    lateinit var passkeysApi: PasskeysApi

    @Inject
    lateinit var stateRepo: StateRepo

    private var isLoading: Boolean = false

    internal fun get(): PasskeysState {
        return stateRepo.getStore().currentState.passkeys
    }

    init {
        CoroutineScope(Dispatchers.IO).launch {
            stateRepo.getStore().stateAsStateFlow().collect() {
                if (
                    it.auth.accessToken != null && !it.passkeys.isInitialized && !isLoading
                ) {
                    fetchPasskeyRegistration()
                }
            }
        }
    }

    internal fun fetchPasskeyRegistration(): Deferred<PasskeyRegistrationResponse?> {
        isLoading = true
        return CoroutineScope(Dispatchers.IO).async {
            val result = passkeysApi.client.fetchRegistration()
                .onSuccess {
                    val domainModalList = it.passkeys?.map { x -> x.asDomainModel() }
                    stateRepo.getStore().dispatch(StateAction.SetPasskeys(PasskeysState(isInitialized = true, registrations = domainModalList ?: emptyList())))
                }
                .onFailure {
                    Log.e("Rownd", "Passkey registration fetch failed! ${it.message}")
                }

            isLoading = false
            return@async result.getOrNull()
        }
    }

}