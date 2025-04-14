package io.rownd.android.di.module

import dagger.Module
import dagger.Provides
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.android.Android

@Module
class NetworkModule {
    @Provides fun provideHttpClientEngine(): HttpClientEngine {
        return Android.create()
    }
}