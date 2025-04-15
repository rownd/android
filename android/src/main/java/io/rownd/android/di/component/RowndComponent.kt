package io.rownd.android.di.component

import dagger.Component
import io.ktor.client.engine.HttpClientEngine
import io.rownd.android.authenticators.passkeys.PasskeysCommon
import io.rownd.android.di.module.ApiModule
import io.rownd.android.di.module.AuthRepoModule
import io.rownd.android.di.module.NetworkModule
import io.rownd.android.di.module.RowndConfigProvider
import io.rownd.android.models.RowndConfig
import io.rownd.android.models.RowndConnectionAction
import io.rownd.android.models.network.SignInLinkApi
import io.rownd.android.models.repos.AuthRepo
import io.rownd.android.models.repos.SignInRepo
import io.rownd.android.models.repos.StateRepo
import io.rownd.android.models.repos.UserRepo
import io.rownd.android.util.AuthenticatedApiClient
import io.rownd.android.util.RowndContext
import io.rownd.android.util.RowndEvent
import io.rownd.android.util.RowndEventEmitter
import io.rownd.android.util.SignInWithGoogle
import io.rownd.android.util.Telemetry
import io.rownd.android.util.TokenApiClient
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        RowndConfigProvider::class,
        NetworkModule::class,
        ApiModule::class,
        AuthRepoModule::class,
    ]
)
interface RowndGraph {
    fun stateRepo(): StateRepo
    fun userRepo(): UserRepo
    fun authRepo(): AuthRepo
    fun connectionAction(): RowndConnectionAction
    fun signInRepo(): SignInRepo
    fun signInLinkApi(): SignInLinkApi
    fun rowndContext(): RowndContext
    fun passkeyAuthenticator(): PasskeysCommon
    fun rowndEventEmitter(): RowndEventEmitter<RowndEvent>
    fun signInWithGoogle(): SignInWithGoogle
    fun telemetry(): Telemetry
    fun tokenApiClient(): TokenApiClient
    fun authenticatedApiClient(): AuthenticatedApiClient
    fun httpEngine(): HttpClientEngine
    fun config(): RowndConfig
    fun inject(rowndConfig: RowndConfig)
}