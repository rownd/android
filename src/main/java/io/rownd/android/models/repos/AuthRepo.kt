package io.rownd.android.models.repos

import android.util.Log
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.domain.AuthState
import io.rownd.android.models.network.AppConfigApi
import io.rownd.android.models.network.AuthApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthRepo {
    internal val _authState = MutableStateFlow<AuthState>(AuthState())
    val authState: StateFlow<AuthState> get() = _authState

    // TODO: Can this method be synchronized or do we need an actual queue?
    @Synchronized
    private fun fetchToken() {
        CoroutineScope(Dispatchers.IO).launch {
            AuthApi.client.exchangeToken().onSuccess {
                _authState.value = it?.asDomainModel() ?: AuthState()
            }
                .onFailure {
                    Log.e("RowndApi", "Oh no! Request failed! ${it.message}")
                }

        }
    }
}