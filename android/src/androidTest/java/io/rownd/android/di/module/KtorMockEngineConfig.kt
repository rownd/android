package io.rownd.android.di.module

import dagger.Module
import dagger.Provides
import io.ktor.client.engine.mock.MockEngineConfig

@Module
class KtorMockEngineConfig {
    @Provides
    fun provideMockEngineConfig(): MockEngineConfig {
        return MockEngineConfig()

    }

}