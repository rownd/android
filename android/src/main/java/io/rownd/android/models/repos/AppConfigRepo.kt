package io.rownd.android.models.repos

import android.util.Log
import io.ktor.client.plugins.ClientRequestException
import io.rownd.android.models.domain.AppConfigState
import io.rownd.android.models.network.AppConfigApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepo @Inject constructor() {
    @Inject
    lateinit var appConfigApi: AppConfigApi

    internal fun loadAppConfigAsync(stateRepo: StateRepo): Deferred<AppConfigState?> {
        return CoroutineScope(Dispatchers.IO).async {
            try {
                val result = appConfigApi.getAppConfig()
                stateRepo.getStore().dispatch(StateAction.SetAppConfig(result.app.asDomainModel()))

                return@async result.app.asDomainModel()
            } catch (ex: ClientRequestException) {
                Log.e(
                    "Rownd.AppConfig",
                    "Failed to load Rownd app config. This probably means you specified an invalid app key. ${ex.message}",
                    ex
                )
                stateRepo.getStore().dispatch(
                    StateAction.SetAppConfig(
                        stateRepo.getStore().currentState.appConfig.copy(
                            isLoading = false
                        )
                    )
                )
                return@async null
            } catch (ex: Exception) {
                Log.e(
                    "Rownd.AppConfig",
                    "Failed to load Rownd app config.",
                    ex
                )
                stateRepo.getStore().dispatch(
                    StateAction.SetAppConfig(
                        stateRepo.getStore().currentState.appConfig.copy(
                            isLoading = false
                        )
                    )
                )
                return@async null
            }
        }
    }
}