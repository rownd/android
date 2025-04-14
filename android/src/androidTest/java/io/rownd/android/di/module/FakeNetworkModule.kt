package io.rownd.android.di.module

import dagger.Module
import dagger.Provides
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import javax.inject.Inject

@Module
class FakeNetworkModule @Inject constructor(private val config: MockEngineConfig) {

    @Provides fun provideHttpClientEngine(): HttpClientEngine {
        return MockEngine(config)
    }
}