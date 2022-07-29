package io.rownd.android.models.repos

import android.util.Log
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.network.AppConfig
import io.rownd.android.models.network.AppConfigApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.http.Header

class AppConfigRepo {
    private val _appConfigState = MutableStateFlow<AppConfigState>(AppConfigState())
    val appConfigState: StateFlow<AppConfigState> get() = _appConfigState

    init {
        fetchAppConfig()
    }

    private fun fetchAppConfig() {
        CoroutineScope(Dispatchers.IO).launch {
            AppConfigApi.client.getAppConfig().onSuccess {
                _appConfigState.value = it.app.asDomainModel() ?: AppConfigState()
            }
                .onFailure {
                    Log.e("Rownd", "Oh no! Request failed! ${it.message}")
                }

        }
    }
}