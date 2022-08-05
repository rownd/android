package io.rownd.android.models.repos

import android.util.Log
import io.rownd.android.Rownd
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.network.AppConfigApi
import kotlinx.coroutines.*

class AppConfigRepo {
    companion object {
        internal fun loadAppConfigAsync(): Deferred<AppConfigState?> {
            return CoroutineScope(Dispatchers.IO).async {
                val result = AppConfigApi.client.getAppConfig()
                    .onSuccess {
                        Rownd.store.dispatch(StateAction.SetAppConfig(it.app.asDomainModel()))
                    }
                    .onFailure {
                        Log.e("Rownd", "Oh no! Request failed! ${it.message}")
                    }

                return@async result.getOrNull()?.app?.asDomainModel()
            }
        }
    }
}